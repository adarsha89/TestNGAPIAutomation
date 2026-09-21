package com.reqres.automation.helpers;

import com.reqres.automation.clients.auth.ApiKeyAuthStrategy;
import com.reqres.automation.clients.auth.AuthStrategy;
import com.reqres.automation.clients.auth.BasicAuthStrategy;
import com.reqres.automation.clients.auth.BearerAuthStrategy;
import com.reqres.automation.clients.auth.JwtBearerAuthStrategy;
import com.reqres.automation.clients.auth.OAuth2AccessTokenAuthStrategy;
import com.reqres.automation.clients.auth.OAuth2RefreshTokenAuthStrategy;
import com.reqres.automation.clients.auth.OidcIdTokenAuthStrategy;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;

import java.util.Locale;
import java.util.Optional;

/**
 * Per-endpoint-key dispatch: resolves the configured
 * {@code <endpointKey>.auth.strategy} (API_KEY/BASIC/BEARER/
 * OAUTH2_ACCESS_TOKEN/OIDC_ID_TOKEN/JWT_BEARER/OAUTH2_REFRESH_TOKEN) to its
 * {@link AuthStrategy} implementation. Returns {@link Optional#empty()}
 * (no-op) when absent/blank or when {@code endpointKey} is {@code null}.
 */
public final class AuthStrategyFactory {

    private AuthStrategyFactory() {
    }

    public static Optional<AuthStrategy> resolve(String endpointKey, EnvConfig config) {
        if (endpointKey == null || endpointKey.isBlank()) {
            return Optional.empty();
        }
        String strategyName = config.getProperty(endpointKey + Constants.AUTH_STRATEGY_SUFFIX);
        if (strategyName == null || strategyName.isBlank()) {
            return Optional.empty();
        }
        switch (strategyName.trim().toUpperCase(Locale.ROOT)) {
            case "API_KEY":
                return Optional.of(new ApiKeyAuthStrategy());
            case "BASIC":
                return Optional.of(new BasicAuthStrategy());
            case "BEARER":
                return Optional.of(new BearerAuthStrategy());
            case "OAUTH2_ACCESS_TOKEN":
                return Optional.of(new OAuth2AccessTokenAuthStrategy());
            case "OIDC_ID_TOKEN":
                return Optional.of(new OidcIdTokenAuthStrategy());
            case "JWT_BEARER":
                return Optional.of(new JwtBearerAuthStrategy());
            case "OAUTH2_REFRESH_TOKEN":
                return Optional.of(new OAuth2RefreshTokenAuthStrategy());
            default:
                throw new IllegalArgumentException(
                        "Unsupported auth strategy '" + strategyName + "' configured for endpoint key '"
                                + endpointKey + "'");
        }
    }
}
