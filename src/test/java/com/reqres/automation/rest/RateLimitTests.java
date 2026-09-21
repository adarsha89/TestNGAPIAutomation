package com.reqres.automation.rest;

import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.dataproviders.RateLimitDataProvider;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies {@code RateLimitHelper}'s per-endpoint-key, delay-based pacing
 * against a local {@link RestStubServer}, since reqres.in can't be made to
 * pace itself on demand.
 */
@Story("Per-endpoint-key rate limiting")
public class RateLimitTests extends BaseRestTest {

    private static final String STUB_PATH = "/rate-limit-check";
    private static final String POST_STUB_PATH = "/rate-limit-post-check";
    private static final String UNLIMITED_ENDPOINT_KEY = "rateLimitDemoUnlimited";
    private static final String USERS_ENDPOINT_KEY = "users";
    private static final int USERS_PERMITS_PER_SECOND = 100;
    private static final String OVERRIDE_ENDPOINT_KEY = "rateLimitDemoA";
    private static final int OVERRIDE_PERMITS_PER_SECOND = 5;

    private final RestStubServer stubServer = new RestStubServer();

    @BeforeClass(alwaysRun = true)
    public void startStubServer() {
        stubServer.start();
        stubServer.stubGetResponse(STUB_PATH, 200, "{}");
        stubServer.stubPostResponse(POST_STUB_PATH, 200, "{}");
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServer() {
        stubServer.stop();
    }

    @Test(groups = {"rest", "rate-limit", "regression"},
            dataProvider = "rateLimitedEndpoints", dataProviderClass = RateLimitDataProvider.class)
    @Description("A burst of calls against an endpoint key configured with <endpointKey>.rate.limit.per.second is "
            + "paced so the elapsed time is at least the theoretical minimum implied by that endpoint's own limit")
    public void shouldPaceCallsAccordingToConfiguredLimit(String endpointKey, int permitsPerSecond) {
        int burstSize = permitsPerSecond + 2;
        long minGapMs = (long) Math.ceil(1000.0 / permitsPerSecond);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            restService().getFromOverrideBaseUriRateLimitedAndVerify(
                    endpointKey, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(200));
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " calls against '" + endpointKey
                        + "' (limit " + permitsPerSecond + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of calls against an endpoint key with no configured rate limit completes with no added "
            + "delay - the no-op baseline this feature must not regress")
    public void shouldNotDelayCallsForUnconfiguredEndpoint() {
        int burstSize = 5;
        long generousUnpacedCeilingMs = 3_000;

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            restService().getFromOverrideBaseUriRateLimitedAndVerify(
                    UNLIMITED_ENDPOINT_KEY, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(200));
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs < generousUnpacedCeilingMs,
                "Expected " + burstSize + " calls against the unconfigured endpoint key to complete quickly with "
                        + "no rate-limit pacing added, took " + elapsedMs + "ms");
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of calls through the 3-arg postToOverrideBaseUri override path (implicitly keyed to "
            + "'users') is paced per users.rate.limit.per.second, closing that method's rate-limiting gap")
    public void shouldPaceThreeArgPostToOverrideBaseUri() {
        int burstSize = USERS_PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / USERS_PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);
        // generous ceiling: rules out double-pacing (twice the gaps) while tolerating scheduling jitter
        long upperBoundMs = theoreticalMinimumMs * 2 + 2000;

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            restService().postToOverrideBaseUriAndVerify(
                    stubServer.stubUrl(""), POST_STUB_PATH, null, ResponseExpectation.status(200));
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " calls through the 3-arg "
                        + "postToOverrideBaseUri override path (limit " + USERS_PERMITS_PER_SECOND
                        + "/s) to be >= the theoretical minimum (" + theoreticalMinimumMs + "ms)");
        Assert.assertTrue(elapsedMs <= upperBoundMs,
                "Expected elapsed time (" + elapsedMs + "ms) to be <= " + upperBoundMs
                        + "ms, ruling out double-pacing of the same calls");
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of calls through the 5-arg postToOverrideBaseUri override path is paced per the "
            + "caller-supplied endpoint key's configured limit, closing that method's rate-limiting gap")
    public void shouldPaceFiveArgPostToOverrideBaseUri() {
        int burstSize = OVERRIDE_PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / OVERRIDE_PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);
        // generous ceiling: rules out double-pacing (twice the gaps) while tolerating scheduling jitter
        long upperBoundMs = theoreticalMinimumMs * 2 + 2000;

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            restService().postToOverrideBaseUriAndVerify(
                    stubServer.stubUrl(""), POST_STUB_PATH, OVERRIDE_ENDPOINT_KEY, null,
                    ResponseExpectation.status(200));
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " calls through the 5-arg "
                        + "postToOverrideBaseUri override path against '" + OVERRIDE_ENDPOINT_KEY + "' (limit "
                        + OVERRIDE_PERMITS_PER_SECOND + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
        Assert.assertTrue(elapsedMs <= upperBoundMs,
                "Expected elapsed time (" + elapsedMs + "ms) to be <= " + upperBoundMs
                        + "ms, ruling out double-pacing of the same calls");
    }
}
