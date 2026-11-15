package com.codecool.flightapi.common.error;

import org.springframework.security.core.AuthenticationException;

/**
 * A refresh token is unknown, expired, revoked or was already used. Rendered as {@code 401}; the client must
 * log in again. Deliberately one message for every case.
 */
public class InvalidRefreshTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException() {
        super("The refresh token is invalid or expired. Log in again.");
    }
}
