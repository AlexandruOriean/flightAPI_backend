package com.codecool.flightapi.aircraft;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST/PUT /api/v1/aircraft-types}.
 *
 * @param icaoCode ICAO aircraft type designator, 2 to 4 upper-case letters or digits, e.g. {@code A320}
 * @param cruiseSpeedKmh typical cruise speed in km/h
 * @param typicalSeats typical number of seats, optional
 */
public record AircraftTypeRequest(
        @NotNull @Pattern(regexp = "[A-Z0-9]{2,4}", message = "must be 2 to 4 upper-case letters or digits") String icaoCode,

        @NotBlank @Size(max = 100) String manufacturer,
        @NotBlank @Size(max = 100) String model,
        @NotNull @Positive Integer cruiseSpeedKmh,
        @Positive Integer typicalSeats) {}
