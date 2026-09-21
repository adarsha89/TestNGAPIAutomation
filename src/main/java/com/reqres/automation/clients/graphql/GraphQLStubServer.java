package com.reqres.automation.clients.graphql;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

// embedded WireMock server simulating a local GraphQL endpoint, binds to an OS-assigned port.
// This is the target GraphQLClient posts to, not a client itself, so it skips ClientFactory -
// same role as WebhookReceiver.
public class GraphQLStubServer {

    private WireMockServer server;

    public void start() {
        this.server = new WireMockServer(WireMockConfiguration.options().port(0));
        this.server.start();
    }

    /** The full URL of {@code path} on this stub server's base URL. */
    public String graphQLUrl(String path) {
        return server.baseUrl() + path;
    }

    /** Configures this stub to respond to POSTs at {@code path} with a fixed 200 JSON body. */
    public void stubQueryResponse(String path, String jsonResponseBody) {
        server.stubFor(WireMock.post(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponseBody)));
    }

    public void stop() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
