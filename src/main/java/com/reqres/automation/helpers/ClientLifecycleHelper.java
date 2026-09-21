package com.reqres.automation.helpers;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.config.ConfigLoader;

/**
 * The single place that builds a fully-initialized protocol client via
 * {@link ClientFactory}/{@link ConfigLoader}. Base classes call here for
 * construction; services and base classes don't call
 * {@link ClientFactory}/{@link ConfigLoader} directly.
 */
public final class ClientLifecycleHelper {

    private ClientLifecycleHelper() {
    }

    public static UserRestClient createRestClient() {
        return (UserRestClient) ClientFactory.create(ClientFactory.Protocol.REST, ConfigLoader.load());
    }

    public static WebhookReceiver createWebhookReceiver() {
        return (WebhookReceiver) ClientFactory.create(ClientFactory.Protocol.WEBHOOK, ConfigLoader.load());
    }
}
