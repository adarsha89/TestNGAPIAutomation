package com.reqres.automation.clients.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.clients.ApiClient;
import com.reqres.automation.config.EnvConfig;
import com.reqres.automation.models.websocket.WsMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thin wrapper around a Java-WebSocket client connection: connect, send,
 * and block-await the next inbound message.
 */
public class WebSocketTestClient implements ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketTestClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> receivedBinaryMessages = new LinkedBlockingQueue<>();
    private final AtomicInteger lastCloseCode = new AtomicInteger();
    private final AtomicReference<String> lastCloseReason = new AtomicReference<>();
    private final AtomicBoolean onCloseCallbackFired = new AtomicBoolean(false);
    private String defaultUrl;
    private WebSocketClient client;

    @Override
    public void init(EnvConfig config) {
        this.defaultUrl = config.getWebSocketBaseUrl();
    }

    /** Connects using the configured default WebSocket base URL. */
    public void connect() throws InterruptedException {
        connect(URI.create(defaultUrl));
    }

    /** Connects to an explicit URI - used to target a local embedded server. */
    public void connect(URI uri) throws InterruptedException {
        onCloseCallbackFired.set(false);
        this.client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                LOGGER.info("WebSocket connection opened: {}", uri);
            }

            @Override
            public void onMessage(String message) {
                receivedMessages.add(message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                byte[] copy = new byte[bytes.remaining()];
                bytes.get(copy);
                receivedBinaryMessages.add(copy);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                LOGGER.info("WebSocket connection closed: code={} reason={}", code, reason);
                lastCloseCode.set(code);
                lastCloseReason.set(reason);
                onCloseCallbackFired.set(true);
            }

            @Override
            public void onError(Exception ex) {
                LOGGER.warn("WebSocket error", ex);
            }
        };

        if ("wss".equalsIgnoreCase(uri.getScheme())) {
            try {
                client.setSocketFactory(SSLContext.getDefault().getSocketFactory());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Unable to initialize default SSL context for wss:// connection", e);
            }
        }

        boolean connected = client.connectBlocking(10, TimeUnit.SECONDS);
        if (!connected) {
            throw new IllegalStateException("Failed to connect to WebSocket server at " + uri);
        }
    }

    public void send(String message) {
        if (client == null) {
            throw new IllegalStateException("WebSocket client is not connected. Call connect() first.");
        }
        client.send(message);
    }

    /** Serializes {@code message} to JSON and sends it as a text frame. */
    public void sendMessage(WsMessage message) {
        try {
            send(MAPPER.writeValueAsString(message));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize WsMessage for sending: " + message, e);
        }
    }

    /** Sends a binary frame. */
    public void sendBinary(byte[] data) {
        if (client == null) {
            throw new IllegalStateException("WebSocket client is not connected. Call connect() first.");
        }
        client.send(data);
    }

    /** Blocks up to {@code timeoutSeconds} for the next received message, or returns null. */
    public String awaitMessage(long timeoutSeconds) throws InterruptedException {
        return receivedMessages.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    /** Blocks up to {@code timeoutSeconds} for the next received binary message, or returns null. */
    public byte[] awaitBinaryMessage(long timeoutSeconds) throws InterruptedException {
        return receivedBinaryMessages.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    /** True if a connection has been established and is currently open. */
    public boolean isOpen() {
        return client != null && client.isOpen();
    }

    /**
     * Polls at a short interval until the connection reports closed (both
     * {@link #isOpen()} is false and the {@code onClose} callback has fired,
     * so {@link #getLastCloseCode()}/{@link #getLastCloseReason()} are
     * guaranteed populated by the time this returns true) or
     * {@code timeoutSeconds} elapses. Bounded by design (no unbounded block)
     * since the underlying client library's {@code closeBlocking()} has no
     * timeout parameter in the pinned version.
     *
     * @return true if the connection reached a closed state within the timeout
     */
    public boolean awaitClosed(long timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            if (!isOpen() && onCloseCallbackFired.get()) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return !isOpen() && onCloseCallbackFired.get();
    }

    /** The close code from the most recent {@code onClose} callback, or 0 if not yet closed. */
    public int getLastCloseCode() {
        return lastCloseCode.get();
    }

    /** The close reason from the most recent {@code onClose} callback, or null if not yet closed. */
    public String getLastCloseReason() {
        return lastCloseReason.get();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
        receivedMessages.clear();
        receivedBinaryMessages.clear();
    }
}
