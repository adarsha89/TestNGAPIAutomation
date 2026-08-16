package com.reqres.automation.assertions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.clients.websocket.WebSocketTestClient;
import com.reqres.automation.models.websocket.WsMessage;
import org.testng.Assert;

/**
 * WebSocket-shape-aware assertion helpers shared across WebSocket test
 * classes: message-received/not-received, payload-equality (text/binary),
 * open, closed, closed-with-code, and JSON-envelope-equality checks.
 */
public final class WebSocketAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WebSocketAssertions() {
    }

    public static String assertMessageReceived(WebSocketTestClient client, long timeoutSeconds) throws InterruptedException {
        String message = client.awaitMessage(timeoutSeconds);
        Assert.assertNotNull(message, "Did not receive a message within " + timeoutSeconds + "s timeout");
        return message;
    }

    public static String assertMessageReceived(WebSocketTestClient client, long timeoutSeconds, String context) throws InterruptedException {
        String message = client.awaitMessage(timeoutSeconds);
        Assert.assertNotNull(message, "[" + context + "] Did not receive a message within " + timeoutSeconds + "s timeout");
        return message;
    }

    public static byte[] assertBinaryMessageReceived(WebSocketTestClient client, long timeoutSeconds) throws InterruptedException {
        byte[] message = client.awaitBinaryMessage(timeoutSeconds);
        Assert.assertNotNull(message, "Did not receive a binary message within " + timeoutSeconds + "s timeout");
        return message;
    }

    public static void assertNoMessageReceived(WebSocketTestClient client, long timeoutSeconds) throws InterruptedException {
        String message = client.awaitMessage(timeoutSeconds);
        Assert.assertNull(message, "Expected no message within " + timeoutSeconds + "s timeout, but received: " + message);
    }

    public static void assertPayloadEquals(String actual, String expected) {
        Assert.assertEquals(actual, expected,
                "Payload did not match. Expected: '" + expected + "', actual: '" + actual + "'");
    }

    public static void assertPayloadEquals(String actual, String expected, String context) {
        Assert.assertEquals(actual, expected,
                "[" + context + "] Payload did not match. Expected: '" + expected + "', actual: '" + actual + "'");
    }

    public static void assertPayloadEquals(byte[] actual, byte[] expected) {
        Assert.assertEquals(actual, expected, "Binary payload did not match the expected binary payload");
    }

    public static void assertOpen(WebSocketTestClient client) {
        Assert.assertTrue(client.isOpen(), "Expected the connection to be open");
    }

    public static void assertClosed(WebSocketTestClient client, long timeoutSeconds) throws InterruptedException {
        boolean closed = client.awaitClosed(timeoutSeconds);
        Assert.assertTrue(closed, "Expected the connection to reach a closed state within " + timeoutSeconds + "s timeout");
    }

    public static void assertClosedWithCode(WebSocketTestClient client, long timeoutSeconds, int expectedCloseCode) throws InterruptedException {
        assertClosed(client, timeoutSeconds);
        Assert.assertEquals(client.getLastCloseCode(), expectedCloseCode,
                "Expected close code " + expectedCloseCode + " but got " + client.getLastCloseCode());
    }

    /**
     * Parses {@code rawMessage} as a {@link WsMessage} and asserts its
     * {@code type}/{@code payload} match {@code expected}, failing with a
     * descriptive message (rather than throwing a checked exception) if
     * {@code rawMessage} is not valid JSON.
     */
    public static void assertJsonMessageEquals(String rawMessage, WsMessage expected) {
        WsMessage actual;
        try {
            actual = MAPPER.readValue(rawMessage, WsMessage.class);
        } catch (Exception e) {
            Assert.fail("Expected a JSON-encoded WsMessage, but failed to parse: " + rawMessage, e);
            return;
        }
        Assert.assertEquals(actual.getType(), expected.getType());
        Assert.assertEquals(actual.getPayload(), expected.getPayload());
    }

    /** Asserts {@code message} is non-null and non-blank, described by {@code description} on failure. */
    public static void assertNonBlankTextMessage(String message, String description) {
        Assert.assertNotNull(message, "Expected " + description + ", but got null");
        Assert.assertFalse(message.isBlank(), "Expected " + description + " to be non-blank");
    }
}
