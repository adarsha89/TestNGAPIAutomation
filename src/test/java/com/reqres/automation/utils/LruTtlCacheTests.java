package com.reqres.automation.utils;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests {@link LruTtlCache}'s LRU/TTL mechanics directly, independent of
 * any call site (see {@code ConfigCachingTests} for the {@code ConfigLoader}
 * integration).
 */
@Story("Generic bounded LRU + TTL cache mechanics")
public class LruTtlCacheTests {

    @Test(groups = {"cache", "regression"})
    @Description("A second get() for the same key within TTL returns the same cached value, without needing the "
            + "loader to run again")
    public void shouldServeFreshEntryFromCacheWithinTtl() {
        LruTtlCache<String, String> cache = new LruTtlCache<>(4, 5_000);
        cache.put("key", "value");

        Assert.assertEquals(cache.get("key"), "value");
        Assert.assertEquals(cache.get("key"), "value");
    }

    @Test(groups = {"cache", "regression"})
    @Description("get() after the TTL has elapsed returns null/absent, exactly as an uncached first lookup")
    public void shouldTreatEntryAsAbsentOnceTtlElapses() throws InterruptedException {
        LruTtlCache<String, String> cache = new LruTtlCache<>(4, 200);
        cache.put("key", "value");
        Assert.assertEquals(cache.get("key"), "value");

        Thread.sleep(300);

        Assert.assertNull(cache.get("key"), "Expected the entry to be treated as absent once its TTL elapsed");
    }

    @Test(groups = {"cache", "regression"})
    @Description("Inserting capacity + 1 distinct keys evicts specifically the least-recently-used one - keys "
            + "touched via get() before the insert that trips eviction survive; the untouched one is evicted")
    public void shouldEvictLeastRecentlyUsedEntryWhenCapacityExceeded() {
        LruTtlCache<String, String> cache = new LruTtlCache<>(3, 60_000);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        // touch a and c (most-recently-used), leave b untouched (least-recently-used)
        cache.get("a");
        cache.get("c");

        cache.put("d", "4");

        Assert.assertNull(cache.get("b"), "Expected the untouched key 'b' to be evicted");
        Assert.assertEquals(cache.get("a"), "1");
        Assert.assertEquals(cache.get("c"), "3");
        Assert.assertEquals(cache.get("d"), "4");
    }
}
