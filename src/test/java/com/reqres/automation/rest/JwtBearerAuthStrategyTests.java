package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.auth.cache.CachedToken;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.LogMasker;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Covers {@code JwtBearerAuthStrategy} - applies a pre-issued JWT, no
 * token-endpoint call. Each scenario uses its own dedicated endpoint
 * key/client id so cached tokens never collide across scenarios.
 */
@Story("JWT Bearer strategy driven by the decoded exp claim")
public class JwtBearerAuthStrategyTests implements BaseRestInterface {

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
    public void resetStub() {
        stubServer.reset();
        stubServer.stubPostResponse(TARGET_PATH, 200, "{}");
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("The pre-issued JWT configured for the endpoint key is applied as Authorization: Bearer <jwt>")
    public void shouldApplyPreIssuedJwtAsBearerCredential() {
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "jwtBearerPositiveDemo", "{}", ResponseExpectation.status(200));

        String configuredJwt = restService().config().getProperty("jwtBearerPositiveDemo.auth.jwt.token");
        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + configuredJwt)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A JWT whose decoded exp claim is in the future is cached, keyed by the configured client id - "
            + "applying it again still sends the same token and the cache entry remains present")
    public void shouldCacheJwtWithFutureExpClaim() {
        String cacheKey = TokenCacheRegistry.buildKey("JWT_BEARER", "jwtBearerValidCacheDemo", "jwt-bearer-cache");

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "jwtBearerValidCacheDemo", "{}", ResponseExpectation.status(200));

        Optional<CachedToken> cached = TokenCacheRegistry.getInstance(restService().config()).get(cacheKey);
        Assert.assertTrue(cached.isPresent(), "Expected a JWT with a future exp claim to be cached");

        String configuredJwt = restService().config().getProperty("jwtBearerValidCacheDemo.auth.jwt.token");
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "jwtBearerValidCacheDemo", "{}", ResponseExpectation.status(200));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + configuredJwt)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A JWT whose decoded exp claim is already in the past is still applied to the request, but never "
            + "cached - not a stale cache hit")
    public void shouldNotCacheJwtWithPastExpClaim() {
        String cacheKey = TokenCacheRegistry.buildKey("JWT_BEARER", "jwtBearerExpiredDemo", "jwt-bearer-expired");

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "jwtBearerExpiredDemo", "{}", ResponseExpectation.status(200));

        Optional<CachedToken> cached = TokenCacheRegistry.getInstance(restService().config()).get(cacheKey);
        Assert.assertTrue(cached.isEmpty(), "Expected a JWT with an already-past exp claim to never be cached");

        String configuredJwt = restService().config().getProperty("jwtBearerExpiredDemo.auth.jwt.token");
        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + configuredJwt)));
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("A JWT with no exp claim at all is applied to the request but never cached")
    public void shouldApplyJwtWithNoExpClaimWithoutCaching() {
        String cacheKey = TokenCacheRegistry.buildKey("JWT_BEARER", "jwtBearerNoExpDemo", "jwt-bearer-noexp");

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "jwtBearerNoExpDemo", "{}", ResponseExpectation.status(200));

        Optional<CachedToken> cached = TokenCacheRegistry.getInstance(restService().config()).get(cacheKey);
        Assert.assertTrue(cached.isEmpty(), "Expected a JWT with no exp claim to never be cached");

        String configuredJwt = restService().config().getProperty("jwtBearerNoExpDemo.auth.jwt.token");
        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer " + configuredJwt)));
    }

    @Test(groups = {"config", "regression"})
    @Description("Every JWT-bearing demo property used by this strategy's fixture variants is registered as "
            + "sensitive, so LogMasker masks it wherever it's logged")
    public void shouldTreatAllJwtDemoTokensAsSensitive() {
        var sensitiveNames = restService().config().getSensitiveDataNames();

        Assert.assertTrue(LogMasker.isSensitive("jwtBearerPositiveDemo.auth.jwt.token", sensitiveNames));
        Assert.assertTrue(LogMasker.isSensitive("jwtBearerValidCacheDemo.auth.jwt.token", sensitiveNames));
        Assert.assertTrue(LogMasker.isSensitive("jwtBearerExpiredDemo.auth.jwt.token", sensitiveNames));
        Assert.assertTrue(LogMasker.isSensitive("jwtBearerNoExpDemo.auth.jwt.token", sensitiveNames));
    }
}
