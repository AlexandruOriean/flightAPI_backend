package com.codecool.flightapi.auth;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Refresh-token settings ({@code app.refresh-token.*}).
 *
 * @param lifetime how long a refresh token stays valid; every refresh issues a new one with a fresh lifetime
 */
@ConfigurationProperties("app.refresh-token")
@Validated
public record RefreshTokenProperties(
        @NotNull @DefaultValue("7d") Duration lifetime) {}
