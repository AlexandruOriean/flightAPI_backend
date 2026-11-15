package com.codecool.flightapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"https://flights.example.com", "http://localhost:3000", "https://10.0.0.5:8443"})
    @DisplayName("CORS: scheme, host and optional port are accepted")
    void corsAcceptsOrigins(String origin) {
        assertThat(bind(Map.of("app.cors.allowed-origins", origin))
                        .bindOrCreate("app.cors", CorsProperties.class)
                        .allowedOrigins())
                .containsExactly(origin);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://flights.example.com/", // trailing slash: browsers never send it
                "https://flights.example.com/app",
                "flights.example.com",
                "*",
                "https://*.example.com",
                "ftp://flights.example.com",
                "https://flights.example.com?x=1"
            })
    @DisplayName("CORS: anything that is not an exact origin stops the startup, naming the entry")
    void corsRejectsWhatBrowsersNeverSend(String origin) {
        assertThatThrownBy(() ->
                        bind(Map.of("app.cors.allowed-origins", origin)).bindOrCreate("app.cors", CorsProperties.class))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'" + origin + "' is not an origin");
    }

    private static Binder bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties));
    }
}
