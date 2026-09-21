package com.reqres.automation.clients.auth;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;

/**
 * Adds the API key header using the {@code api.key} config value.
 * Header name is configurable per endpoint via
 * {@code <endpointKey>.auth.header.name}, defaulting to
 * {@link Constants#HEADER_API_KEY}.
 */
public class ApiKeyAuthStrategy implements AuthStrategy {

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        String headerName = config.getProperty(endpointKey + Constants.AUTH_HEADER_NAME_SUFFIX);
        if (headerName == null || headerName.isBlank()) {
            headerName = Constants.HEADER_API_KEY;
        }
        builder.addHeader(headerName, apiKey);
    }
}
