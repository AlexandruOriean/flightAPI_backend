package com.codecool.flightapi.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Access-token settings, e.g. from the environment variables {@code JWT_SECRET} and {@code JWT_EXPIRATION_MS}.
 *
 * @param secret Base64-encoded HMAC key of at least 64 bytes (HS512), e.g. {@code openssl rand -base64 64}
 * @param expirationMs access-token lifetime in milliseconds; short, since a refresh token renews it
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret, @DefaultValue("900000") long expirationMs) {

    /** How long an issued token stays valid. */
    public Duration tokenLifetime() {
        return Duration.ofMillis(expirationMs);
    }

    @Override
    public String toString() {
        // never expose the signing key, e.g. in logs or actuator output
        return "JwtProperties[secret=****, expirationMs=" + expirationMs + "]";
    }
}
