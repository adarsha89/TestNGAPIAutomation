package com.reqres.automation.clients.webhook;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.config.EnvConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

// embedded WireMock server acting as a local inbound-webhook receiver, binds to an OS-assigned
// port by default so it's parallel-safe
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

    // stands in for the external caller hitting this receiver's webhook endpoint
    public Response sendCallerRequest(String path, String jsonBody) {
        return RestAssured.given()
                .baseUri(baseUrl())
                .contentType(ContentType.JSON)
                .body(jsonBody)
                .when()
                .post(path);
    }

    public void stubIncomingCallResponse(String path, int status, String responseJsonBody) {
        server.stubFor(WireMock.post(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse().withStatus(status).withBody(responseJsonBody)));
    }

    // server is shared for the whole test class, so call this first if a test needs
    // to see only its own traffic
    public void reset() {
        server.resetRequests();
    }

    @Override
    public void close() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
