package com.codecool.flightapi.auth;

import java.util.List;

/** Body of a successful login. The client sends {@code accessToken} back as {@code Authorization: Bearer <token>}. */
public record TokenResponse(
        String accessToken, String tokenType, Long id, String username, String email, List<String> roles) {

    static TokenResponse bearer(String accessToken, Long id, String username, String email, List<String> roles) {
        return new TokenResponse(accessToken, "Bearer", id, username, email, roles);
    }
}
