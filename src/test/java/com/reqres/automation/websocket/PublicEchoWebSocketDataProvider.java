package com.reqres.automation.websocket;

import org.testng.annotations.DataProvider;

/**
 * {@link DataProvider} methods feeding {@link PublicEchoWebSocketTests}. Kept
 * separate from the test class so provider data stays independent of test
 * logic. TestNG requires these to be {@code static} since they're referenced
 * via {@code dataProviderClass} rather than living on the test class itself.
 */
public final class PublicEchoWebSocketDataProvider {

    private PublicEchoWebSocketDataProvider() {
    }

    @DataProvider(name = "textPayloads")
    public static Object[][] textPayloads() {
        return new Object[][]{
                {"empty", ""},
                {"short", "hello-from-automation"},
                {"large", "x".repeat(50_000)},
        };
    }
}
