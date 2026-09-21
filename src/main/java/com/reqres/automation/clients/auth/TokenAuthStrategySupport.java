package com.reqres.automation.clients.auth;

import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Package-private helper shared by the four token-acquiring
 * {@link AuthStrategy} implementations - cache-key identity resolution and
 * the common "apply as Bearer header" step, kept here once instead of
 * duplicated four times. Not a public API; kept internal to this package.
 */
final class TokenAuthStrategySupport {

    private TokenAuthStrategySupport() {
    }

    /**
     * Resolves the cache-key identity for {@code endpointKey}: the
     * configured {@code <endpointKey>.auth.client.id} when present,
     * otherwise a SHA-256 hash of {@code fallbackCredentialValue} (the
     * configured JWT/refresh-token value itself) - used by strategies with
     * no separate client id configured.
     */
    static String resolveIdentity(String endpointKey, EnvConfig config, String fallbackCredentialValue) {
        String clientId = config.getProperty(endpointKey + Constants.AUTH_CLIENT_ID_SUFFIX);
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }
        return sha256Hex(fallbackCredentialValue == null ? "" : fallbackCredentialValue);
    }

    static String buildCacheKey(String strategyType, String endpointKey, String identity) {
        return TokenCacheRegistry.buildKey(strategyType, endpointKey, identity);
    }

    static void applyBearerHeader(RequestSpecBuilder builder, String tokenValue) {
        builder.addHeader(Constants.HEADER_AUTHORIZATION, "Bearer " + (tokenValue == null ? "" : tokenValue));
    }

    /**
     * Resolves a token response's real (pre-safety-margin) expiry from
     * either an {@code expires_in} (seconds from now, per {@code clock})
     * or an absolute {@code expires_at} (epoch seconds), preferring
     * {@code expires_in} when both are present. Empty when neither field
     * is present - the "never cache" no-expiry-signal fallback.
     */
    static Optional<Instant> resolveRawExpiry(Long expiresInSeconds, Long expiresAtEpochSeconds, Clock clock) {
        if (expiresInSeconds != null) {
            return Optional.of(Instant.now(clock).plusSeconds(expiresInSeconds));
        }
        if (expiresAtEpochSeconds != null) {
            return Optional.of(Instant.ofEpochSecond(expiresAtEpochSeconds));
        }
        return Optional.empty();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
