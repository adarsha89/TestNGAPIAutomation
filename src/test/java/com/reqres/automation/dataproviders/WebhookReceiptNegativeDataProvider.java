package com.reqres.automation.dataproviders;

import org.testng.annotations.DataProvider;

// negative-path rows for WebhookReceiptTests; static since TestNG pulls these via dataProviderClass
public final class WebhookReceiptNegativeDataProvider {

    private WebhookReceiptNegativeDataProvider() {
    }

    @DataProvider(name = "mismatchedPayloads")
    public static Object[][] mismatchedPayloads() {
        return new Object[][]{
                {"different orderId", "{\"event\":\"order.created\",\"orderId\":\"ORD-9999\"}"},
                {"different event type", "{\"event\":\"order.cancelled\",\"orderId\":\"ORD-1001\"}"},
                {"extra unexpected field",
                        "{\"event\":\"order.created\",\"orderId\":\"ORD-1001\",\"unexpected\":\"field\"}"},
        };
    }

    @DataProvider(name = "mismatchedPaths")
    public static Object[][] mismatchedPaths() {
        return new Object[][]{
                {"unrelated resource path", "/webhook/user-updated"},
                {"path with trailing extra segment", "/webhook/order-created/extra"},
                {"case-different path", "/webhook/Order-Created"},
        };
    }
}
