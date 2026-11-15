package com.codecool.flightapi.airport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST/PUT /api/v1/airports}.
 *
 * @param iataCode three-letter IATA airport code, e.g. {@code OTP}
 * @param cityName name of an existing city (case-insensitive)
 */
public record AirportRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String iataCode,

        @NotBlank @Size(max = 255) String name,
        @NotBlank String cityName) {}
