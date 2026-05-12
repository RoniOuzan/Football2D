package com.football.client.messages;

import com.football.util.json.JsonUtil;

import lombok.ToString;

@ToString
public class Message {
    protected final String type;
    protected final Object data;

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

    public String getJson() {
        return JsonUtil.toJson(this);
    }
}
