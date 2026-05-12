package com.football.client.inputs.devices;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.geometry.Translation2d;

import lombok.ToString;

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

    public Translation2d lastClickLocation = new Translation2d();
    public Translation2d clickLocation = new Translation2d();

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

    @Override
    public Player getPlayerToSwitchTo(TeamStrategy teamStrategy) {
        Player defaultPlayer = super.getPlayerToSwitchTo(teamStrategy);
        if (defaultPlayer != null) return defaultPlayer;

        if (!(this.clickLocation != null && this.lastClickLocation == null)) return null;

        return teamStrategy.getTeam().getPlayers().stream()
                .filter(p -> !p.equals(teamStrategy.getChosenPlayer()))
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.clickLocation)))
                .orElse(null);
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        super.updateInput(devicePacket);

        this.lastClickLocation = this.clickLocation;
        this.clickLocation = devicePacket.click;
    }
}
