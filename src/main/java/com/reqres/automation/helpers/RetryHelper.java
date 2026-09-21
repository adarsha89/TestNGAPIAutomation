package com.reqres.automation.helpers;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Bounded, exponential-backoff retry over a REST call, keyed off the
 * *response status* (never a thrown exception) being in a configured
 * transient set. Retries are capped at {@code maxAttempts} - never
 * indefinite.
 */
public final class RetryHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHelper.class);

    private RetryHelper() {
    }

    /**
     * Invokes {@code call} up to {@code maxAttempts} times. Retries only
     * when the returned {@link Response}'s status code is in
     * {@code transientStatuses}; the first non-transient (or successful)
     * response is returned immediately. Each retry waits
     * {@code initialDelayMs * 2^(attempt - 1)} before the next attempt.
     * Never catches/retries on a thrown exception from {@code call} - that
     * propagates immediately.
     */
    public static Response retryOnTransientStatus(Supplier<Response> call, Set<Integer> transientStatuses,
            int maxAttempts, long initialDelayMs) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be > 0, was: " + maxAttempts);
        }

        Response response = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            response = call.get();
            if (!transientStatuses.contains(response.getStatusCode())) {
                return response;
            }
            LOGGER.warn("Transient status {} on attempt {}/{}", response.getStatusCode(), attempt, maxAttempts);
            if (attempt < maxAttempts) {
                sleep(initialDelayMs * (1L << (attempt - 1)));
            }
        }
        return response;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", e);
        }
    }
}
