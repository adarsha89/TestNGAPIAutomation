package com.reqres.automation.clients.auth;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Adds an {@code Authorization: Basic base64(username:password)} header,
 * sourced from {@code <endpointKey>.auth.username}/{@code .auth.password}.
 */
public class BasicAuthStrategy implements AuthStrategy {

    @Override
    public void apply(RequestSpecBuilder builder, EnvConfig config, String endpointKey) {
        String username = config.getProperty(endpointKey + Constants.AUTH_USERNAME_SUFFIX);
        String password = config.getProperty(endpointKey + Constants.AUTH_PASSWORD_SUFFIX);
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        builder.addHeader(Constants.HEADER_AUTHORIZATION, "Basic " + encoded);
    }
}
