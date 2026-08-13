package com.reqres.automation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reqres.automation.base.BaseWebSocketTest;
import com.reqres.automation.models.websocket.WsMessage;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Sample WebSocket coverage against the local embedded echo server started
 * by {@link BaseWebSocketTest}.
 */
@Story("Embedded WebSocket echo")
public class EchoWebSocketTests extends BaseWebSocketTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test(groups = {"websocket", "smoke", "regression"})
    @Description("Send a message to the embedded echo server and assert it is echoed back unchanged")
    public void shouldEchoSentMessage() throws Exception {
        WsMessage outbound = new WsMessage("greeting", "hello-from-automation");

        client().send(MAPPER.writeValueAsString(outbound));
        String rawReply = client().awaitMessage(5);

        Assert.assertNotNull(rawReply, "Did not receive an echo reply within the timeout");
        WsMessage inbound = MAPPER.readValue(rawReply, WsMessage.class);

        Assert.assertEquals(inbound.getType(), outbound.getType());
        Assert.assertEquals(inbound.getPayload(), outbound.getPayload());
    }
}
