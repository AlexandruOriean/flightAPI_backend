package com.codecool.flightapi.auth;

import java.util.List;

/**
 * Body of a successful login. The client sends {@code accessToken} back as {@code Authorization: Bearer <token>}.
 *
 * @param expiresIn seconds until the token expires (as OAuth2's {@code expires_in}); log in again after that
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long id,
        String username,
        String email,
        List<String> roles) {

    static TokenResponse bearer(
            String accessToken, long expiresIn, Long id, String username, String email, List<String> roles) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, id, username, email, roles);
    }
}
