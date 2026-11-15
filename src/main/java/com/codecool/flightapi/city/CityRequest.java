package com.codecool.flightapi.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST/PUT /api/v1/cities}. */
public record CityRequest(@NotBlank @Size(max = 255) String name) {}
