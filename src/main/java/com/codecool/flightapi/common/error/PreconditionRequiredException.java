package com.codecool.flightapi.common.error;

/** An update without {@code If-Match}. Rendered as {@code 428 Precondition Required}. */
public class PreconditionRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PreconditionRequiredException() {
        super("Send the resource's ETag in an If-Match header; GET the resource to get its ETag.");
    }
}
