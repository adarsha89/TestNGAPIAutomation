package com.reqres.automation.websocket;

import com.reqres.automation.base.BasePublicWebSocketTest;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

/**
 * Coverage against the real, public {@code wss://echo.websocket.org} server
 * (config-driven via {@code websocket.base.url} - see
 * {@link BasePublicWebSocketTest}). On connect, the server pushes one
 * unsolicited greeting message; {@link BasePublicWebSocketTest} drains it
 * into {@link #connectionGreeting()} so these tests never have to
 * special-case it.
 */
@Story("Public WebSocket echo")
public class PublicEchoWebSocketTests extends BasePublicWebSocketTest {

    @Test(groups = {"websocket", "external", "regression"})
    @Description("Connection establishes and the unsolicited server greeting is drained as connection-lifecycle "
            + "noise, never compared against a sent payload")
    public void shouldDrainUnsolicitedGreetingOnConnect() {
        Assert.assertTrue(client().isOpen(), "Expected the connection to be open after connect");
        String greeting = connectionGreeting();
        Assert.assertNotNull(greeting, "Expected an unsolicited greeting message to have been drained on connect");
        Assert.assertFalse(greeting.isBlank(), "Expected the unsolicited greeting to be non-blank");
    }

    @DataProvider(name = "textPayloads")
    public Object[][] textPayloads() {
        return new Object[][]{
                {"empty", ""},
                {"short", "hello-from-automation"},
                {"large", "x".repeat(50_000)},
        };
    }

    @Test(dataProvider = "textPayloads", groups = {"websocket", "external", "regression"})
    @Description("A sent text payload is echoed back exactly, across representative sizes (empty/short/large)")
    public void shouldEchoTextPayloadAcrossSizes(String caseName, String payload) throws InterruptedException {
        client().send(payload);
        String echoed = client().awaitMessage(10);

        Assert.assertNotNull(echoed, "[" + caseName + "] Did not receive an echo reply within the timeout");
        Assert.assertEquals(echoed, payload,
                "[" + caseName + "] Echoed text did not match the sent text. Sent: '" + payload + "', received: '"
                        + echoed + "'");
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("A sent binary payload is echoed back byte-for-byte")
    public void shouldEchoBinaryPayload() throws InterruptedException {
        byte[] payload = "binary-echo".getBytes(StandardCharsets.UTF_8);

        client().sendBinary(payload);
        byte[] echoed = client().awaitBinaryMessage(10);

        Assert.assertNotNull(echoed, "Did not receive a binary echo reply within the timeout");
        Assert.assertEquals(echoed, payload, "Echoed binary payload did not match the sent binary payload");
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("Multiple sequential messages on one connection are echoed back in order, with no cross-talk")
    public void shouldEchoMultipleMessagesInOrder() throws InterruptedException {
        String first = "message-one";
        String second = "message-two";
        String third = "message-three";

        client().send(first);
        String echoedFirst = client().awaitMessage(10);
        client().send(second);
        String echoedSecond = client().awaitMessage(10);
        client().send(third);
        String echoedThird = client().awaitMessage(10);

        Assert.assertEquals(echoedFirst, first, "First echoed message did not match what was sent");
        Assert.assertEquals(echoedSecond, second, "Second echoed message did not match what was sent");
        Assert.assertEquals(echoedThird, third, "Third echoed message did not match what was sent");
    }

    @Test(groups = {"websocket", "external", "regression"}, timeOut = 20000)
    @Description("A clean client-initiated close is observable deterministically, without an arbitrary sleep")
    public void shouldCleanlyCloseAndObserveClosedState() throws InterruptedException {
        client().close();
        boolean closed = client().awaitClosed(15);

        Assert.assertTrue(closed, "Expected the connection to reach a closed state within the timeout");
        Assert.assertEquals(client().getLastCloseCode(), 1000, "Expected a normal closure close code (1000)");
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("When no message is sent within a reasonable window, awaitMessage returns null without hanging "
            + "and the connection remains open")
    public void shouldReturnNullOnIdleWindowWithoutHanging() throws InterruptedException {
        String message = client().awaitMessage(3);

        Assert.assertNull(message, "Expected no message to arrive during the idle window");
        Assert.assertTrue(client().isOpen(), "Expected the connection to remain open after the idle window");
    }

    @Test(groups = {"websocket", "external", "regression"}, timeOut = 20000)
    @Description("An abrupt client-side close (no prior graceful exchange) still reaches a closed state without "
            + "hanging")
    public void shouldReachClosedStateAfterAbruptClose() throws InterruptedException {
        client().close();
        boolean closed = client().awaitClosed(15);

        Assert.assertTrue(closed, "Expected the connection to reach a closed state within the timeout after an "
                + "abrupt close");
    }
}
