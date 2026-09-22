package com.reqres.automation.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.base.BaseRestInterface;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.auth.token.TokenEndpointOverride;
import com.reqres.automation.clients.rest.RestStubServer;
import com.reqres.automation.testdata.ResponseExpectation;
import com.reqres.automation.utils.LogMasker;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Covers {@code OAuth2RefreshTokenAuthStrategy} end to end against a local
 * WireMock token endpoint. Each scenario uses its own dedicated endpoint
 * key/client id so cached tokens never collide across scenarios.
 */
@Story("OAuth 2.0 refresh token grant strategy with expiry-aware caching")
public class OAuth2RefreshTokenAuthStrategyTests implements BaseRestInterface {

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
    @Description("The refresh grant call (grant_type=refresh_token) is made against the token endpoint and the "
            + "resulting access token is applied as Authorization: Bearer <access_token>; exactly 1 call")
    public void shouldPerformRefreshGrantAndApplyAccessToken() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(refreshResponseJson("refreshed-access-token", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenPositiveDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH))
                .withRequestBody(WireMock.containing("grant_type=refresh_token"))
                .withRequestBody(WireMock.containing("refresh_token=tGzv3JOkF0XG5Qx2TlKWIA")));
        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer refreshed-access-token")));
        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("A second request for the same identity within the derived access token's expires_in window is "
            + "a cache hit - no second refresh-grant call")
    public void shouldServeSecondRequestFromCacheWithinExpiry() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(refreshResponseJson("cachehit-access-token", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenCacheHitDemo", "{}",
                ResponseExpectation.status(200));
        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenCacheHitDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer cachehit-access-token")));
    }

    @Test(groups = {"rest", "auth", "cache", "regression"})
    @Description("Once the derived access token's real expiry has passed (simulated via an injected Clock), a "
            + "fresh refresh-grant call is made")
    public void shouldRefetchAfterDerivedAccessTokenExpires() {
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(refreshResponseJson("before-expiry-access-token", 60))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenCacheMissDemo", "{}",
                ResponseExpectation.status(200));

        TokenCacheRegistry.useClockForTesting(Clock.fixed(Instant.now().plus(Duration.ofHours(2)), ZoneOffset.UTC));

        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(refreshResponseJson("after-expiry-access-token", 3600))));

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenCacheMissDemo", "{}",
                ResponseExpectation.status(200));

        stubServer.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(TARGET_PATH))
                .withHeader("Authorization", WireMock.equalTo("Bearer after-expiry-access-token")));
        stubServer.getServer().verify(2, WireMock.postRequestedFor(WireMock.urlEqualTo(TOKEN_PATH)));
    }

    @Test(groups = {"config", "regression"})
    @Description("Every refresh-token-bearing demo property used by this strategy's fixture variants is registered "
            + "as sensitive, so LogMasker masks it wherever it's logged")
    public void shouldTreatAllRefreshTokenDemoTokensAsSensitive() {
        var sensitiveNames = restService().config().getSensitiveDataNames();

        Assert.assertTrue(LogMasker.isSensitive("oauth2RefreshTokenPositiveDemo.auth.refresh.token", sensitiveNames));
        Assert.assertTrue(LogMasker.isSensitive("oauth2RefreshTokenCacheHitDemo.auth.refresh.token", sensitiveNames));
        Assert.assertTrue(
                LogMasker.isSensitive("oauth2RefreshTokenCacheMissDemo.auth.refresh.token", sensitiveNames));
    }

    @Test(groups = {"rest", "auth", "regression"})
    @Description("The refresh_token form field sent to the token endpoint is masked in the Allure request "
            + "attachment, while the non-sensitive grant_type field stays visible")
    public void shouldMaskRefreshTokenFormBodyValueInTokenEndpointCallAttachment() throws IOException {
        // dedicated endpoint key/client id, per this class's convention, so this test's cached
        // token never collides with shouldPerformRefreshGrantAndApplyAccessToken's
        stubServer.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(TOKEN_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(refreshResponseJson("masking-verification-access-token", 3600))));

        Set<String> attachmentFileNamesBeforeCall = existingAttachmentFileNames();

        restService().postToOverrideBaseUriAndVerify(
                stubServer.stubUrl(""), TARGET_PATH, "oauth2RefreshTokenMaskingDemo", "{}",
                ResponseExpectation.status(200));

        String tokenRequestAttachment = readTokenEndpointRequestAttachment(attachmentFileNamesBeforeCall);
        Assert.assertNotNull(tokenRequestAttachment,
                "Expected an Allure request attachment recorded for the token endpoint call");
        Assert.assertTrue(tokenRequestAttachment.contains("refresh_token=***MASKED***"),
                "Expected refresh_token to be masked in the attachment, got:\n" + tokenRequestAttachment);
        Assert.assertFalse(tokenRequestAttachment.contains("tGzv3JOkF0XG5Qx2TlKWIA"),
                "Real refresh token value must not appear in the attachment, got:\n" + tokenRequestAttachment);
        Assert.assertTrue(tokenRequestAttachment.contains("grant_type=refresh_token"),
                "Expected non-sensitive grant_type to remain unmasked in the attachment, got:\n"
                        + tokenRequestAttachment);
    }

    // the stub server's port is stable across every method in this class, so a stubbed URL
    // match alone can't tell this call's attachment apart from an earlier method's; snapshot
    // the attachment file names before the call and keep only files absent from that snapshot,
    // then apply the existing URL/"Query Params:" markers to pick the token-endpoint request
    private Set<String> existingAttachmentFileNames() throws IOException {
        Path resultsDir = Path.of("target", "allure-results");
        if (!Files.isDirectory(resultsDir)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(resultsDir)) {
            return files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.contains("-attachment"))
                    .collect(Collectors.toSet());
        }
    }

    // reads only attachments this test's own call produced (absent from the pre-call
    // snapshot), so a stable port shared with earlier methods in this class can't cause
    // a different test's attachment to be picked
    private String readTokenEndpointRequestAttachment(Set<String> attachmentFileNamesBeforeCall)
            throws IOException {
        Path resultsDir = Path.of("target", "allure-results");
        if (!Files.isDirectory(resultsDir)) {
            return null;
        }
        String urlMarker = "URL: " + stubServer.stubUrl(TOKEN_PATH);
        try (Stream<Path> files = Files.list(resultsDir)) {
            return files
                    .filter(path -> path.getFileName().toString().contains("-attachment"))
                    .filter(path -> !attachmentFileNamesBeforeCall.contains(path.getFileName().toString()))
                    .map(this::readFileQuietly)
                    .filter(content -> content.contains(urlMarker) && content.contains("Query Params:"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String readFileQuietly(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    private static String refreshResponseJson(String accessToken, long expiresInSeconds) {
        return "{\"access_token\":\"" + accessToken + "\",\"token_type\":\"Bearer\",\"expires_in\":"
                + expiresInSeconds + ",\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\"}";
    }
}
