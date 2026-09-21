package com.reqres.automation.clients.rest;

import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.config.EnvConfig;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * Shared {@link RequestSpecification} setup (base URI, configured auth
 * strategy, enforced HTTP timeout, masked logging filter, Allure attachment
 * filter) for all REST clients.
 */
public class RestClientBase implements ApiClient {

    protected UserRequestSpecification userRequestSpecification;

    @Override
    public void init(EnvConfig config) {
        this.userRequestSpecification = UserRequestSpecification.forUsersEndpoint(config);
    }

    /**
     * Single choke point for building a request against this client's own
     * configured endpoint key. Rate-limit pacing is the caller's
     * responsibility (see {@code RestUserService}, which calls
     * {@code RateLimitHelper.acquire} explicitly before each outbound call).
     */
    protected RequestSpecification given() {
        return RestAssured.given().spec(userRequestSpecification.spec());
    }

    @Override
    public void close() {
        this.userRequestSpecification = null;
    }
}
