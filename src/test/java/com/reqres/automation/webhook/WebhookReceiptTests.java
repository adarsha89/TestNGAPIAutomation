package com.reqres.automation.webhook;

import com.reqres.automation.base.BaseWebhookInterface;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.dataproviders.WebhookReceiptNegativeDataProvider;
import com.reqres.automation.services.WebhookService;
import com.reqres.automation.testdata.ResponseExpectation;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Sample webhook coverage: the test itself plays the role of the external
 * caller, POSTs to the embedded {@link WebhookReceiver}'s URL, then asserts
 * WireMock recorded the expected request - the pattern a real
 * inbound-webhook test would follow.
 */
@Story("Inbound webhook receipt")
public class WebhookReceiptTests implements BaseWebhookInterface {

    private static final String WEBHOOK_PATH = "/webhook/order-created";
    private static final String PAYLOAD = "{\"event\":\"order.created\",\"orderId\":\"ORD-1001\"}";

    @Test(groups = {"webhook", "smoke", "regression"})
    @Description("Simulate an inbound webhook call and verify it was recorded")
    public void shouldRecordIncomingWebhookCall() {
        WebhookService service = webhookService();

        service.stubIncomingCallResponse(WEBHOOK_PATH, 200, "{\"received\":true}");

        service.sendCallerRequestAndVerify(WEBHOOK_PATH, PAYLOAD, ResponseExpectation.status(200));

        service.verifyCallReceived(WEBHOOK_PATH, PAYLOAD);
    }

    @Test(groups = {"webhook", "smoke", "regression"})
    @Description("A call to an unrelated path is not recorded, even though the receiver has other recorded "
            + "traffic")
    public void shouldNotRecordWebhookCallForUnrelatedPath() {
        WebhookService service = webhookService();

        service.stubIncomingCallResponse(WEBHOOK_PATH, 200, "{\"received\":true}");

        service.sendCallerRequestAndVerify(WEBHOOK_PATH, PAYLOAD, ResponseExpectation.status(200));

        service.verifyNoCallReceived("/webhook/order-cancelled");
    }

    @Test(groups = {"webhook", "negative", "regression"},
            dataProvider = "mismatchedPayloads", dataProviderClass = WebhookReceiptNegativeDataProvider.class)
    @Description("A call sent to the expected path but with a mismatched payload is recorded at the path, "
            + "but not as a match for the expected payload")
    public void shouldNotTreatPayloadMismatchAsExpectedReceipt(String caseName, String mismatchedPayload) {
        WebhookService service = webhookService();
        service.reset();

        service.stubIncomingCallResponse(WEBHOOK_PATH, 200, "{\"received\":true}");

        service.sendCallerRequestAndVerify(WEBHOOK_PATH, mismatchedPayload, ResponseExpectation.status(200));

        service.verifyCallReceivedWithDifferentPayload(WEBHOOK_PATH, PAYLOAD);
    }

    @Test(groups = {"webhook", "negative", "regression"},
            dataProvider = "mismatchedPaths", dataProviderClass = WebhookReceiptNegativeDataProvider.class)
    @Description("A call sent to a path other than the expected path is recorded at that mismatched path, "
            + "but does not count toward the expected path's matched-call record")
    public void shouldNotTreatCallToDifferentPathAsReceivedForExpectedPath(String caseName, String mismatchedPath) {
        WebhookService service = webhookService();
        service.reset();

        service.stubIncomingCallResponse(WEBHOOK_PATH, 200, "{\"received\":true}");
        service.sendCallerRequestAndVerify(WEBHOOK_PATH, PAYLOAD, ResponseExpectation.status(200));

        service.sendCallerRequest(mismatchedPath, PAYLOAD);

        service.verifyCallReceived(mismatchedPath, PAYLOAD);
        service.verifyCallReceivedExactly(WEBHOOK_PATH, PAYLOAD, 1);
    }
}
