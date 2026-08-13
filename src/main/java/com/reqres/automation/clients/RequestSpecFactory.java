package com.reqres.automation.clients;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.util.LogMasker;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

/**
 * Builds the shared, protocol-agnostic {@link RequestSpecBuilder} setup
 * (base URI, JSON content type, {@link RestAssuredConfigFactory}-derived
 * timeout/logging config, masked logging filter, Allure attachment filter)
 * consumed by every protocol client's {@code init(EnvConfig)}.
 * <p>
 * Returns an <b>unbuilt</b> builder so callers can layer protocol-specific
 * behavior (e.g. REST's conditional API-key header) before calling
 * {@code .build()}.
 */
public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    public static RequestSpecBuilder baseSpecBuilder(String baseUri, EnvConfig config) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setConfig(RestAssuredConfigFactory.from(config))
                .addFilter(new LogMasker.MaskingLoggingFilter())
                .addFilter(new AllureRestAssured());
    }
}
