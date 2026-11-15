package com.codecool.flightapi.security;

import com.codecool.flightapi.common.error.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Limits login and registration attempts per client IP with a token bucket, against password guessing and
 * e-mail probing. Runs first in the security filter chain, so a rejected request costs neither JSON parsing
 * nor a password hash. Over the limit: {@code 429} problem+json with {@code Retry-After}, rendered by
 * {@code GlobalExceptionHandler}.
 *
 * <p>The client IP is {@code request.getRemoteAddr()}, which Tomcat takes from {@code X-Forwarded-For} only
 * when the request comes from a trusted proxy. Buckets live in memory, per application instance.
 */
class AuthRateLimitFilter extends OncePerRequestFilter {

    /** Caps memory use when many different IPs call the endpoints. */
    private static final long MAX_TRACKED_CLIENTS = 100_000;

    private final List<LimitedEndpoint> endpoints;
    private final HandlerExceptionResolver exceptionResolver;

    AuthRateLimitFilter(
            RateLimitProperties properties, HandlerExceptionResolver exceptionResolver, TimeMeter timeMeter) {
        this.endpoints = List.of(
                new LimitedEndpoint("/api/v1/auth/login", properties.login(), timeMeter),
                new LimitedEndpoint("/api/v1/auth/register", properties.register(), timeMeter),
                // a stolen access token must not allow guessing the current password
                new LimitedEndpoint("/api/v1/auth/password", properties.password(), timeMeter));
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        for (LimitedEndpoint endpoint : endpoints) {
            if (endpoint.matcher().matches(request)) {
                ConsumptionProbe probe =
                        endpoint.bucketFor(request.getRemoteAddr()).tryConsumeAndReturnRemaining(1);
                if (!probe.isConsumed()) {
                    exceptionResolver.resolveException(
                            request,
                            response,
                            null,
                            new TooManyRequestsException(Duration.ofNanos(probe.getNanosToWaitForRefill())));
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    /** One limited endpoint with a bucket per client IP. */
    private record LimitedEndpoint(
            RequestMatcher matcher, Cache<String, Bucket> buckets, Bandwidth bandwidth, TimeMeter timeMeter) {

        LimitedEndpoint(String path, RateLimitProperties.Limit limit, TimeMeter timeMeter) {
            this(
                    PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path),
                    // an idle client's bucket is full again after one period, so it can be forgotten then
                    Caffeine.newBuilder()
                            .expireAfterAccess(limit.period())
                            .maximumSize(MAX_TRACKED_CLIENTS)
                            .build(),
                    Bandwidth.builder()
                            .capacity(limit.requests())
                            .refillGreedy(limit.requests(), limit.period())
                            .build(),
                    timeMeter);
        }

        Bucket bucketFor(String clientIp) {
            return buckets.get(
                    clientIp,
                    ip -> Bucket.builder()
                            .addLimit(bandwidth)
                            .withCustomTimePrecision(timeMeter)
                            .build());
        }
    }
}
