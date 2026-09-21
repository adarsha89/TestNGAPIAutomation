package com.reqres.automation.helpers;

import com.reqres.automation.assertions.WebhookAssertions;
import com.reqres.automation.services.WebhookService;

/**
 * Static delegation wrapper around {@link WebhookAssertions}, relocated out
 * of the Service Layer's {@code verifyXxx} methods. Still takes
 * {@link WebhookService} (not the raw {@code WebhookReceiver}) since
 * {@code WebhookAssertions} needs {@code getServer()}.
 */
public final class WebhookAssertionHelper {

    private WebhookAssertionHelper() {
    }

    public static void assertCallReceived(WebhookService receiver, String path, String expectedJsonPayload) {
        WebhookAssertions.assertCallReceived(receiver, path, expectedJsonPayload);
    }

    public static void assertNoCallReceived(WebhookService receiver, String path) {
        WebhookAssertions.assertNoCallReceived(receiver, path);
    }

    public static void assertCallReceivedWithDifferentPayload(WebhookService receiver, String path,
            String expectedOriginalPayload) {
        WebhookAssertions.assertCallReceivedWithDifferentPayload(receiver, path, expectedOriginalPayload);
    }

    public static void assertCallReceivedExactly(WebhookService receiver, String path, String expectedJsonPayload,
            int expectedCount) {
        WebhookAssertions.assertCallReceivedExactly(receiver, path, expectedJsonPayload, expectedCount);
    }
}
