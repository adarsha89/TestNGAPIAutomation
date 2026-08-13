package com.reqres.automation.models.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal envelope for messages exchanged over the sample WebSocket echo
 * connection.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsMessage {

    private String type;
    private String payload;

    public WsMessage() {
    }

    public WsMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
