package com.reqres.automation.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import io.restassured.response.Response;

/**
 * Thin static delegation wrapper around {@link WebhookReceiver} - the one
 * legal seam between {@code WebhookService} and the client layer. Each
 * method forwards unchanged to the same-named receiver method.
 */
public final class WebhookClientHelper {

    private WebhookClientHelper() {
    }

    public static Response sendCallerRequest(WebhookReceiver receiver, String path, String jsonBody) {
        return receiver.sendCallerRequest(path, jsonBody);
    }

    public static void stubIncomingCallResponse(WebhookReceiver receiver, String path, int status,
            String responseJsonBody) {
        receiver.stubIncomingCallResponse(path, status, responseJsonBody);
    }

    public static void reset(WebhookReceiver receiver) {
        receiver.reset();
    }

    public static String baseUrl(WebhookReceiver receiver) {
        return receiver.baseUrl();
    }

    public static WireMockServer getServer(WebhookReceiver receiver) {
        return receiver.getServer();
    }

    public static void close(WebhookReceiver receiver) {
        receiver.close();
    }
}
