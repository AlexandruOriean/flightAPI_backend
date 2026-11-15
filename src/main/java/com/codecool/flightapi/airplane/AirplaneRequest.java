package com.codecool.flightapi.airplane;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST/PUT /api/v1/airplanes}.
 *
 * @param speed cruise speed in km/h
 */
public record AirplaneRequest(
        @NotBlank @Size(max = 255) String manufacturer,
        @NotBlank @Size(max = 255) String model,
        @NotNull @Positive Long numberOfSeats,
        @NotNull @Positive Double speed) {}
