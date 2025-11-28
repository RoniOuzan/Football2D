package com.football.client.inputs.devices;

import com.football.client.inputs.InputPacket;
import com.football.client.inputs.InputDevice;
import com.football.client.keybinds.Keybind;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.Map;

@ToString
public class InputKeyboardDevice extends InputDevice {
    private static final Map<Keybind, String> keybinds = Map.of(
            Keybind.SWITCH_PLAYER, "q",
            Keybind.SPRINT, "shift",
            Keybind.PASS, "e",
            Keybind.THROUGH, "f",
            Keybind.SHOOT, "r"
    );

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
