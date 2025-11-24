package com.football.client;

import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.Map;

@ToString
public class InputKeyboard extends ClientInput {
    private static final Map<Keybind, String> keybinds = Map.of(
            Keybind.SWITCH_PLAYER, "Q",
            Keybind.SPRINT, "shift",
            Keybind.PASS, "E",
            Keybind.THROUGH, "F",
            Keybind.SHOOT, "R"
    );

    public InputKeyboard(InputPacket.DevicePacket devicePacket) {
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
