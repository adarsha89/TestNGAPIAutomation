package com.reqres.automation.clients.auth.cache;

import java.time.Clock;
import java.time.Instant;

/**
 * A cached token value paired with its real expiry (already reduced by the
 * configured safety margin) - the actual per-token expiry enforcement
 * point, independent of the outer
 * {@link com.reqres.automation.utils.LruTtlCache}'s own coarse TTL, which
 * is only a generous backstop.
 */
public final class CachedToken {

    private final String tokenValue;
    private final Instant realExpiry;

    public CachedToken(String tokenValue, Instant realExpiry) {
        this.tokenValue = tokenValue;
        this.realExpiry = realExpiry;
    }

    public String tokenValue() {
        return tokenValue;
    }

    public Instant realExpiry() {
        return realExpiry;
    }

    public boolean isValid(Clock clock) {
        return Instant.now(clock).isBefore(realExpiry);
    }

    /** Builds a {@link CachedToken} whose real expiry is {@code rawExpiry} reduced by the
     * configured safety margin - the actual value stored/checked by {@link #isValid(Clock)}. */
    public static CachedToken withSafetyMargin(String tokenValue, Instant rawExpiry, long safetyMarginSeconds) {
        return new CachedToken(tokenValue, rawExpiry.minusSeconds(safetyMarginSeconds));
    }
}
