package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.base.BaseWebhookInterface;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.helpers.FallbackHelper;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies {@code FallbackHelper}'s config-gated, per-endpoint-key
 * 503-fallback path: a local {@link RestStubServer} serves the on-demand
 * 503 (reqres.in can't be made to return one), and the
 * {@code WebhookReceiver} serves as the fallback target.
 */
@Story("Config-gated per-endpoint-key 503 webhook fallback")
public class Fallback503Tests implements BaseWebhookInterface, BaseRestInterface {

    private static final String FALLBACK_ON_KEY = "fallbackDemoOn";
    private static final String FALLBACK_OFF_KEY = "fallbackDemoOff";
    private static final String FALLBACK_ISOLATION_B_KEY = "fallbackDemoIsolationB";

    private static final String FALLBACK_ON_WEBHOOK_PATH = "/fallback/demo-on";
    private static final String FALLBACK_ISOLATION_B_WEBHOOK_PATH = "/fallback/demo-isolation-b";

    private static final String RATE_LIMITED_ENDPOINT_KEY = "rateLimitDemoA";
    private static final int RATE_LIMITED_PERMITS_PER_SECOND = 5;

    private final RestStubServer stubServer = new RestStubServer();

    @BeforeClass(alwaysRun = true)
    public void startStubServer() {
        stubServer.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServer() {
        stubServer.stop();
    }

    @Test(groups = {"rest", "fallback", "regression"})
    @Description("With fallback.on503.enabled=false (or absent), a primary 503 is returned unmodified, no "
            + "fallback call is made, and the outcome flag reports 'not from fallback'")
    public void shouldReturnRaw503UnchangedWhenFallbackDisabled() {
        webhookService().reset();
        String primaryPath = "/fallback-off-check";
        stubServer.stubGetResponse(primaryPath, 503, "{\"error\":\"service unavailable\"}");

        restService().getFromOverrideBaseUriWithFallbackAndVerify(
                FALLBACK_OFF_KEY, stubServer.stubUrl(""), primaryPath, webhookService(),
                ResponseExpectation.status(503));

        Assert.assertFalse(FallbackHelper.wasLastCallServedByFallback(),
                "Expected the outcome flag to report the response did not come from the fallback");
        webhookService().getServer().verify(0, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/fallback/demo-off")));
    }

    @Test(groups = {"rest", "fallback", "regression"})
    @Description("With fallback.on503.enabled=true and a primary 503, exactly one call is made to the configured "
            + "local webhook path and its response is returned instead, with the outcome flag reporting 'from "
            + "fallback'")
    public void shouldServeFromFallbackWhenEnabledAndPrimaryReturns503() {
        webhookService().reset();
        String primaryPath = "/fallback-on-check";
        String standInBody = "{\"source\":\"fallback\"}";
        stubServer.stubGetResponse(primaryPath, 503, "{\"error\":\"service unavailable\"}");
        webhookService().stubIncomingCallResponse(FALLBACK_ON_WEBHOOK_PATH, 200, standInBody);

        restService().getFromOverrideBaseUriWithFallbackAndVerify(
                FALLBACK_ON_KEY, stubServer.stubUrl(""), primaryPath, webhookService(),
                ResponseExpectation.status(200).andRawBodyEquals(standInBody));

        Assert.assertTrue(FallbackHelper.wasLastCallServedByFallback(),
                "Expected the outcome flag to report the response came from the fallback");
        webhookService().getServer().verify(1, WireMock.postRequestedFor(
                WireMock.urlEqualTo(FALLBACK_ON_WEBHOOK_PATH)));
    }

    @Test(groups = {"rest", "fallback", "regression"})
    @Description("Fallback never triggers for any status other than exactly 503 - a primary 500 is returned "
            + "unchanged and no fallback call is made, even with fallback enabled for that endpoint key")
    public void shouldNotTriggerFallbackForNon503Status() {
        webhookService().reset();
        String primaryPath = "/fallback-non-503-check";
        stubServer.stubGetResponse(primaryPath, 500, "{\"error\":\"internal server error\"}");

        restService().getFromOverrideBaseUriWithFallbackAndVerify(
                FALLBACK_ON_KEY, stubServer.stubUrl(""), primaryPath, webhookService(),
                ResponseExpectation.status(500));

        Assert.assertFalse(FallbackHelper.wasLastCallServedByFallback(),
                "Expected the outcome flag to report the response did not come from the fallback");
        webhookService().getServer().verify(0, WireMock.postRequestedFor(
                WireMock.urlEqualTo(FALLBACK_ON_WEBHOOK_PATH)));
    }

    @Test(groups = {"rest", "fallback", "regression"})
    @Description("One endpoint key's fallback opt-in does not affect another endpoint key's 503 handling in the "
            + "same run - endpoint A (enabled) is served from fallback, endpoint B (disabled) returns its raw 503")
    public void shouldIsolateFallbackOptInPerEndpointKey() {
        webhookService().reset();
        String primaryPathA = "/fallback-isolation-a";
        String primaryPathB = "/fallback-isolation-b";
        String standInBody = "{\"source\":\"fallback-isolation-a\"}";

        stubServer.stubGetResponse(primaryPathA, 503, "{\"error\":\"service unavailable\"}");
        stubServer.stubGetResponse(primaryPathB, 503, "{\"error\":\"service unavailable\"}");
        webhookService().stubIncomingCallResponse(FALLBACK_ON_WEBHOOK_PATH, 200, standInBody);

        restService().getFromOverrideBaseUriWithFallbackAndVerify(
                FALLBACK_ON_KEY, stubServer.stubUrl(""), primaryPathA, webhookService(),
                ResponseExpectation.status(200).andRawBodyEquals(standInBody));
        Assert.assertTrue(FallbackHelper.wasLastCallServedByFallback());

        restService().getFromOverrideBaseUriWithFallbackAndVerify(
                FALLBACK_ISOLATION_B_KEY, stubServer.stubUrl(""), primaryPathB, webhookService(),
                ResponseExpectation.status(503));
        Assert.assertFalse(FallbackHelper.wasLastCallServedByFallback());
        webhookService().getServer().verify(0, WireMock.postRequestedFor(
                WireMock.urlEqualTo(FALLBACK_ISOLATION_B_WEBHOOK_PATH)));
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("A burst of primary calls through getFromOverrideBaseUriWithFallback is paced per the configured "
            + "endpoint key's limit before the fallback decision logic runs, closing that method's rate-limiting "
            + "gap - unchanged 200 primary responses prove the fallback/success outcome is unaffected")
    public void shouldPacePrimaryCallsBeforeFallbackDecision() {
        webhookService().reset();
        String primaryPath = "/fallback-rate-limit-check";
        stubServer.stubGetResponse(primaryPath, 200, "{}");

        int burstSize = RATE_LIMITED_PERMITS_PER_SECOND + 2;
        long minGapMs = (long) Math.ceil(1000.0 / RATE_LIMITED_PERMITS_PER_SECOND);
        long theoreticalMinimumMs = minGapMs * (burstSize - 1);
        // generous ceiling: rules out double-pacing (twice the gaps) while tolerating scheduling jitter
        long upperBoundMs = theoreticalMinimumMs * 2 + 2000;

        long startedAt = System.currentTimeMillis();
        for (int i = 0; i < burstSize; i++) {
            restService().getFromOverrideBaseUriWithFallbackAndVerify(
                    RATE_LIMITED_ENDPOINT_KEY, stubServer.stubUrl(""), primaryPath, webhookService(),
                    ResponseExpectation.status(200));
        }
        long elapsedMs = System.currentTimeMillis() - startedAt;

        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) for " + burstSize + " calls through "
                        + "getFromOverrideBaseUriWithFallback against '" + RATE_LIMITED_ENDPOINT_KEY + "' (limit "
                        + RATE_LIMITED_PERMITS_PER_SECOND + "/s) to be >= the theoretical minimum ("
                        + theoreticalMinimumMs + "ms)");
        Assert.assertTrue(elapsedMs <= upperBoundMs,
                "Expected elapsed time (" + elapsedMs + "ms) to be <= " + upperBoundMs
                        + "ms, ruling out double-pacing of the same calls");
    }
}
