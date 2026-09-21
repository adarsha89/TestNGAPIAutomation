package com.reqres.automation.clients.auth.cache;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Pure unit coverage of {@link TokenCacheRegistry}'s LRU/TTL mechanics and
 * identity isolation, independent of any HTTP call - uses the
 * package-private constructor to build small, isolated instances rather
 * than the shared production singleton.
 */
@Story("Token cache LRU/TTL mechanics and identity isolation")
public class TokenCacheRegistryTests {

    private static final long GENEROUS_OUTER_TTL_MS = 60_000;

    @DataProvider(name = "capacityAndOverflow")
    public Object[][] capacityAndOverflow() {
        return new Object[][]{
                {2, 3},
                {3, 4},
        };
    }

    @Test(groups = {"cache", "regression"}, dataProvider = "capacityAndOverflow")
    @Description("Inserting capacity + 1 distinct identities evicts specifically the least-recently-used one - "
            + "a subsequent get() returns a miss only for the evicted key")
    public void shouldEvictLeastRecentlyUsedIdentityWhenCapacityExceeded(int capacity, int identityCount) {
        TokenCacheRegistry registry = new TokenCacheRegistry(capacity, GENEROUS_OUTER_TTL_MS);
        Clock clock = Clock.systemUTC();
        Instant farFuture = Instant.now(clock).plus(Duration.ofHours(1));

        for (int i = 0; i < identityCount - 1; i++) {
            registry.put("identity-" + i, new CachedToken("token-" + i, farFuture));
        }
        // touch every already-inserted identity so the next (not-yet-inserted) key trips
        // eviction of the true least-recently-used entry: identity-0
        for (int i = 1; i < identityCount - 1; i++) {
            registry.get("identity-" + i, clock);
        }
        registry.get("identity-1", clock);

        registry.put("identity-" + (identityCount - 1), new CachedToken("token-last", farFuture));

        Assert.assertTrue(registry.get("identity-0", clock).isEmpty(),
                "Expected the untouched, oldest identity to be evicted");
        Assert.assertTrue(registry.get("identity-" + (identityCount - 1), clock).isPresent(),
                "Expected the newly-inserted identity to remain cached");
    }

    @Test(groups = {"cache", "regression"})
    @Description("An entry whose real expiry has passed is never returned, independent of LRU/capacity state")
    public void shouldTreatEntryAsMissOnceRealExpiryPasses() {
        TokenCacheRegistry registry = new TokenCacheRegistry(4, GENEROUS_OUTER_TTL_MS);
        Instant alreadyPast = Instant.now().minus(Duration.ofMinutes(1));
        registry.put("identity", new CachedToken("token", alreadyPast));

        Optional<CachedToken> result = registry.get("identity", Clock.systemUTC());

        Assert.assertTrue(result.isEmpty(), "Expected an entry past its real expiry to be treated as a miss");
    }

    @Test(groups = {"cache", "regression"})
    @Description("Distinct identity keys never collide/overwrite each other's cached value")
    public void shouldIsolateDistinctIdentities() {
        TokenCacheRegistry registry = new TokenCacheRegistry(4, GENEROUS_OUTER_TTL_MS);
        Clock clock = Clock.systemUTC();
        Instant farFuture = Instant.now(clock).plus(Duration.ofHours(1));

        registry.put("identity-a", new CachedToken("token-a", farFuture));
        registry.put("identity-b", new CachedToken("token-b", farFuture));

        Assert.assertEquals(registry.get("identity-a", clock).get().tokenValue(), "token-a");
        Assert.assertEquals(registry.get("identity-b", clock).get().tokenValue(), "token-b");
    }

    @Test(groups = {"cache", "regression"})
    @Description("An injected fixed Clock, not real elapsed time, drives the hit/miss decision - a token valid "
            + "against the real clock reads as expired once the injected clock is moved past its real expiry")
    public void shouldHonorInjectedClockOverRealElapsedTime() {
        TokenCacheRegistry registry = new TokenCacheRegistry(4, GENEROUS_OUTER_TTL_MS);
        Instant realExpiry = Instant.now().plus(Duration.ofMinutes(5));
        registry.put("identity", new CachedToken("token", realExpiry));

        Assert.assertTrue(registry.get("identity", Clock.systemUTC()).isPresent(),
                "Expected a hit against the real, current clock");

        Clock farFutureClock = Clock.fixed(realExpiry.plus(Duration.ofHours(1)), ZoneOffset.UTC);
        Assert.assertTrue(registry.get("identity", farFutureClock).isEmpty(),
                "Expected a miss once the injected clock is moved past the entry's real expiry");
    }
}
