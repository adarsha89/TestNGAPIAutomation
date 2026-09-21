package com.reqres.automation.clients.auth.token;

import com.reqres.automation.clients.RestAssuredConfigFactory;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import com.reqres.automation.utils.LogMasker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Small internal RestAssured POST helper for a token endpoint, issuing a
 * {@code application/x-www-form-urlencoded} grant request per RFC 6749 and
 * deserializing the JSON response into a {@link TokenResponse}. Used by the
 * OAuth2/OIDC token-acquiring {@code AuthStrategy} implementations - never
 * called directly by test code.
 */
public final class TokenEndpointClient {

    private TokenEndpointClient() {
    }

    /**
     * Posts {@code formParams} to the token endpoint resolved for
     * {@code endpointKey} - {@link TokenEndpointOverride}'s thread-local
     * value if set (test-support), otherwise the configured
     * {@code <endpointKey>.auth.token.endpoint} property. Built through the
     * same shared {@link RestAssuredConfigFactory} config and
     * {@link LogMasker.MaskingLoggingFilter} every other outbound call in
     * the framework uses, so this request also gets the configured
     * connection/socket timeouts and masked request/response logging.
     */
    public static TokenResponse requestToken(String endpointKey, EnvConfig config, Map<String, String> formParams) {
        String tokenEndpointUrl = resolveTokenEndpointUrl(endpointKey, config);

        Response response = RestAssured.given()
                .config(RestAssuredConfigFactory.from(config))
                .filter(new LogMasker.MaskingLoggingFilter(config.getSensitiveDataNames()))
                .contentType(ContentType.URLENC)
                .formParams(formParams)
                .when()
                .post(tokenEndpointUrl);

        return response.as(TokenResponse.class);
    }

    private static String resolveTokenEndpointUrl(String endpointKey, EnvConfig config) {
        String override = TokenEndpointOverride.get();
        if (override != null && !override.isBlank()) {
            return override;
        }
        return config.getProperty(endpointKey + Constants.AUTH_TOKEN_ENDPOINT_SUFFIX);
    }
}
