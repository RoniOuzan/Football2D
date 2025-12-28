package com.football.client.inputs.devices;

import com.football.client.inputs.InputDevice;
import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@ToString
public class InputMobileDevice extends InputDevice {
    private static final Map<Keybind, String> keybinds = new HashMap<>();
    static {
//        keybinds.put(Keybind.SWITCH_PLAYER, "LB" );
//        keybinds.put(Keybind.SWITCH_CAMERA, "RS");
        keybinds.put(Keybind.SPRINT, "sprint");
        keybinds.put(Keybind.PASS, "pass");
//        keybinds.put(Keybind.CROSS, "cross");
        keybinds.put(Keybind.THROUGH, "through");
        keybinds.put(Keybind.SHOOT, "shoot");

//        keybinds.put(Keybind.CHIP, "LB");
//        keybinds.put(Keybind.FINESSE, "RB");
//        keybinds.put(Keybind.TRIVELA, "LT");
//        keybinds.put(Keybind.DRIVEN, "RT");
    }

    public Translation2d joystick = new Translation2d();
    public Translation2d lastClickLocation = new Translation2d();
    public Translation2d clickLocation = new Translation2d();

    public InputMobileDevice(InputPacket.DevicePacket devicePacket) {
        super(keybinds, devicePacket);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        return this.joystick.normalized().times(this.joystick.getNorm()); // times norm for squared
    }

    @Override
    public Player getPlayerToSwitchTo(TeamStrategy teamStrategy) {
        if (!(this.clickLocation != null && this.lastClickLocation == null)) return null;

        return teamStrategy.getTeam().getPlayers().stream()
                .filter(p -> !p.equals(teamStrategy.getChosenPlayer()))
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.clickLocation)))
                .orElse(null);
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        super.updateInput(devicePacket);

        this.joystick = new Translation2d(devicePacket.axes[0], -devicePacket.axes[1]);
        this.lastClickLocation = this.clickLocation;
        this.clickLocation = devicePacket.click;
    }
}
