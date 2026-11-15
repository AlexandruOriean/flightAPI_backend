package com.codecool.flightapi.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username, @NotBlank String password) {}
