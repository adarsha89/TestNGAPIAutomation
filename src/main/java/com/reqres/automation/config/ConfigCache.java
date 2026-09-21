package com.reqres.automation.config;

import com.reqres.automation.utils.LruTtlCache;

import java.util.function.Supplier;

/**
 * Thin wrapper over a {@link LruTtlCache} keyed by resolved environment
 * name, used exclusively by {@link ConfigLoader#load()}. Capacity 4 (one
 * slot per realistic environment: qa/staging/prod/local-override) and a
 * 10-minute TTL - long enough to cover a full suite run, bounded so no
 * long-running process could ever serve arbitrarily stale config.
 */
final class ConfigCache {

    private static final int CAPACITY = 4;
    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    private static final LruTtlCache<String, EnvConfig> CACHE = new LruTtlCache<>(CAPACITY, TTL_MILLIS);

    private ConfigCache() {
    }

    static EnvConfig getOrLoad(String env, Supplier<EnvConfig> loader) {
        EnvConfig cached = CACHE.get(env);
        if (cached != null) {
            return cached;
        }
        EnvConfig loaded = loader.get();
        CACHE.put(env, loaded);
        return loaded;
    }
}
