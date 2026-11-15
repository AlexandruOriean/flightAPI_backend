package com.codecool.flightapi.common.error;

/** {@code If-Match} names an older version of the resource. Rendered as {@code 412 Precondition Failed}. */
public class PreconditionFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PreconditionFailedException() {
        super("The resource was changed since you read it. GET it again, reapply your changes and retry.");
    }
}
