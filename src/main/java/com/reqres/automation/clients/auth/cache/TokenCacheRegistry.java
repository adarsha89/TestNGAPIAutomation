package com.reqres.automation.clients.auth.cache;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.LruTtlCache;

import java.time.Clock;
import java.util.Optional;

/**
 * Process-wide singleton wrapper over a {@link LruTtlCache}, keyed by the
 * {@code <STRATEGY_TYPE>|<endpointKey>|<identity>} scheme (see
 * {@link #buildKey(String, String, String)}). Capacity/outer TTL are taken
 * from the first {@link EnvConfig} passed to {@link #getInstance(EnvConfig)}
 * - a singleton because {@code AuthStrategyFactory.resolve()} constructs a
 * fresh strategy instance per call, so the cache can't live on the strategy
 * itself.
 * <p>
 * A lookup is only a hit if the cached {@link CachedToken#isValid(Clock)} -
 * this is the real per-token expiry enforcement, independent of this
 * class's outer, generous TTL backstop.
 */
public final class TokenCacheRegistry {

    private static volatile TokenCacheRegistry instance;

    // test-support-only clock override (mirrors TokenEndpointOverride's ThreadLocal shape) - lets
    // a cache-expiry test simulate the passage of time without a real Thread.sleep
    private static final ThreadLocal<Clock> CLOCK_OVERRIDE = new ThreadLocal<>();

    private final LruTtlCache<String, CachedToken> cache;

    // package-private: lets TokenCacheRegistryTests exercise LRU/TTL boundary behavior with a
    // small, isolated capacity instead of the shared production singleton.
    TokenCacheRegistry(int capacity, long ttlMillis) {
        this.cache = new LruTtlCache<>(capacity, ttlMillis);
    }

    public static TokenCacheRegistry getInstance(EnvConfig config) {
        TokenCacheRegistry local = instance;
        if (local == null) {
            synchronized (TokenCacheRegistry.class) {
                local = instance;
                if (local == null) {
                    local = new TokenCacheRegistry(config.getAuthTokenCacheMaxSize(), config.getAuthTokenCacheMaxTtlMs());
                    instance = local;
                }
            }
        }
        return local;
    }

    public static String buildKey(String strategyType, String endpointKey, String identity) {
        return strategyType + "|" + endpointKey + "|" + identity;
    }

    public Optional<CachedToken> get(String key, Clock clock) {
        CachedToken cached = cache.get(key);
        if (cached == null || !cached.isValid(clock)) {
            return Optional.empty();
        }
        return Optional.of(cached);
    }

    /** Production entry point used by the token-acquiring {@code AuthStrategy} implementations -
     * resolves the clock from {@link #useClockForTesting(Clock)}'s thread-local override when set,
     * otherwise {@link Clock#systemUTC()}. */
    public Optional<CachedToken> get(String key) {
        return get(key, currentClock());
    }

    public void put(String key, CachedToken token) {
        cache.put(key, token);
    }

    /** The clock this instance resolves "now" against for production {@link #get(String)} lookups
     * - {@link #useClockForTesting(Clock)}'s override when set, otherwise {@link Clock#systemUTC()}.
     * Exposed so a token-acquiring strategy computes a new token's expiry against the same notion
     * of "now" its own cache lookups use. */
    public Clock clock() {
        return currentClock();
    }

    // test-support only - lets a cache-expiry test simulate the passage of time deterministically
    public static void useClockForTesting(Clock clock) {
        CLOCK_OVERRIDE.set(clock);
    }

    public static void resetClockForTesting() {
        CLOCK_OVERRIDE.remove();
    }

    private static Clock currentClock() {
        Clock override = CLOCK_OVERRIDE.get();
        return override != null ? override : Clock.systemUTC();
    }
}
