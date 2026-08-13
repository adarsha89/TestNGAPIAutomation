package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.websocket.WebSocketTestClient;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Connects a fresh {@link WebSocketTestClient} to the real, configured
 * public WebSocket echo endpoint ({@code websocket.base.url}) once per test
 * method - unlike {@link BaseWebSocketTest}, no embedded local server is
 * started here. On connect, the target server
 * ({@code wss://echo.websocket.org}) immediately pushes one unsolicited
 * greeting message before any echo; that greeting is drained here into a
 * thread-local field so individual test methods never have to special-case
 * it.
 */
public abstract class BasePublicWebSocketTest {

    private static final ThreadLocal<WebSocketTestClient> CLIENT = new ThreadLocal<>();
    private static final ThreadLocal<String> GREETING = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void connectClient() throws Exception {
        WebSocketTestClient client =
                (WebSocketTestClient) ClientFactory.create(ClientFactory.Protocol.WEBSOCKET, ConfigLoader.load());
        client.connect();
        CLIENT.set(client);
        GREETING.set(client.awaitMessage(5));
    }

    @AfterMethod(alwaysRun = true)
    public void disconnectClient() {
        WebSocketTestClient client = CLIENT.get();
        if (client != null) {
            client.close();
        }
        CLIENT.remove();
        GREETING.remove();
    }

    protected WebSocketTestClient client() {
        return CLIENT.get();
    }

    /** The one-time unsolicited server greeting drained on connect. */
    protected String connectionGreeting() {
        return GREETING.get();
    }
}
