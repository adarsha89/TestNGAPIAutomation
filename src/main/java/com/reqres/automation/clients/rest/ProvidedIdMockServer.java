package com.reqres.automation.clients.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.reqres.automation.utils.Constants;

/**
 * Local, entirely stateful REST test double - "the provided-ID mock API" - backed by an embedded
 * WireMock server on an OS-assigned port, same shape as {@link RestStubServer}. Unlike
 * {@code RestStubServer}'s fixed canned responses, every request to {@link Constants#PROVIDED_ID_PATH}
 * is delegated to a {@link ProvidedIdResponseTransformer} backed by {@link ProvidedIdStore#instance()},
 * since the response here must depend on durable, cross-request state (has this ID been seen
 * before?).
 */
public class ProvidedIdMockServer {

    private WireMockServer server;

    public void start() {
        this.server = new WireMockServer(WireMockConfiguration.options()
                .port(0)
                .extensions(new ProvidedIdResponseTransformer(ProvidedIdStore.instance())));
        this.server.start();
        this.server.stubFor(WireMock.post(WireMock.urlEqualTo(Constants.PROVIDED_ID_PATH))
                .willReturn(WireMock.aResponse().withTransformers(ProvidedIdResponseTransformer.NAME)));
    }

    /** The full URL of {@code path} on this mock server's base URL. */
    public String stubUrl(String path) {
        return server.baseUrl() + path;
    }

    /** Exposes the underlying WireMock server so a test can {@code verify(...)} what was received. */
    public WireMockServer getServer() {
        return server;
    }

    public void stop() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
