package com.reqres.automation.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.helpers.RestUserAssertionHelper;
import com.reqres.automation.helpers.WebhookAssertionHelper;
import com.reqres.automation.helpers.WebhookClientHelper;
import com.reqres.automation.testdata.ResponseExpectation;
import io.restassured.response.Response;

/**
 * Thin pass-through service wrapping {@link WebhookReceiver} 1:1,
 * constructed with an already-built receiver instance (built via
 * {@code ClientLifecycleHelper.createWebhookReceiver()} in the base class).
 * Still exposes {@link #getServer()} so {@code WebhookAssertions} - which
 * verifies against the raw WireMock server - keeps working unchanged.
 *
 * <p>{@link #sendCallerRequestAndVerify} and the {@code verifyXxx} wrappers
 * below are this service's only callers of the assertion helpers - test
 * classes call only these, never {@code WebhookAssertions}/
 * {@code ResponseAssertions} directly.</p>
 */
public class WebhookService {

    private final WebhookReceiver receiver;

    public WebhookService(WebhookReceiver receiver) {
        this.receiver = receiver;
    }

    public Response sendCallerRequest(String path, String jsonBody) {
        return WebhookClientHelper.sendCallerRequest(receiver, path, jsonBody);
    }

    public Response sendCallerRequestAndVerify(String path, String jsonBody, ResponseExpectation expectation) {
        return RestUserAssertionHelper.verify(sendCallerRequest(path, jsonBody), expectation);
    }

    public void verifyCallReceived(String path, String expectedJsonPayload) {
        WebhookAssertionHelper.assertCallReceived(this, path, expectedJsonPayload);
    }

    public void verifyNoCallReceived(String path) {
        WebhookAssertionHelper.assertNoCallReceived(this, path);
    }

    public void verifyCallReceivedWithDifferentPayload(String path, String expectedOriginalPayload) {
        WebhookAssertionHelper.assertCallReceivedWithDifferentPayload(this, path, expectedOriginalPayload);
    }

    public void verifyCallReceivedExactly(String path, String expectedJsonPayload, int expectedCount) {
        WebhookAssertionHelper.assertCallReceivedExactly(this, path, expectedJsonPayload, expectedCount);
    }

    public void stubIncomingCallResponse(String path, int status, String responseJsonBody) {
        WebhookClientHelper.stubIncomingCallResponse(receiver, path, status, responseJsonBody);
    }

    public void reset() {
        WebhookClientHelper.reset(receiver);
    }

    public String baseUrl() {
        return WebhookClientHelper.baseUrl(receiver);
    }

    public WireMockServer getServer() {
        return WebhookClientHelper.getServer(receiver);
    }

    public void close() {
        WebhookClientHelper.close(receiver);
    }
}
