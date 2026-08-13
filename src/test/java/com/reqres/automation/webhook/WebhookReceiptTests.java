package com.reqres.automation.webhook;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reqres.automation.assertions.ResponseAssertions;
import com.reqres.automation.base.BaseWebhookTest;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Sample webhook coverage: the test itself plays the role of the external
 * caller, POSTs to the embedded {@link WebhookReceiver}'s URL, then asserts
 * WireMock recorded the expected request - the pattern a real
 * inbound-webhook test would follow.
 */
@Story("Inbound webhook receipt")
public class WebhookReceiptTests extends BaseWebhookTest {

    private static final String WEBHOOK_PATH = "/webhook/order-created";
    private static final String PAYLOAD = "{\"event\":\"order.created\",\"orderId\":\"ORD-1001\"}";

    @Test(groups = {"webhook", "smoke", "regression"})
    @Description("Simulate an inbound webhook call and verify it was recorded as expected")
    public void shouldRecordIncomingWebhookCall() {
        WebhookReceiver receiver = receiver();

        receiver.getServer().stubFor(WireMock.post(WireMock.urlEqualTo(WEBHOOK_PATH))
                .willReturn(WireMock.aResponse().withStatus(200).withBody("{\"received\":true}")));

        Response response = receiver.sendCallerRequest(WEBHOOK_PATH, PAYLOAD);

        ResponseAssertions.assertStatusCode(response, 200);

        receiver.getServer().verify(WireMock.postRequestedFor(WireMock.urlEqualTo(WEBHOOK_PATH))
                .withRequestBody(WireMock.equalToJson(PAYLOAD)));
    }
}
