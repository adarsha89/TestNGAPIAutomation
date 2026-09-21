package com.reqres.automation.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small, generic bounded cache combining LRU eviction (capacity-based) with a
 * per-entry TTL (time-based). Not thread-hardened beyond what
 * {@code LinkedHashMap}'s access-order mode provides internally - callers
 * needing cross-thread safety (e.g. {@code ConfigCache}) synchronize
 * externally.
 */
public final class LruTtlCache<K, V> {

    private final int capacity;
    private final long ttlMillis;
    private final LinkedHashMap<K, CacheEntry<V>> entries;

    public LruTtlCache(int capacity, long ttlMillis) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0, was: " + capacity);
        }
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be > 0, was: " + ttlMillis);
        }
        this.capacity = capacity;
        this.ttlMillis = ttlMillis;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > LruTtlCache.this.capacity;
            }
        };
    }

    public synchronized V get(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            entries.remove(key);
            return null;
        }
        return entry.value();
    }

    public synchronized void put(K key, V value) {
        entries.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
    }

    public synchronized int size() {
        return entries.size();
    }

    private static final class CacheEntry<V> {
        private final V value;
        private final long expiresAt;

        private CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        private V value() {
            return value;
        }

        // corrected from the reference's inverted "expiresAt > now" - see class Javadoc
        private boolean isExpired() {
            return expiresAt <= System.currentTimeMillis();
        }
    }
}
