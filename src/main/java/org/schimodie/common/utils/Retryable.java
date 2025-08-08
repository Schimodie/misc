package org.schimodie.common.utils;

import java.util.concurrent.Callable;

public interface Retryable<O> {
    O retry(Callable<O> callable) throws RetryableException;
}
