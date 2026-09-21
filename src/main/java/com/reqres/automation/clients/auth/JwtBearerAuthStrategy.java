package com.reqres.automation.clients.auth;

import com.reqres.automation.clients.auth.cache.CachedToken;
import com.reqres.automation.clients.auth.cache.TokenCacheRegistry;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.JwtClaimsDecoder;
import io.restassured.builder.RequestSpecBuilder;

import java.time.Instant;
import java.util.Optional;

/**
 * Applies a pre-issued JWT (sourced from {@code <endpointKey>.auth.jwt.token},
 * same provisioning shape as {@link BearerAuthStrategy}'s static token) as
 * {@code Authorization: Bearer <jwt>}. No token-endpoint call is made. The
 * JWT's own {@code exp} claim ({@link JwtClaimsDecoder}) drives cache
 * validity, keyed by a hash of the configured JWT value itself (no separate
 * client id for this strategy); a JWT with no {@code exp} claim is still
 * applied but never cached.
 */
public class JwtBearerAuthStrategy implements AuthStrategy {

    private static final String STRATEGY_TYPE = "JWT_BEARER";

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String jwt = valueOrEmpty(config.getProperty(endpointKey + Constants.AUTH_JWT_TOKEN_SUFFIX));

        String identity = TokenAuthStrategySupport.resolveIdentity(endpointKey, config, jwt);
        TokenCacheRegistry cacheRegistry = TokenCacheRegistry.getInstance(config);
        String cacheKey = TokenAuthStrategySupport.buildCacheKey(STRATEGY_TYPE, endpointKey, identity);

        Optional<CachedToken> cached = cacheRegistry.get(cacheKey);
        if (cached.isPresent()) {
            TokenAuthStrategySupport.applyBearerHeader(builder, cached.get().tokenValue());
            return;
        }

        Optional<Instant> rawExpiry = JwtClaimsDecoder.decodeExpiry(jwt);
        rawExpiry.ifPresent(expiry -> cacheRegistry.put(cacheKey,
                CachedToken.withSafetyMargin(jwt, expiry, config.getAuthTokenCacheSafetyMarginSeconds())));

        TokenAuthStrategySupport.applyBearerHeader(builder, jwt);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
