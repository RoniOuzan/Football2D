package com.football.client.inputs.devices;

import com.football.client.json.InputPacket;
import com.football.client.inputs.InputDevice;
import com.football.client.keybinds.Keybind;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class InputControllerDevice extends InputDevice {
    private static final Map<Keybind, String> keybinds = new HashMap<>();
    static {
        keybinds.put(Keybind.SWITCH_PLAYER, "LB");
        keybinds.put(Keybind.SWITCH_CAMERA, "RS");
        keybinds.put(Keybind.SPRINT, "RT");
        keybinds.put(Keybind.PASS, "A");
        keybinds.put(Keybind.CROSS, "X");
        keybinds.put(Keybind.THROUGH, "Y");
        keybinds.put(Keybind.SHOOT, "B");

        keybinds.put(Keybind.CHIP, "LB");
        keybinds.put(Keybind.FINESSE, "RB");
        keybinds.put(Keybind.TRIVELA, "LT");
        keybinds.put(Keybind.DRIVEN, "RT");
    }

    public double leftX = 0;
    public double leftY = 0;
    public double rightX = 0;
    public double rightY = 0;
    public double LT = 0;
    public double RT = 0;

    public InputControllerDevice(InputPacket.DevicePacket devicePacket) {
        super(keybinds, devicePacket);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        Translation2d joy = new Translation2d(this.leftX, this.leftY);
        return joy.getNorm() < 0.05 ? new Translation2d() : joy.normalized();
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        super.updateInput(devicePacket);

        this.leftX = devicePacket.axes.leftX;
        this.leftY = -devicePacket.axes.leftY;
        this.rightX = devicePacket.axes.rightX;
        this.rightY = devicePacket.axes.rightY;
        this.LT = devicePacket.axes.LT;
        this.RT = devicePacket.axes.RT;
    }
}
