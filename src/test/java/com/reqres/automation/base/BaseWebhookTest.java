package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Starts a thread-local embedded WireMock instance (an OS-assigned port)
 * once per test class, exposing its base URL so a test can simulate an
 * external caller hitting the "webhook" and assert on what WireMock
 * recorded.
 */
public abstract class BaseWebhookTest {

    private static final ThreadLocal<WebhookReceiver> RECEIVER = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    public void startWebhookReceiver() {
        WebhookReceiver receiver =
                (WebhookReceiver) ClientFactory.create(ClientFactory.Protocol.WEBHOOK, ConfigLoader.load());
        RECEIVER.set(receiver);
    }

    @AfterClass(alwaysRun = true)
    public void stopWebhookReceiver() {
        WebhookReceiver receiver = RECEIVER.get();
        if (receiver != null) {
            receiver.close();
        }
        RECEIVER.remove();
    }

    protected WebhookReceiver receiver() {
        return RECEIVER.get();
    }
}
