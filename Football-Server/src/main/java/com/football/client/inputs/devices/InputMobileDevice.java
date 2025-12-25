package com.football.client.inputs.devices;

import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class InputMobileDevice extends InputDevice {
    private static final Map<Keybind, String> keybinds = new HashMap<>();
    static {
//        keybinds.put(Keybind.SWITCH_PLAYER, "LB" );
//        keybinds.put(Keybind.SWITCH_CAMERA, "RS");
        keybinds.put(Keybind.SPRINT, "");
        keybinds.put(Keybind.PASS, "pass");
        keybinds.put(Keybind.CROSS, "cross");
        keybinds.put(Keybind.THROUGH, "through");
        keybinds.put(Keybind.SHOOT, "shoot");

//        keybinds.put(Keybind.CHIP, "LB");
//        keybinds.put(Keybind.FINESSE, "RB");
//        keybinds.put(Keybind.TRIVELA, "LT");
//        keybinds.put(Keybind.DRIVEN, "RT");
    }

    public double leftX = 0;
    public double leftY = 0;

    public InputMobileDevice(InputPacket.DevicePacket devicePacket) {
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
        this.leftY = devicePacket.axes.leftY;
    }
}
