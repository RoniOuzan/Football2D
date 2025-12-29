package com.football.client.messages;

import lombok.AllArgsConstructor;

public class AlertMessage extends Message {
    public AlertMessage(String message) {
        super("alert", new Json(message));
    }

    @AllArgsConstructor
    private static class Json {
        public String message;
    }
}
