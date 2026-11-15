package com.codecool.flightapi.common.error;

import java.time.Duration;

/** A client exceeded a rate limit. Rendered as {@code 429 Too Many Requests} with {@code Retry-After}. */
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    public TooManyRequestsException(Duration retryAfter) {
        super("Too many attempts. Try again in " + retryAfterSeconds(retryAfter) + " seconds.");
        this.retryAfter = retryAfter;
    }

    /** Whole seconds until the next attempt is allowed, at least 1, as sent in {@code Retry-After}. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds(retryAfter);
    }

    private static long retryAfterSeconds(Duration retryAfter) {
        long seconds = retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
        return Math.max(1, seconds);
    }
}
