package com.football.client.inputs;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;
import java.util.Map;

@ToString
public class InputPacket {
    public String type;
    public List<DevicePacket> devices;

    @ToString
    public static class DevicePacket {
        public String type; // "keyboard" or "controller"
        public Set<String> buttons;
        public Axes axes;

        @AllArgsConstructor
        @ToString
        public static class Axes {
            public double leftX, leftY;
            public double rightX, rightY;
            public double LT, RT;
        }
    }
}
