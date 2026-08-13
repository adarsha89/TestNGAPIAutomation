package com.reqres.automation.clients.webhook;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.config.EnvConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Embedded WireMock lifecycle wrapper acting as a local inbound-webhook
 * receiver. Binds to an OS-assigned port by default (parallel-safe), and
 * exposes its base URL plus the underlying server so a test can stub the
 * expected call and later verify what was recorded.
 */
public class WebhookReceiver implements ApiClient {

    private WireMockServer server;

    @Override
    public void init(EnvConfig config) {
        this.server = new WireMockServer(WireMockConfiguration.options().port(config.getWebhookPort()));
        this.server.start();
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public WireMockServer getServer() {
        return server;
    }

    /**
     * Plays the role of the external caller invoking this receiver's
     * webhook endpoint - the client-layer wrapper tests must go through
     * instead of building a raw {@code RestAssured.given()} call inline,
     * consistent with every other protocol client in this framework.
     *
     * @param path        the webhook path being invoked (e.g. {@code /webhook/order-created})
     * @param jsonBody    the raw JSON payload the external caller sends
     */
    public Response sendCallerRequest(String path, String jsonBody) {
        return RestAssured.given()
                .baseUri(baseUrl())
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post(path);
    }

    @Override
    public void close() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
