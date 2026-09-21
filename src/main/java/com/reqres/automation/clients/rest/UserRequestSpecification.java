package com.reqres.automation.clients.rest;

import com.reqres.automation.clients.RequestSpecFactory;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.Constants;
import io.restassured.specification.RequestSpecification;

/**
 * Immutable holder of the endpoint key, {@link EnvConfig}, and built
 * {@link RequestSpecification} that together define how a call is made
 * against the Users API. {@link RestClientBase} and {@link UserRestClient}
 * resolve their request configuration from this.
 */
public final class UserRequestSpecification {

    private final String endpointKey;
    private final EnvConfig config;
    private final RequestSpecification spec;

    private UserRequestSpecification(String endpointKey, EnvConfig config, RequestSpecification spec) {
        this.endpointKey = endpointKey;
        this.config = config;
        this.spec = spec;
    }

    /** Builds the Users endpoint's request configuration for {@code config}. */
    public static UserRequestSpecification forUsersEndpoint(EnvConfig config) {
        RequestSpecification spec = RequestSpecFactory.baseSpecBuilder(
                config.getRestBaseUrl(), config, Constants.USERS_ENDPOINT_KEY).build();
        return new UserRequestSpecification(Constants.USERS_ENDPOINT_KEY, config, spec);
    }

    public String endpointKey() {
        return endpointKey;
    }

    public EnvConfig config() {
        return config;
    }

    public RequestSpecification spec() {
        return spec;
    }
}
