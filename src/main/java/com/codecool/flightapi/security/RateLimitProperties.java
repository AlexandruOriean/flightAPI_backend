package com.codecool.flightapi.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Attempts allowed per client IP on the authentication endpoints ({@code app.rate-limit.*}).
 *
 * @param login {@code POST /api/v1/auth/login}
 * @param register {@code POST /api/v1/auth/register}
 * @param password {@code POST /api/v1/auth/password}
 */
@ConfigurationProperties("app.rate-limit")
@Validated
public record RateLimitProperties(
        @NotNull @Valid Limit login,
        @NotNull @Valid Limit register,
        @NotNull @Valid Limit password) {

    /**
     * At most {@code requests} attempts per {@code period}; used-up attempts come back gradually over the period.
     */
    public record Limit(@Positive int requests, @NotNull Duration period) {}
}
