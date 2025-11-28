package com.football.client.inputs;

import com.football.client.keybinds.Keybind;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;
import java.util.List;

public class AllInputDevices implements InputHandler {

    private final List<InputDevice> devices;

    public AllInputDevices(List<InputDevice> devices) {
        this.devices = devices;
    }

    @Override
    public Translation2d getRequestedVelocity() {
        return this.devices.stream()
                .map(InputHandler::getRequestedVelocity)
                .max(Comparator.comparingDouble(Translation2d::getNorm))
                .orElse(new Translation2d());
    }

    @Override
    public boolean isPressed(Keybind keybind) {
        return this.devices.stream().anyMatch(i -> i.isPressed(keybind));
    }

    @Override
    public boolean isHolding(Keybind keybind) {
        return this.devices.stream().anyMatch(i -> i.isHolding(keybind));
    }

    @Override
    public boolean isReleased(Keybind keybind) {
        return this.devices.stream().anyMatch(i -> i.isReleased(keybind));
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {}

    @Override
    public void runInputs(TeamStrategy teamStrategy, Player player) {
        for (InputDevice device : this.devices) {
            device.runInputs(teamStrategy, player);
        }
    }
}
