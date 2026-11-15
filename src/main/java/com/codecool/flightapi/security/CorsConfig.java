package com.codecool.flightapi.security;

import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The single CORS policy of the application, applied by {@code http.cors(...)} in {@link SecurityConfig}.
 * Only the origins listed in {@link CorsProperties} ({@code app.cors.allowed-origins}) may call the API from a browser; with
 * no origins configured, cross-origin requests are rejected.
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.IF_MATCH));
        // POST answers 201 with the new resource's URI
        config.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.ETAG, HttpHeaders.RETRY_AFTER));
        // the token travels in the Authorization header, so browsers need not send cookies
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
