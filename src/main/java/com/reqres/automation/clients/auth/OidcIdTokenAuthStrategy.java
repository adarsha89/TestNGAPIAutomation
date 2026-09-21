package com.reqres.automation.clients.auth;

import com.reqres.automation.clients.auth.cache.CachedToken;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.clients.auth.token.TokenEndpointClient;
import com.reqres.automation.clients.auth.token.TokenResponse;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.JwtClaimsDecoder;
import io.restassured.builder.RequestSpecBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * OIDC ID Token strategy (OIDC Core 1.0 §3.1.3.3) - requests a token
 * response carrying an {@code id_token} (JWT), applies it as
 * {@code Authorization: Bearer <id_token>}, and caches it keyed by the
 * configured {@code <endpointKey>.auth.client.id}. Expiry is taken from the
 * HTTP {@code expires_in}/{@code expires_at} field when present, otherwise
 * decoded from the {@code id_token}'s own {@code exp} claim
 * ({@link JwtClaimsDecoder}); when neither yields an expiry (including a
 * malformed/non-JWT {@code id_token}), the token is still applied but never
 * cached.
 */
public class OidcIdTokenAuthStrategy implements AuthStrategy {

    private static final String STRATEGY_TYPE = "OIDC_ID_TOKEN";

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
        String idToken = tokenResponse.getIdToken();

        Optional<Instant> rawExpiry = TokenAuthStrategySupport.resolveRawExpiry(
                tokenResponse.getExpiresIn(), tokenResponse.getExpiresAt(), cacheRegistry.clock());
        if (rawExpiry.isEmpty()) {
            rawExpiry = JwtClaimsDecoder.decodeExpiry(idToken);
        }
        rawExpiry.ifPresent(expiry -> cacheRegistry.put(cacheKey,
                CachedToken.withSafetyMargin(idToken, expiry, config.getAuthTokenCacheSafetyMarginSeconds())));

        TokenAuthStrategySupport.applyBearerHeader(builder, idToken);
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
