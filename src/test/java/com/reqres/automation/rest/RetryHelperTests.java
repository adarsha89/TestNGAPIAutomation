package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.reqres.automation.base.BaseRestTest;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.Constants;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies {@code RetryHelper}'s bounded, exponential-backoff retry against
 * a local {@link RestStubServer} using WireMock's stateful scenarios, since
 * reqres.in can't be made to fail transiently on demand.
 */
@Story("Bounded retry on transient (502/503/504) response status")
public class RetryHelperTests extends BaseRestTest {

    private static final String STUB_PATH = "/retry-check";
    // Deliberately unconfigured endpoint key (see common.properties) - retry pacing is a no-op here,
    // so these exact-call-count/timing assertions hold.
    private static final String RETRY_DEMO_KEY = Constants.RETRY_DEMO_ENDPOINT_KEY;
    private static final String PACED_RETRY_DEMO_KEY = "rateLimitDemoB";

    private final RestStubServer stubServer = new RestStubServer();

    @BeforeClass(alwaysRun = true)
    public void startStubServer() {
        stubServer.start();
    }

    @BeforeMethod(alwaysRun = true)
    public void resetStubServer() {
        stubServer.reset();
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServer() {
        stubServer.stop();
    }

    @Test(groups = {"rest", "utils", "regression"})
    @Description("A first call returning 503 followed by a second call returning 200 results in an overall "
            + "success, with more than one call observed against the stub (proving a retry happened)")
    public void shouldRetryOnceOnTransientStatusThenSucceed() {
        stubServer.getServer().stubFor(WireMock.get(WireMock.urlEqualTo(STUB_PATH))
                .inScenario("retry-then-succeed")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"service unavailable\"}"))
                .willSetStateTo("succeeded"));
        stubServer.getServer().stubFor(WireMock.get(WireMock.urlEqualTo(STUB_PATH))
                .inScenario("retry-then-succeed")
                .whenScenarioStateIs("succeeded")
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        restService().getFromOverrideBaseUriWithRetryAndVerify(
                RETRY_DEMO_KEY, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(200));

        stubServer.getServer().verify(2, WireMock.getRequestedFor(WireMock.urlEqualTo(STUB_PATH)));
    }

    @Test(groups = {"rest", "utils", "regression"})
    @Description("A non-transient status (404) is returned immediately with no retry - zero additional calls "
            + "beyond the first")
    public void shouldNotRetryOnNonTransientStatus() {
        stubServer.stubGetResponse(STUB_PATH, 404, "{\"error\":\"not found\"}");

        restService().getFromOverrideBaseUriWithRetryAndVerify(
                RETRY_DEMO_KEY, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(404));

        stubServer.getServer().verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo(STUB_PATH)));
    }

    @Test(groups = {"rest", "utils", "regression"}, timeOut = 15_000)
    @Description("A target that always returns a transient status exhausts retries and fails after exactly "
            + "retry.max.attempts calls, bounded rather than hanging")
    public void shouldFailAfterExhaustingConfiguredMaxAttempts() {
        stubServer.stubGetResponse(STUB_PATH, 503, "{\"error\":\"service unavailable\"}");

        restService().getFromOverrideBaseUriWithRetryAndVerify(
                RETRY_DEMO_KEY, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(503));

        // default retry.max.attempts=3 (see common.properties) - exactly that many calls, not more, not fewer
        stubServer.getServer().verify(3, WireMock.getRequestedFor(WireMock.urlEqualTo(STUB_PATH)));
    }

    @Test(groups = {"rest", "rate-limit", "regression"})
    @Description("Each individual retry attempt against a configured endpoint key is paced, not just the first "
            + "one - elapsed time across all exhausted attempts meets the theoretical minimum for that many calls")
    public void shouldPaceEachRetryAttemptForConfiguredEndpointKey() {
        stubServer.stubGetResponse(STUB_PATH, 503, "{\"error\":\"service unavailable\"}");
        int permitsPerSecond = 2;
        long minGapMs = (long) Math.ceil(1000.0 / permitsPerSecond);
        // default retry.max.attempts=3 - 3 paced attempts means 2 gaps between them
        long theoreticalMinimumMs = minGapMs * 2;
        // generous ceiling: rules out double-pacing (4 gaps) while tolerating normal scheduling jitter
        long upperBoundMs = minGapMs * 3 + 2000;

        long startedAt = System.currentTimeMillis();
        restService().getFromOverrideBaseUriWithRetryAndVerify(
                PACED_RETRY_DEMO_KEY, stubServer.stubUrl(""), STUB_PATH, ResponseExpectation.status(503));
        long elapsedMs = System.currentTimeMillis() - startedAt;

        stubServer.getServer().verify(3, WireMock.getRequestedFor(WireMock.urlEqualTo(STUB_PATH)));
        Assert.assertTrue(elapsedMs >= theoreticalMinimumMs,
                "Expected elapsed time (" + elapsedMs + "ms) across all retry attempts against '"
                        + PACED_RETRY_DEMO_KEY + "' (limit " + permitsPerSecond
                        + "/s) to be >= the theoretical minimum (" + theoreticalMinimumMs + "ms)");
        Assert.assertTrue(elapsedMs <= upperBoundMs,
                "Expected elapsed time (" + elapsedMs + "ms) to be <= " + upperBoundMs
                        + "ms, ruling out double-pacing of the same retry attempts");
    }
}
