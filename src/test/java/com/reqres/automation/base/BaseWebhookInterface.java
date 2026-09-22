package com.reqres.automation.base;

import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.helpers.ClientLifecycleHelper;
import com.reqres.automation.services.WebhookService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Starts a thread-local embedded WireMock instance (an OS-assigned port)
 * once per test class, exposing its base URL so a test can simulate an
 * external caller hitting the "webhook" and assert on what WireMock
 * recorded.
 */
public interface BaseWebhookInterface {

    ThreadLocal<WebhookService> SERVICE = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    default void startWebhookReceiver() {
        WebhookReceiver receiver = ClientLifecycleHelper.createWebhookReceiver();
        SERVICE.set(new WebhookService(receiver));
    }

    @AfterMethod(alwaysRun = true)
    default void stopWebhookReceiver() {
        WebhookService service = SERVICE.get();
        if (service != null) {
            service.close();
        }
        SERVICE.remove();
    }

    default WebhookService webhookService() {
        return SERVICE.get();
    }
}
