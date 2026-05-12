package com.football.client.messages;

import lombok.AllArgsConstructor;

public class CountdownMessage extends Message {
    public CountdownMessage(String message, int from, int to) {
        super("countdown", new Json(message, from, to));
    }

    @AllArgsConstructor
    @SuppressWarnings("unused")
    private static class Json {
        public String message;
        public int from;
        public int to;
    }
}
