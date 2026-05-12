package com.football.client.json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.football.client.Client;
import com.football.client.inputs.InputDevice;
import com.football.client.inputs.bot.InputBot;
import com.football.util.math.geometry.Translation2d;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class InputPacket implements DataPacket {
    public static final DevicePacket EMPTY_DEVICE = new InputPacket.DevicePacket("", new HashSet<>(), new double[0], new Translation2d());

    public List<DevicePacket> devices;

    @ToString
    @AllArgsConstructor
    public static class DevicePacket {
        public String type; // "keyboard" or "controller"
        public Set<String> buttons;
        public double[] axes;
        public Translation2d click;

        public DevicePacket() {}
    }

    @Override
    public void handle(Client client) {
        client.updateInput(this);
    }
}
