package com.reqres.automation.clients;

import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.utils.LogMasker;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * Builds the shared {@link RestAssuredConfig} applied to every REST/GraphQL
 * client's {@code RequestSpecification}:
 * <ul>
 *   <li>enforces the configured {@code request.timeout.ms} (see
 *       {@link EnvConfig#getRequestTimeoutMs()}) as both the HTTP connection
 *       and socket timeout, so a hung/slow call has a framework-enforced
 *       upper bound instead of relying on an unbounded default;</li>
 *   <li>blacklists {@link EnvConfig#getSensitiveDataNames()} via RestAssured's
 *       own {@link LogConfig#blacklistHeaders} - this is not the primary
 *       masking mechanism (that is {@link LogMasker.MaskingLoggingFilter},
 *       which builds and attaches its own masked request/response records
 *       directly to the Allure report), it is kept only as harmless
 *       defense-in-depth for any future direct RestAssured {@code .log()}
 *       usage.</li>
 * </ul>
 * Used by every {@code clients/*} implementation instead of each one
 * re-deriving this config independently.
 */
public final class RestAssuredConfigFactory {

    private RestAssuredConfigFactory() {
    }

    public static RestAssuredConfig from(EnvConfig config) {
        int timeoutMs = config.getRequestTimeoutMs();
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMs)
                        .setParam("http.socket.timeout", timeoutMs))
                .logConfig(LogConfig.logConfig().blacklistHeaders(config.getSensitiveDataNames()));
    }
}
