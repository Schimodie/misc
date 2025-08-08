package org.schimodie.common.utils;

public class RetryableException extends RuntimeException {
    public RetryableException() {
        super();
    }

    public RetryableException(final String message) {
        super(message);
    }

    public RetryableException(final Throwable cause) {
        super(cause);
    }

    public RetryableException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RetryableException(final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
