package org.schimodie.common.utils;

import java.util.concurrent.Callable;

public class ExponentialBackoffRetryable<O> implements Retryable<O> {
    private final int maxRetries;
    private final int baseTimeInMillis;

    public ExponentialBackoffRetryable(int maxRetries, int baseTimeInMillis) {
        this.maxRetries = maxRetries;
        this.baseTimeInMillis = baseTimeInMillis;
    }

    @Override
    public O retry(Callable<O> callable) throws RetryableException {
        int retries = 0;
        Throwable caughtThrowable = null;

        while (retries < maxRetries) {
            try {
                Thread.sleep(((1L << retries) - 1) * baseTimeInMillis);
                return callable.call();
            } catch (Throwable t) {
                caughtThrowable = t;
            }

            ++retries;
        }

        if (caughtThrowable != null) {
            throw new RetryableException(String.format("Reached the maximum number of retries (%s)", maxRetries),
                    caughtThrowable);
        }

        throw new RetryableException(String.format("Reached the maximum number of retries (%s)", maxRetries));
    }
}
