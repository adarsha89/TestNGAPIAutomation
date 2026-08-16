package com.reqres.automation.assertions;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.clients.webhook.WebhookReceiver;

/**
 * Webhook-shape-aware assertion helpers shared across webhook test classes:
 * call-received (semantic JSON payload equality) and call-not-received
 * (path-only scoping) checks, wrapping WireMock's own {@code verify(...)}.
 */
public final class WebhookAssertions {

    private WebhookAssertions() {
    }

    public static void assertCallReceived(WebhookReceiver receiver, String path, String expectedJsonPayload) {
        receiver.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(path))
                .withRequestBody(WireMock.equalToJson(expectedJsonPayload)));
    }

    public static void assertNoCallReceived(WebhookReceiver receiver, String path) {
        receiver.getServer().verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(path)));
    }
}
