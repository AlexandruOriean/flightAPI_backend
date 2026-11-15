package com.codecool.flightapi.security;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Browser origins allowed to call the API, e.g. from the environment variable {@code CORS_ALLOWED_ORIGINS}.
 *
 * @param allowedOrigins exact origins such as {@code https://flights.example.com}, no wildcards; empty rejects
 *     every cross-origin request
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {

    /**
     * An origin as browsers send it: {@code http(s)://host} with an optional port, nothing after it. A trailing
     * slash or a path never matches, so every browser request would fail with a hard-to-trace CORS error.
     */
    private static final Pattern ORIGIN = Pattern.compile("https?://[^/:?#*\\s]+(:\\d{1,5})?");

    /**
     * Checks every origin when the settings are bound, so a mistake stops the startup.
     *
     * @throws IllegalArgumentException naming the entry, if an origin is not in that form
     */
    public CorsProperties {
        for (String origin : allowedOrigins) {
            if (!ORIGIN.matcher(origin).matches()) {
                throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS: '" + origin + "' is not an origin;"
                        + " use scheme, host and optional port only, e.g. https://flights.example.com"
                        + " (no trailing slash, path or wildcard)");
            }
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
