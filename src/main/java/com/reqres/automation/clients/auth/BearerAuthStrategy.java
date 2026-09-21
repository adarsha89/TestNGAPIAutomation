package com.reqres.automation.clients.auth;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;

/**
 * Adds an {@code Authorization: Bearer <token>} header, sourced from the
 * pre-configured {@code <endpointKey>.auth.bearer.token} property. There's
 * no token-acquisition flow - the token is expected to already be valid.
 */
public class BearerAuthStrategy implements AuthStrategy {

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String token = config.getProperty(endpointKey + Constants.AUTH_BEARER_TOKEN_SUFFIX);
        if (token == null) {
            token = "";
        }
        builder.addHeader(Constants.HEADER_AUTHORIZATION, "Bearer " + token);
    }
}
