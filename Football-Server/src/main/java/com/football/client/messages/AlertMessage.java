package com.football.client.messages;

import lombok.AllArgsConstructor;

public class AlertMessage extends Message {
    public AlertMessage(Object title, Object subTitle, String color, double time, String size) {
        super("alert", new Json(title.toString(), subTitle.toString(), color, time, size));
    }

    public AlertMessage(Object title, String color, double time, String size) {
        super("alert", new Json(title.toString(), "", color, time, size));
    }

    @AllArgsConstructor
    private static class Json {
        public String title;
        public String subTitle;
        public String color;
        public double time;
        public String size;
    }
}
