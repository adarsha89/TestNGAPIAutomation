package com.reqres.automation.clients.auth;

import com.reqres.automation.clients.auth.cache.CachedToken;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.auth.token.TokenEndpointClient;
import com.reqres.automation.clients.auth.token.TokenResponse;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth 2.0 refresh-token grant strategy (RFC 6749 §6) - performs the
 * refresh grant call (seed refresh token from
 * {@code <endpointKey>.auth.refresh.token}) to obtain a fresh access token,
 * applies it as {@code Authorization: Bearer <access_token>}, and caches the
 * derived access token keyed by the configured
 * {@code <endpointKey>.auth.client.id} (falling back to a hash of the seed
 * refresh-token value when no client id is configured).
 */
public class OAuth2RefreshTokenAuthStrategy implements AuthStrategy {

    private static final String STRATEGY_TYPE = "OAUTH2_REFRESH_TOKEN";

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String refreshToken = valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_REFRESH_TOKEN_SUFFIX));

        String identity = TokenAuthStrategySupport.resolveIdentity(endpointKey, config, refreshToken);
        TokenCacheRegistry cacheRegistry = TokenCacheRegistry.getInstance(config);
        String cacheKey = TokenAuthStrategySupport.buildCacheKey(STRATEGY_TYPE, endpointKey, identity);

        Optional<CachedToken> cached = cacheRegistry.get(cacheKey);
        if (cached.isPresent()) {
            TokenAuthStrategySupport.applyBearerHeader(builder, cached.get().tokenValue());
            return;
        }

        TokenResponse tokenResponse = TokenEndpointClient.requestToken(
                endpointKey, config, formParams(config, endpointKey, refreshToken));
        String accessToken = tokenResponse.getAccessToken();

        Optional<Instant> rawExpiry = TokenAuthStrategySupport.resolveRawExpiry(
                tokenResponse.getExpiresIn(), tokenResponse.getExpiresAt(), cacheRegistry.clock());
        rawExpiry.ifPresent(expiry -> cacheRegistry.put(cacheKey,
                CachedToken.withSafetyMargin(accessToken, expiry, config.getAuthTokenCacheSafetyMarginSeconds())));

        TokenAuthStrategySupport.applyBearerHeader(builder, accessToken);
    }

    private static Map<String, String> formParams(EnvConfig config, String endpointKey, String refreshToken) {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "refresh_token");
        formParams.put("refresh_token", refreshToken);
        formParams.put("client_id", valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_CLIENT_ID_SUFFIX)));
        formParams.put("client_secret",
                valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_CLIENT_SECRET_SUFFIX)));
        return formParams;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
