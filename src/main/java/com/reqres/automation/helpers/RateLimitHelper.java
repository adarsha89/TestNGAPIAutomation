package com.reqres.automation.helpers;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sleeps the calling thread as needed to keep calls for a given endpoint key
 * spaced at least {@code 1000 / permitsPerSecond} ms apart, per
 * {@code <endpointKey>.rate.limit.per.second}. No-op if that property is
 * unset. Pacing state is per endpoint key, so limits don't share a budget.
 */
public final class RateLimitHelper {

    private static final ConcurrentHashMap<String, AtomicLong> LAST_PERMITTED_CALL_AT = new ConcurrentHashMap<>();

    private RateLimitHelper() {
    }

    public static void acquire(String endpointKey, EnvConfig config) {
        String rawLimit = config.getProperty(endpointKey + Constants.RATE_LIMIT_PER_SECOND_SUFFIX);
        if (rawLimit == null || rawLimit.isBlank()) {
            return;
        }
        double permitsPerSecond = Double.parseDouble(rawLimit.trim());
        if (permitsPerSecond <= 0) {
            return;
        }
        long minGapMs = (long) Math.ceil(1000.0 / permitsPerSecond);

        AtomicLong lastCallAt = LAST_PERMITTED_CALL_AT.computeIfAbsent(endpointKey, key -> new AtomicLong(-1));
        synchronized (lastCallAt) {
            long now = System.currentTimeMillis();
            long previous = lastCallAt.get();
            long nextPermittedAt = previous < 0 ? now : previous + minGapMs;
            long waitMs = nextPermittedAt - now;
            if (waitMs > 0) {
                sleep(waitMs);
                nextPermittedAt = System.currentTimeMillis();
            }
            lastCallAt.set(Math.max(nextPermittedAt, now));
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while rate-limit pacing", e);
        }
    }
}
