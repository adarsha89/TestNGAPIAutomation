package com.reqres.automation.clients.rest;

import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.clients.RequestSpecFactory;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

/**
 * Shared {@link RequestSpecification} setup (base URI, auth header,
 * enforced HTTP timeout, masked logging filter, Allure attachment filter)
 * for all REST clients.
 */
public class RestClientBase implements ApiClient {

    protected RequestSpecification spec;

    @Override
    public void init(EnvConfig config) {
        RequestSpecBuilder builder = RequestSpecFactory.baseSpecBuilder(config.getRestBaseUrl(), config);

        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader(Constants.HEADER_API_KEY, apiKey);
        }

        this.spec = builder.build();
    }

    @Override
    public void close() {
        this.spec = null;
    }
}
