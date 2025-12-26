package com.football.client.inputs.devices;

import com.football.client.json.InputPacket;
import com.football.client.inputs.InputDevice;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Player;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.Comparator;
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

    public Translation2d leftStick = new Translation2d();
    public Translation2d rightStick = new Translation2d();
    private double lastRightStickNorm = 0;
    public double LT = 0;
    public double RT = 0;

    public InputControllerDevice(InputPacket.DevicePacket devicePacket) {
        super(keybinds, devicePacket);
    }

    @Override
    public Translation2d getRequestedVelocity() {
        return this.leftStick.getNorm() < 0.05 ? new Translation2d() : this.leftStick.normalized();
    }

    @Override
    public Player getPlayerToSwitchTo(TeamStrategy teamStrategy) {
        Player defaultPlayer = super.getPlayerToSwitchTo(teamStrategy);
        if (defaultPlayer != null) return defaultPlayer;

        if (!(this.rightStick != null && this.rightStick.getNorm() >= 0.5 && this.lastRightStickNorm < 0.5)) return null;

        Player chosen = teamStrategy.getChosenPlayer();

        return teamStrategy.getTeam().getPlayers().stream()
                .filter(p -> !p.equals(chosen))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(chosen.getPosition());

                    double angle = Math.abs(delta.getAngle()
                            .minus(teamStrategy.getCameraManager().getOrientedTranslation(this.rightStick).getAngle())
                            .getRadians());
                    double distance = delta.getNorm();

                    return angle + distance / 50;
                })).orElse(null);
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        super.updateInput(devicePacket);

        if (this.rightStick != null) // IDK why it can be null
            this.lastRightStickNorm = this.rightStick.getNorm();

        this.leftStick = new Translation2d(devicePacket.axes[0], -devicePacket.axes[1]);
        this.rightStick = new Translation2d(devicePacket.axes[2], -devicePacket.axes[3]);
        this.LT = devicePacket.axes[4];
        this.RT = devicePacket.axes[5];
    }
}
