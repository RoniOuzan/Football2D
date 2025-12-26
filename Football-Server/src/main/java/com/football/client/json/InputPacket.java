package com.football.client.json;

import com.football.client.Client;
import com.football.util.math.geometry.Translation2d;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;
import java.util.Map;

@ToString
@AllArgsConstructor
public class InputPacket implements DataPacket {
    public List<DevicePacket> devices;

    @ToString
    public static class DevicePacket {
        public String type; // "keyboard" or "controller"
        public Set<String> buttons;
        public double[] axes;
        public Translation2d click;
    }

    @Override
    public void handle(Client client) {
        client.updateInput(this);
    }
}
