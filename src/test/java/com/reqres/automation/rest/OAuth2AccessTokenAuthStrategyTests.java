package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.auth.token.TokenEndpointOverride;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Covers {@code OAuth2AccessTokenAuthStrategy} end to end against a local
 * WireMock token endpoint. Each scenario uses its own dedicated endpoint
 * key/client id so cached tokens never collide across scenarios.
 */
@Story("OAuth 2.0 access token strategy with expiry-aware caching")
public class OAuth2AccessTokenAuthStrategyTests implements BaseRestInterface {

    private static final String TOKEN_PATH = "/oauth/token";
    private static final String TARGET_PATH = "/protected-resource";

    private final RestStubServer stubServer = new RestStubServer();

    @BeforeClass(alwaysRun = true)
    public void startStubServer() {
        stubServer.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopStubServer() {
        stubServer.stop();
    }

    @BeforeMethod(alwaysRun = true)
    public void resetStubAndOverride() {
        stubServer.reset();
        stubServer.stubPostResponse(TARGET_PATH, 200, "{}");
        TokenEndpointOverride.set(stubServer.stubUrl(TOKEN_PATH));
    }

    @AfterMethod(alwaysRun = true)
    public void clearOverride() {
        TokenEndpointOverride.clear();
        TokenCacheRegistry.resetClockForTesting();
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("A first call with no cached token obtains an access token from the token endpoint and applies "
            + "it as Authorization: Bearer <access_token> on the outgoing request; exactly 1 token-endpoint call")
    public void shouldObtainAndApplyAccessToken() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJson("token-positive", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenPositiveDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer token-positive")));
        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A second request for the same identity within the cached token's expires_in window is served "
            + "from cache - exactly 1 token-endpoint call total across both requests")
    public void shouldServeSecondRequestFromCacheWithinExpiry() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJson("token-cachehit", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenCacheHitDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenCacheHitDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer token-cachehit")));
        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("Once the cached token's real expiry has passed (simulated via an injected Clock, no real sleep), "
            + "the next request is a cache miss - a second, fresh token-endpoint call is made and the new token "
            + "applied")
    public void shouldRefetchTokenAfterCachedTokenExpires() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJson("token-before-expiry", 60))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenCacheMissDemo", "{}",
                ResponseExpectation.status(200));

        // move "now" well past the cached token's real expiry (60s - 30s safety margin)
        TokenCacheRegistry.useClockForTesting(Clock.fixed(Instant.now().plus(Duration.ofHours(2)), ZoneOffset.UTC));

        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJson("token-after-expiry", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenCacheMissDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer token-after-expiry")));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @DataProvider(name = "distinctIdentities")
    public Object[][] distinctIdentities() {
        return new Object[][]{
                {"oauth2AccessTokenIdentityADemo", "token-identity-a"},
                {"oauth2AccessTokenIdentityBDemo", "token-identity-b"},
        };
    }

    @Test(groups = {"rest", "auth", "cache", "regression"}, dataProvider = "distinctIdentities")
    @Description("Two distinct client identities never share a cached token - each identity's request carries "
            + "only its own token")
    public void shouldIsolateTokensByIdentity(String endpointKey, String expectedToken) {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJson(expectedToken, 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, endpointKey, "{}", ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + expectedToken)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A token response with no expires_in/expires_at at all is still applied to the request, but "
            + "never cached - a subsequent call makes a fresh token-endpoint hit again")
    public void shouldApplyButNeverCacheTokenWithNoExpirySignal() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(tokenResponseJsonNoExpiry("token-no-expiry"))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenNoExpiryDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenNoExpiryDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2AccessTokenNoExpiryDemo", "{}",
                ResponseExpectation.status(200));

        Assert.assertEquals(stubServer.getServer().countRequestsMatching(
                        WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)).build()).getCount(), 3,
                "Expected every call to re-hit the token endpoint since no real expiry was ever known");
    }

    private static String tokenResponseJson(String accessToken, long expiresInSeconds) {
        return "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"Bearer\",\"expires_in\":"
                + expiresInSeconds + "}";
    }

    private static String tokenResponseJsonNoExpiry(String accessToken) {
        return "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"Bearer\"}";
    }
}
