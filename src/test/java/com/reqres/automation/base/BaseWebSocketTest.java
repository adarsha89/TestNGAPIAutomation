package com.reqres.automation.base;

import com.reqres.automation.clients.ClientFactory;
import com.reqres.automation.clients.websocket.WebSocketTestClient;
import com.reqres.automation.config.ConfigLoader;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// starts a local embedded echo server per test class (no dependency on a third-party echo service's
// uptime), and a thread-local client connection per method. Server is thread-local too, like the other
// base classes, so parallel="classes" runs don't race on each other's port.
public abstract class BaseWebSocketTest {

    private static final ThreadLocal<EchoServer> ECHO_SERVER = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ECHO_SERVER_PORT = new ThreadLocal<>();

    private static final ThreadLocal<WebSocketTestClient> CLIENT = new ThreadLocal<>();

    @BeforeClass(alwaysRun = true)
    public void startEchoServer() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        EchoServer echoServer = new EchoServer(new InetSocketAddress("localhost", 0), startLatch);
        echoServer.start();
        if (!startLatch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Embedded WebSocket echo server did not start in time");
        }
        ECHO_SERVER.set(echoServer);
        ECHO_SERVER_PORT.set(echoServer.getPort());
    }

    @AfterClass(alwaysRun = true)
    public void stopEchoServer() throws InterruptedException {
        EchoServer echoServer = ECHO_SERVER.get();
        if (echoServer != null) {
            echoServer.stop();
        }
        ECHO_SERVER.remove();
        ECHO_SERVER_PORT.remove();
    }

    @BeforeMethod(alwaysRun = true)
    public void connectClient() throws Exception {
        WebSocketTestClient client =
                (WebSocketTestClient) ClientFactory.create(ClientFactory.Protocol.WEBSOCKET, ConfigLoader.load());
        client.connect(URI.create("ws://localhost:" + ECHO_SERVER_PORT.get()));
        CLIENT.set(client);
    }

    @AfterMethod(alwaysRun = true)
    public void disconnectClient() {
        WebSocketTestClient client = CLIENT.get();
        if (client != null) {
            client.close();
        }
        CLIENT.remove();
    }

    protected WebSocketTestClient client() {
        return CLIENT.get();
    }

    private static final class EchoServer extends WebSocketServer {

        private final CountDownLatch startLatch;

        private EchoServer(InetSocketAddress address, CountDownLatch startLatch) {
            super(address);
            this.startLatch = startLatch;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            // no-op: connection accepted
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            // no-op
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            conn.send(message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            // surfaced to the test via a missing echo reply / timeout
        }

        @Override
        public void onStart() {
            startLatch.countDown();
        }
    }
}
