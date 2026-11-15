package com.codecool.flightapi.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Browser origins allowed to call the API, e.g. from the environment variable {@code CORS_ALLOWED_ORIGINS}.
 *
 * @param allowedOrigins exact origins such as {@code https://flights.example.com}, no wildcards; empty rejects
 *     every cross-origin request
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {}
