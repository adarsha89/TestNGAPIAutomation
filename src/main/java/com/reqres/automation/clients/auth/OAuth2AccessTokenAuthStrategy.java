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
 * OAuth 2.0 client-credentials access-token strategy (RFC 6749 §5.1) -
 * requests an access token from {@code <endpointKey>.auth.token.endpoint}
 * (or {@code TokenEndpointOverride} in tests), caches it keyed by the
 * configured {@code <endpointKey>.auth.client.id}, and applies it as
 * {@code Authorization: Bearer <access_token>}. A token response with no
 * {@code expires_in}/{@code expires_at} is still applied but never cached -
 * caching would mean holding onto a token whose real lifetime is unknown.
 */
public class OAuth2AccessTokenAuthStrategy implements AuthStrategy {

    private static final String STRATEGY_TYPE = "OAUTH2_ACCESS_TOKEN";

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String clientId = valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_CLIENT_ID_SUFFIX));
        TokenCacheRegistry cacheRegistry = TokenCacheRegistry.getInstance(config);
        String cacheKey = TokenAuthStrategySupport.buildCacheKey(STRATEGY_TYPE, endpointKey, clientId);

        Optional<CachedToken> cached = cacheRegistry.get(cacheKey);
        if (cached.isPresent()) {
            TokenAuthStrategySupport.applyBearerHeader(builder, cached.get().tokenValue());
            return;
        }

        TokenResponse tokenResponse = TokenEndpointClient.requestToken(endpointKey, config, formParams(config, endpointKey));
        String accessToken = tokenResponse.getAccessToken();

        Optional<Instant> rawExpiry = TokenAuthStrategySupport.resolveRawExpiry(
                tokenResponse.getExpiresIn(), tokenResponse.getExpiresAt(), cacheRegistry.clock());
        rawExpiry.ifPresent(expiry -> cacheRegistry.put(cacheKey,
                CachedToken.withSafetyMargin(accessToken, expiry, config.getAuthTokenCacheSafetyMarginSeconds())));

        TokenAuthStrategySupport.applyBearerHeader(builder, accessToken);
    }

    private static Map<String, String> formParams(EnvConfig config, String endpointKey) {
        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "client_credentials");
        formParams.put("client_id", valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_CLIENT_ID_SUFFIX)));
        formParams.put("client_secret",
                valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_CLIENT_SECRET_SUFFIX)));
        String scope = config.getProperty(endpointKey + Constants.AUTH_SCOPE_SUFFIX);
        if (scope != null && !scope.isBlank()) {
            formParams.put("scope", scope);
        }
        return formParams;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
