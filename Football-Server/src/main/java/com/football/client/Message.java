package com.football.client;

public class Message {
    private final String type;
    private final Object data;

    public Message(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return this.type;
    }

    public Object getData() {
        return this.data;
    }
}
