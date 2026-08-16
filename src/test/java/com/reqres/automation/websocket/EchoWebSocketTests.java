package com.reqres.automation.websocket;

import com.reqres.automation.assertions.WebSocketAssertions;
import com.reqres.automation.base.BaseWebSocketTest;
import com.reqres.automation.models.websocket.WsMessage;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

/**
 * Sample WebSocket coverage against the local embedded echo server started
 * by {@link BaseWebSocketTest}.
 */
@Story("Embedded WebSocket echo")
public class EchoWebSocketTests extends BaseWebSocketTest {

    @Test(groups = {"websocket", "smoke", "regression"})
    @Description("Send a message to the embedded echo server and assert it is echoed back unchanged")
    public void shouldEchoSentMessage() throws Exception {
        WsMessage outbound = new WsMessage("greeting", "hello-from-automation");

        client().sendMessage(outbound);
        String rawReply = WebSocketAssertions.assertMessageReceived(client(), 5);

        WebSocketAssertions.assertJsonMessageEquals(rawReply, outbound);
    }
}
