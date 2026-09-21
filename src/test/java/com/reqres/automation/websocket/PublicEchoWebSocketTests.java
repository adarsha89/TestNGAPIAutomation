package com.reqres.automation.websocket;

import com.reqres.automation.assertions.WebSocketAssertions;
import com.reqres.automation.base.BasePublicWebSocketTest;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

// coverage against the real public wss://echo.websocket.org server (websocket.base.url)
@Story("Public WebSocket echo")
public class PublicEchoWebSocketTests extends BasePublicWebSocketTest {

    @Test(groups = {"websocket", "external", "regression"})
    @Description("Connection establishes and the unsolicited server greeting is drained as connection-lifecycle "
            + "noise, never compared against a sent payload")
    public void shouldDrainUnsolicitedGreetingOnConnect() {
        WebSocketAssertions.assertOpen(client());
        String greeting = connectionGreeting();
        WebSocketAssertions.assertNonBlankTextMessage(greeting, "an unsolicited greeting message to have been drained on connect");
    }

    @Test(dataProvider = "textPayloads", dataProviderClass = PublicEchoWebSocketDataProvider.class,
            groups = {"websocket", "external", "regression"})
    @Description("A sent text payload is echoed back exactly, across representative sizes (empty/short/large)")
    public void shouldEchoTextPayloadAcrossSizes(String caseName, String payload) throws InterruptedException {
        client().send(payload);
        String echoed = WebSocketAssertions.assertMessageReceived(client(), 10, caseName);

        WebSocketAssertions.assertPayloadEquals(echoed, payload, caseName);
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("A sent binary payload is echoed back byte-for-byte")
    public void shouldEchoBinaryPayload() throws InterruptedException {
        byte[] payload = "binary-echo".getBytes(StandardCharsets.UTF_8);

        client().sendBinary(payload);
        byte[] echoed = WebSocketAssertions.assertBinaryMessageReceived(client(), 10);

        WebSocketAssertions.assertPayloadEquals(echoed, payload);
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("Multiple sequential messages on one connection are echoed back in order, with no cross-talk")
    public void shouldEchoMultipleMessagesInOrder() throws InterruptedException {
        String first = "message-one";
        String second = "message-two";
        String third = "message-three";

        client().send(first);
        String echoedFirst = WebSocketAssertions.assertMessageReceived(client(), 10, "first");
        client().send(second);
        String echoedSecond = WebSocketAssertions.assertMessageReceived(client(), 10, "second");
        client().send(third);
        String echoedThird = WebSocketAssertions.assertMessageReceived(client(), 10, "third");

        WebSocketAssertions.assertPayloadEquals(echoedFirst, first, "first");
        WebSocketAssertions.assertPayloadEquals(echoedSecond, second, "second");
        WebSocketAssertions.assertPayloadEquals(echoedThird, third, "third");
    }

    @Test(groups = {"websocket", "external", "regression"}, timeOut = 20000)
    @Description("A clean client-initiated close is observable deterministically, without an arbitrary sleep")
    public void shouldCleanlyCloseAndObserveClosedState() throws InterruptedException {
        client().close();

        WebSocketAssertions.assertClosedWithCode(client(), 15, 1000);
    }

    @Test(groups = {"websocket", "external", "regression"})
    @Description("When no message is sent within a reasonable window, awaitMessage returns null without hanging "
            + "and the connection remains open")
    public void shouldReturnNullOnIdleWindowWithoutHanging() throws InterruptedException {
        WebSocketAssertions.assertNoMessageReceived(client(), 3);
        WebSocketAssertions.assertOpen(client());
    }

    @Test(groups = {"websocket", "external", "regression"}, timeOut = 20000)
    @Description("An abrupt client-side close (no prior graceful exchange) still reaches a closed state without "
            + "hanging")
    public void shouldReachClosedStateAfterAbruptClose() throws InterruptedException {
        client().close();

        WebSocketAssertions.assertClosed(client(), 15);
    }
}
