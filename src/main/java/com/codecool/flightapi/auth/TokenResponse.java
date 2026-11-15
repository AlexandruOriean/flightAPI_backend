package com.codecool.flightapi.auth;

import java.util.List;

/**
 * Body of a successful login or refresh. The client sends {@code accessToken} as
 * {@code Authorization: Bearer <token>} and, before it expires, exchanges {@code refreshToken} at
 * {@code POST /api/v1/auth/refresh} for a new pair. Each refresh token works once.
 *
 * @param expiresIn seconds until the access token expires (as OAuth2's {@code expires_in})
 * @param refreshExpiresIn seconds until the refresh token expires; log in again after that
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        Long id,
        String username,
        String email,
        List<String> roles) {}
