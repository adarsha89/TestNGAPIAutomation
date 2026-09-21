package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.websocket.WebSocketTestClient;
import com.reqres.automation.config.ConfigLoader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

// connects to the real public echo endpoint (websocket.base.url) per test method, no embedded server
// like BaseWebSocketTest. The server sends an unsolicited greeting on connect, drained here so tests
// don't have to special-case it.
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

    protected String connectionGreeting() {
        return GREETING.get();
    }
}
