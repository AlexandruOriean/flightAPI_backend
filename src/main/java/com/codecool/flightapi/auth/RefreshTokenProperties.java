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
 * @param reuseGrace how long after its first use a token may be used again without ending the login, e.g. by a
 *     second browser tab refreshing at the same moment. Each such reuse gets its own new token. Later reuse is
 *     treated as theft and revokes the login; {@code 0s} makes every reuse revoke it.
 */
@ConfigurationProperties("app.refresh-token")
@Validated
public record RefreshTokenProperties(
        @NotNull @DefaultValue("7d") Duration lifetime,
        @NotNull @DefaultValue("10s") Duration reuseGrace) {}
