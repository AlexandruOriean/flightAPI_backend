package com.codecool.flightapi.common.error;

/** A request is well-formed but cannot be served as asked. Rendered as {@code 400 Bad Request}. */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
