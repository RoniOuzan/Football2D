package com.football.client.inputs.devices;

import com.football.client.json.InputPacket;
import com.football.client.inputs.InputDevice;
import com.football.client.keybinds.Keybind;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.security.Key;
import java.util.HashMap;
import java.util.Map;

@ToString
public class InputKeyboardDevice extends InputDevice {
    private static final Map<Keybind, String> keybinds = new HashMap<>();
    static {
        keybinds.put(Keybind.SWITCH_PLAYER, "q");
        keybinds.put(Keybind.SWITCH_CAMERA, " ");
        keybinds.put(Keybind.SPRINT, "shift");
        keybinds.put(Keybind.PASS, "e");
        keybinds.put(Keybind.CROSS, "t");
        keybinds.put(Keybind.THROUGH, "f");
        keybinds.put(Keybind.SHOOT, "r");

        keybinds.put(Keybind.CHIP, "x");
        keybinds.put(Keybind.FINESSE, "v");
        keybinds.put(Keybind.TRIVELA, "c");
        keybinds.put(Keybind.DRIVEN, "x");
    }

    public InputKeyboardDevice(InputPacket.DevicePacket devicePacket) {
        super(keybinds, devicePacket);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        double x = 0;
        double y = 0;

        if (this.buttons.contains("w")) y += 1;
        if (this.buttons.contains("s")) y -= 1;
        if (this.buttons.contains("d")) x += 1;
        if (this.buttons.contains("a")) x -= 1;

        return new Translation2d(x, y).normalized();
    }
}
