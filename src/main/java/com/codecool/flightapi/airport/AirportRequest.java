package com.codecool.flightapi.airport;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;

/**
 * Body of {@code POST/PUT /api/v1/airports}.
 *
 * @param iataCode three-letter IATA airport code, e.g. {@code OTP}
 * @param cityName name of an existing city (case-insensitive)
 * @param timeZone IANA time zone, e.g. {@code Europe/Bucharest}
 */
public record AirportRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String iataCode,

        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String cityName,
        @NotBlank @Size(max = 64) String timeZone) {

    @AssertTrue(message = "must be an IANA time zone, e.g. Europe/Bucharest") public boolean isKnownTimeZone() {
        return timeZone == null || ZoneId.getAvailableZoneIds().contains(timeZone);
    }
}
