package com.codecool.flightapi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/auth/refresh}. */
public record RefreshRequest(@NotBlank @Size(max = 100) String refreshToken) {}
