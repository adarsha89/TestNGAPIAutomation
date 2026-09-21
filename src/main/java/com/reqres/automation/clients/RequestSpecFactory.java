package com.reqres.automation.clients;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.helpers.AuthStrategyFactory;
import com.reqres.automation.utils.LogMasker;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

/**
 * Builds the shared, protocol-agnostic {@link RequestSpecBuilder} setup
 * (base URI, JSON content type, {@link RestAssuredConfigFactory}-derived
 * timeout/logging config, masked logging filter which also attaches masked
 * request/response detail to the Allure report)
 * consumed by every protocol client's {@code init(EnvConfig)}.
 * <p>
 * Returns an <b>unbuilt</b> builder so callers can layer protocol-specific
 * behavior before calling {@code .build()}.
 */
public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    public static RequestSpecBuilder baseSpecBuilder(String baseUri, EnvConfig config) {
        return baseSpecBuilder(baseUri, config, null);
    }

    /**
     * Same as {@link #baseSpecBuilder(String, EnvConfig)}, additionally
     * resolving and applying the {@code endpointKey}'s configured
     * {@link com.reqres.automation.clients.auth.AuthStrategy}, if any.
     * {@code endpointKey} of {@code null} resolves to "no mechanism
     * configured", identical to the no-endpointKey overload.
     */
    public static RequestSpecBuilder baseSpecBuilder(String baseUri, EnvConfig config, String endpointKey) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setConfig(RestAssuredConfigFactory.from(config))
                .addFilter(new LogMasker.MaskingLoggingFilter(config.getSensitiveDataNames()));

        AuthStrategyFactory.resolve(endpointKey, config)
                .ifPresent(strategy -> strategy.apply(builder, config, endpointKey));

        return builder;
    }
}
