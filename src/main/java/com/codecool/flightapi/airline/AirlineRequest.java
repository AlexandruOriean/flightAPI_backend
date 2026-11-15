package com.codecool.flightapi.airline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST/PUT /api/v1/airlines}.
 *
 * @param icaoCode three-letter ICAO airline designator, e.g. {@code ROT}
 */
public record AirlineRequest(
        @NotBlank @Size(max = 255) String name,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be 3 upper-case letters") String icaoCode) {}
