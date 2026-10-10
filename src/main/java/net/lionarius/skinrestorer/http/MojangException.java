package net.lionarius.skinrestorer.http;

import java.time.Duration;

/** A fetch failure that callers can translate without inspecting low-level exception messages. */
public final class MojangException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        INVALID_USERNAME,
        QUEUE_FULL,
        CLOSED,
        NOT_FOUND,
        NO_SKIN,
        RATE_LIMITED,
        TIMEOUT,
        NETWORK,
        HTTP_ERROR,
        INVALID_RESPONSE
    }

    private final Reason reason;
    private final int statusCode;
    private final Duration retryAfter;

    public MojangException(Reason reason, String message) {
        this(reason, message, 0, Duration.ZERO, null);
    }

    public MojangException(Reason reason, String message, Throwable cause) {
        this(reason, message, 0, Duration.ZERO, cause);
    }

    public MojangException(Reason reason, String message, int statusCode, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.reason = java.util.Objects.requireNonNull(reason, "reason");
        this.statusCode = statusCode;
        this.retryAfter = java.util.Objects.requireNonNull(retryAfter, "retryAfter");
    }

    public Reason reason() {
        return reason;
    }

    public int statusCode() {
        return statusCode;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
