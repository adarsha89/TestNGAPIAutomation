package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.base.BaseWebhookTest;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.helpers.AuthStrategyFactory;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.LogMasker;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Covers all three {@code AuthStrategy} implementations (API_KEY/BASIC/BEARER)
 * end to end, plus the no-mechanism no-op case and credential masking - see
 * {@code docs/plans/code-change-plan-auth-config-rate-limit-503-fallback-2026-09-21.md}
 * Section 4. API_KEY runs against the real Reqres {@code users} endpoint
 * (the default strategy); BASIC/BEARER/no-mechanism run against a local
 * {@link RestStubServer}/{@code WebhookReceiver} since reqres.in doesn't
 * support those.
 */
@Story("Per-endpoint-key auth strategy selection")
public class AuthStrategyTests extends BaseWebhookTest implements BaseRestInterface {

    private static final String BASIC_ENDPOINT_KEY = "webhookFallbackDemo";
    private static final String BEARER_ENDPOINT_KEY = "webhookFallbackDemoBearer";
    private static final String NO_AUTH_ENDPOINT_KEY = "webhookNoAuthDemo";

    private final RestStubServer stubServer = new RestStubServer();

    @BeforeClass(alwaysRun = true)
    public void startStubServer() {
        stubServer.start();
        stubServer.stubPostResponse("/auth-check", 200, "{}");
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServer() {
        stubServer.stop();
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("API_KEY strategy on the users endpoint sends the configured api.key as the x-api-key header, "
            + "verified against a local stub target")
    public void shouldApplyApiKeyStrategyForUsersEndpoint() {
        restService().getUserByIdAndVerify(2, ResponseExpectation.status(200));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), "/auth-check", null, ResponseExpectation.status(200));

        String configuredApiKey = restService().config().getApiKey();
        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/auth-check"))
                .withHeader("x-api-key", WireMock.equalTo(configuredApiKey)));
    }

    @Test(groups = {"webhook", "auth", "regression"})
    @Description("The BASIC strategy sends Authorization: Basic base64(username:password) built from "
            + "webhookFallbackDemo.auth.username/password, verified via WireMock against the local webhook receiver")
    public void shouldApplyBasicAuthStrategy() {
        webhookService().stubIncomingCallResponse("/basic-auth-check", 200, "{}");

        restService().postToOverrideBaseUriAndVerify(
                webhookService().baseUrl(), "/basic-auth-check", BASIC_ENDPOINT_KEY, "{}",
                ResponseExpectation.status(200));

        EnvConfig config = restService().config();
        String username = valueOrEmpty(config.getProperty(BASIC_ENDPOINT_KEY + ".auth.username"));
        String password = valueOrEmpty(config.getProperty(BASIC_ENDPOINT_KEY + ".auth.password"));
        String expectedAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        webhookService().getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/basic-auth-check"))
                .withHeader("Authorization", WireMock.equalTo(expectedAuthHeader)));
    }

    @Test(groups = {"webhook", "auth", "regression"})
    @Description("The BEARER strategy sends Authorization: Bearer <token> from "
            + "webhookFallbackDemoBearer.auth.bearer.token, verified via WireMock against the local webhook receiver")
    public void shouldApplyBearerAuthStrategy() {
        webhookService().stubIncomingCallResponse("/bearer-auth-check", 200, "{}");

        restService().postToOverrideBaseUriAndVerify(
                webhookService().baseUrl(), "/bearer-auth-check", BEARER_ENDPOINT_KEY, "{}",
                ResponseExpectation.status(200));

        EnvConfig config = restService().config();
        String token = valueOrEmpty(config.getProperty(BEARER_ENDPOINT_KEY + ".auth.bearer.token"));
        String expectedAuthHeader = "Bearer " + token;

        webhookService().getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/bearer-auth-check"))
                .withHeader("Authorization", WireMock.equalTo(expectedAuthHeader)));
    }

    @Test(groups = {"webhook", "auth", "regression"})
    @Description("An endpoint key with no *.auth.strategy property at all resolves to no mechanism - the call "
            + "succeeds with no Authorization/x-api-key header added, and no exception is thrown resolving it")
    public void shouldNotAddAnyHeaderWhenNoAuthStrategyConfigured() {
        webhookService().stubIncomingCallResponse("/no-auth-check", 200, "{}");

        Assert.assertTrue(AuthStrategyFactory.resolve(NO_AUTH_ENDPOINT_KEY, restService().config()).isEmpty(),
                "Expected no AuthStrategy to be resolved for an endpoint key with no configured strategy");

        restService().postToOverrideBaseUriAndVerify(
                webhookService().baseUrl(), "/no-auth-check", NO_AUTH_ENDPOINT_KEY, "{}",
                ResponseExpectation.status(200));

        webhookService().getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/no-auth-check"))
                .withHeader("Authorization", WireMock.absent())
                .withHeader("x-api-key", WireMock.absent()));
    }

    @Test(groups = {"auth", "regression"})
    @Description("Credential-bearing properties for the BASIC/BEARER demonstration endpoint keys are registered "
            + "as sensitive so LogMasker masks them, without any LogMasker code change")
    public void shouldTreatDemoCredentialPropertiesAsSensitive() {
        var sensitiveNames = restService().config().getSensitiveDataNames();

        Assert.assertTrue(LogMasker.isSensitive(BASIC_ENDPOINT_KEY + ".auth.password", sensitiveNames));
        Assert.assertTrue(LogMasker.isSensitive(BEARER_ENDPOINT_KEY + ".auth.bearer.token", sensitiveNames));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
