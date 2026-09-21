package com.reqres.automation.clients;

import com.reqres.automation.clients.rest.UserRestClient;
import com.reqres.automation.clients.webhook.WebhookReceiver;
import com.reqres.automation.config.EnvConfig;

/**
 * Factory/Strategy: maps a protocol to a fully-initialized {@link ApiClient}
 * instance. Adding a new protocol (e.g. gRPC) later means adding a
 * {@code clients/grpc/} package and one more case here - nothing else in
 * the framework changes.
 */
public final class ClientFactory {

    public enum Protocol {
        REST,
        WEBHOOK
    }

    private ClientFactory() {
    }

    public static ApiClient create(Protocol protocol, EnvConfig config) {
        ApiClient client = newInstance(protocol);
        client.init(config);
        return client;
    }

    private static ApiClient newInstance(Protocol protocol) {
        switch (protocol) {
            case REST:
                return new UserRestClient();
            case WEBHOOK:
                return new WebhookReceiver();
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + protocol);
        }
    }
}
