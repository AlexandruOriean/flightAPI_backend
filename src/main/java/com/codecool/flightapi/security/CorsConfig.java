package com.codecool.flightapi.security;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
        if (cors.allowedOrigins().isEmpty()) {
            log.warn("No browser origins allowed (CORS_ALLOWED_ORIGINS is empty): a browser frontend served from"
                    + " another origin cannot call the API. Set it to the frontend's origin, e.g."
                    + " https://flights.example.com");
        } else {
            log.info("Browser origins allowed to call the API: {}", cors.allowedOrigins());
        }
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
