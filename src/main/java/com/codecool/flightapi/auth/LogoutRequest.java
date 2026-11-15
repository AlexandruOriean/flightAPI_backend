package com.codecool.flightapi.auth;

import jakarta.validation.constraints.Size;

/**
 * Optional body of {@code POST /api/v1/auth/logout}.
 *
 * @param refreshToken revoked together with every other token of its login; without it, logout only expires
 *     the legacy cookie and the client discards its tokens
 */
public record LogoutRequest(@Size(max = 100) String refreshToken) {}
