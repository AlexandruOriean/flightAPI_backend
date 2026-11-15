package com.codecool.flightapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Binding of the {@code app.jwt.*} and {@code app.cors.*} settings, as Spring Boot does at startup. */
class SecurityPropertiesTest {

    @Test
    @DisplayName("JWT: the lifetime defaults to 15 minutes and the secret never appears in toString")
    void jwtDefaultsAndHidesSecret() {
        JwtProperties jwt = bind(Map.of("app.jwt.secret", "c2VjcmV0")).bindOrCreate("app.jwt", JwtProperties.class);

        assertThat(jwt.tokenLifetime()).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.secret()).isEqualTo("c2VjcmV0");
        assertThat(jwt.toString()).doesNotContain("c2VjcmV0");
    }

    @Test
    @DisplayName("JWT: expiration-ms sets the lifetime")
    void jwtExpiration() {
        JwtProperties jwt = bind(Map.of("app.jwt.secret", "x", "app.jwt.expiration-ms", "300000"))
                .bindOrCreate("app.jwt", JwtProperties.class);

        assertThat(jwt.tokenLifetime()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("CORS: comma-separated origins; unset or empty means no origins")
    void corsOrigins() {
        assertThat(bind(Map.of("app.cors.allowed-origins", "https://a.example, https://b.example"))
                        .bindOrCreate("app.cors", CorsProperties.class)
                        .allowedOrigins())
                .containsExactly("https://a.example", "https://b.example");
        assertThat(bind(Map.of()).bindOrCreate("app.cors", CorsProperties.class).allowedOrigins())
                .isEmpty();
        assertThat(bind(Map.of("app.cors.allowed-origins", ""))
                        .bindOrCreate("app.cors", CorsProperties.class)
                        .allowedOrigins())
                .isEmpty();
    }

    private static Binder bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties));
    }
}
