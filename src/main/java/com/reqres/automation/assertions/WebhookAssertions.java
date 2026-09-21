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

    // path got at least one call, but none matched expectedOriginalPayload - distinguishes a
    // payload mismatch from a path miss (see assertNoCallReceived)
    public static void assertCallReceivedWithDifferentPayload(WebhookReceiver receiver, String path,
            String expectedOriginalPayload) {
        receiver.getServer().verify(WireMock.moreThanOrExactly(1),
                WireMock.postRequestedFor(WireMock.urlEqualTo(path)));
        receiver.getServer().verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo(path))
                .withRequestBody(WireMock.equalToJson(expectedOriginalPayload)));
    }

    // exact-count check, stronger than assertCallReceived's implicit "at least one" - catches
    // traffic misattributed from another path (e.g. prefix or case-insensitive match bug)
    public static void assertCallReceivedExactly(WebhookReceiver receiver, String path,
            String expectedJsonPayload, int expectedCount) {
        receiver.getServer().verify(expectedCount, WireMock.postRequestedFor(WireMock.urlEqualTo(path))
                .withRequestBody(WireMock.equalToJson(expectedJsonPayload)));
    }
}
