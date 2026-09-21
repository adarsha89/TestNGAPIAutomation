package com.reqres.automation.clients.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Local REST stub target backed by an embedded WireMock server on an
 * OS-assigned port. Used by tests that need an on-demand status (e.g. 503)
 * or independent rate-limit pacing that reqres.in can't produce on request.
 */
public class RestStubServer {

    private WireMockServer server;

    public void start() {
        this.server = new WireMockServer(WireMockConfiguration.options().port(0));
        this.server.start();
    }

    /** The full URL of {@code path} on this stub server's base URL. */
    public String stubUrl(String path) {
        return server.baseUrl() + path;
    }

    /** Exposes the underlying WireMock server so a test can {@code verify(...)} what was received. */
    public WireMockServer getServer() {
        return server;
    }

    /** Configures this stub to respond to GETs at {@code path} with a fixed status/body. */
    public void stubGetResponse(String path, int status, String jsonResponseBody) {
        server.stubFor(WireMock.get(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponseBody)));
    }

    /** Configures this stub to respond to POSTs at {@code path} with a fixed status/body. */
    public void stubPostResponse(String path, int status, String jsonResponseBody) {
        server.stubFor(WireMock.post(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponseBody)));
    }

    public void reset() {
        server.resetAll();
    }

    public void stop() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
