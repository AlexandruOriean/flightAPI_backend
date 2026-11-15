package com.codecool.flightapi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bounded like the columns they are checked against, so oversized input never reaches the database. */
public record LoginRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Size(max = 255) String password) {}
