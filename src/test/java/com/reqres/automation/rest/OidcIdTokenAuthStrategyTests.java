package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.auth.token.TokenEndpointOverride;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
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
 * Covers {@code OidcIdTokenAuthStrategy} end to end against a local WireMock
 * token endpoint. Each scenario uses its own dedicated endpoint key/client
 * id so cached tokens never collide across scenarios.
 */
@Story("OIDC ID token strategy with HTTP-field/JWT-exp expiry sourcing")
public class OidcIdTokenAuthStrategyTests implements BaseRestInterface {

    private static final String TOKEN_PATH = "/oauth/token";
    private static final String TARGET_PATH = "/protected-resource";

    // exp=4102444800 -> 2100-01-01T00:00:00Z (far future, always valid for this suite's lifetime)
    private static final String JWT_WITH_FAR_FUTURE_EXP =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                    + ".eyJpc3MiOiJodHRwczovL2lzc3Vlci5leGFtcGxlLmNvbSIsInN1YiI6IjI0OCIsImV4cCI6NDEwMjQ0NDgwMH0"
                    + ".sig";
    private static final String MALFORMED_ID_TOKEN = "not-a-jwt";

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
    @Description("The id_token (JWT) is obtained from the token endpoint and applied as "
            + "Authorization: Bearer <id_token> on the outgoing request; exactly 1 token-endpoint call")
    public void shouldObtainAndApplyIdToken() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(idTokenResponseJson(JWT_WITH_FAR_FUTURE_EXP, 3600L))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenPositiveDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + JWT_WITH_FAR_FUTURE_EXP)));
        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @DataProvider(name = "expirySourceVariants")
    public Object[][] expirySourceVariants() {
        return new Object[][]{
                // endpointKey, expiresIn (nullable), idTokenHasExp
                {"oidcIdTokenHttpOnlyDemo", 3600L, false},
                {"oidcIdTokenJwtOnlyDemo", null, true},
                {"oidcIdTokenBothDemo", 3600L, true},
        };
    }

    @Test(groups = {"rest", "auth", "cache", "regression"}, dataProvider = "expirySourceVariants")
    @Description("A cache hit/miss decision behaves correctly regardless of whether expiry comes from the HTTP "
            + "expires_in field, the decoded JWT exp claim, or both agreeing - a second call within the derived "
            + "expiry window is a cache hit (no second token-endpoint call)")
    public void shouldDeriveExpiryAndCacheAcrossSources(String endpointKey, Long expiresIn, boolean idTokenHasExp) {
        String idToken = idTokenHasExp ? JWT_WITH_FAR_FUTURE_EXP : "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.sig";

        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(idTokenResponseJson(idToken, expiresIn))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, endpointKey, "{}", ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, endpointKey, "{}", ResponseExpectation.status(200));

        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + idToken)));
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("A malformed/non-JWT id_token yields no derivable exp claim - the token is still applied to the "
            + "outgoing request (no exception propagated) but the no-expiry-signal policy means it is never cached")
    public void shouldApplyMalformedIdTokenWithoutCachingOrThrowing() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(idTokenResponseJson(MALFORMED_ID_TOKEN, null))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenMalformedDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenMalformedDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + MALFORMED_ID_TOKEN)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A cache miss attributable purely to the HTTP expires_in field: the id_token carries no exp claim, "
            + "so once the cached entry's HTTP-derived expiry passes (simulated via an injected Clock), the next "
            + "call re-hits the token endpoint")
    public void shouldRefetchAfterHttpFieldExpiry() {
        String idTokenWithNoExp = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.sig";
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(idTokenResponseJson(idTokenWithNoExp, 60L))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenHttpFieldExpiredDemo", "{}",
                ResponseExpectation.status(200));

        TokenCacheRegistry.useClockForTesting(Clock.fixed(Instant.now().plus(Duration.ofHours(2)), ZoneOffset.UTC));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenHttpFieldExpiredDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A cache miss attributable purely to the JWT exp claim: the id_token's decoded exp is already in "
            + "the past and no expires_in field is present, so every subsequent call re-hits the token endpoint")
    public void shouldRefetchAfterJwtExpClaimExpiry() {
        // exp=946684800 -> 2000-01-01T00:00:00Z, already past
        String idTokenWithPastExp = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                + ".eyJzdWIiOiJ0ZXN0LWNsaWVudCIsImV4cCI6OTQ2Njg0ODAwfQ.sig";
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(idTokenResponseJson(idTokenWithPastExp, null))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenJwtExpExpiredDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oidcIdTokenJwtExpExpiredDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    private static String idTokenResponseJson(String idToken, Long expiresIn) {
        String expiresInField = expiresIn == null ? "" : ",\"expires_in\":" + expiresIn;
        return "{\"access_token\":\"access-companion\",\"token_type\":\"Bearer\",\"id_token\":\"" + idToken + "\""
                + expiresInField + "}";
    }
}
