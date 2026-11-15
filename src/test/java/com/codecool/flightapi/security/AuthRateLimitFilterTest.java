package com.codecool.flightapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.common.error.TooManyRequestsException;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

/** {@link AuthRateLimitFilter} with a fake clock: 3 logins per minute, 2 registrations per hour. */
class AuthRateLimitFilterTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String REGISTER = "/api/v1/auth/register";
    private static final String PASSWORD = "/api/v1/auth/password";

    private final FakeTime time = new FakeTime();
    private final List<Exception> rejections = new ArrayList<>();

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(
            new RateLimitProperties(
                    new RateLimitProperties.Limit(3, Duration.ofMinutes(1)),
                    new RateLimitProperties.Limit(2, Duration.ofHours(1)),
                    new RateLimitProperties.Limit(1, Duration.ofHours(1))),
            (request, response, handler, ex) -> {
                rejections.add(ex);
                return new ModelAndView();
            },
            time);

    @Test
    @DisplayName("the attempt after the limit is rejected with the time until the next one is allowed")
    void rejectsAttemptsOverTheLimit() throws Exception {
        assertThat(attempts("POST", LOGIN, "10.0.0.1", 3)).isEqualTo(3);
        assertThat(attempts("POST", LOGIN, "10.0.0.1", 1)).isZero();

        // 3 per minute refill one every 20 seconds
        assertThat(rejections)
                .singleElement()
                .isInstanceOfSatisfying(
                        TooManyRequestsException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(20));
    }

    @Test
    @DisplayName("password changes have their own limit, so a stolen access token cannot guess the password")
    void limitsPasswordChanges() throws Exception {
        assertThat(attempts("POST", PASSWORD, "10.0.0.1", 2)).isEqualTo(1);
        assertThat(attempts("POST", LOGIN, "10.0.0.1", 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("used-up attempts come back gradually over the period")
    void refillsOverThePeriod() throws Exception {
        attempts("POST", LOGIN, "10.0.0.1", 3);

        time.advance(Duration.ofSeconds(19));
        assertThat(attempts("POST", LOGIN, "10.0.0.1", 1)).isZero();
        time.advance(Duration.ofSeconds(1));
        assertThat(attempts("POST", LOGIN, "10.0.0.1", 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("each client IP and each endpoint has its own limit")
    void limitsPerClientAndEndpoint() throws Exception {
        attempts("POST", LOGIN, "10.0.0.1", 3);

        assertThat(attempts("POST", LOGIN, "10.0.0.2", 3)).isEqualTo(3);
        assertThat(attempts("POST", REGISTER, "10.0.0.1", 3)).isEqualTo(2);
    }

    @Test
    @DisplayName("other requests are never limited")
    void otherRequestsAreNotLimited() throws Exception {
        attempts("POST", LOGIN, "10.0.0.1", 3);

        assertThat(attempts("POST", "/api/v1/auth/logout", "10.0.0.1", 10)).isEqualTo(10);
        assertThat(attempts("GET", LOGIN, "10.0.0.1", 10)).isEqualTo(10);
        assertThat(attempts("OPTIONS", LOGIN, "10.0.0.1", 10)).isEqualTo(10);
        assertThat(attempts("GET", "/api/v1/flights/1", "10.0.0.1", 10)).isEqualTo(10);
        assertThat(rejections).isEmpty();
    }

    /** Sends {@code count} requests and returns how many were let through. */
    private int attempts(String method, String path, String clientIp, int count) throws Exception {
        int passed = 0;
        for (int i = 0; i < count; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, path);
            request.setRemoteAddr(clientIp);
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            if (chain.getRequest() != null) {
                passed++;
            }
        }
        return passed;
    }

    private static final class FakeTime implements TimeMeter {

        private long nanos;

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}
