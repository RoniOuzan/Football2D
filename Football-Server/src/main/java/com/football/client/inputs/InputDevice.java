package com.football.client.inputs;

import com.football.client.json.InputPacket;
import com.football.client.keybinds.Keybind;
import com.football.game.strategy.TeamStrategy;
import com.football.game.players.Player;
import lombok.ToString;

import java.util.*;

@ToString
public abstract class InputDevice implements InputHandler {

    private final Map<Keybind, String> keybinds;

    protected Set<String> buttons = new HashSet<>();
    private Set<String> lastButtons = new HashSet<>();

    private final Map<Keybind, Set<Keybind>> kickTypes = new HashMap<>();

    private final Map<String, Long> pressTimestamps = new HashMap<>();
    private final Map<String, Double> holdDurations = new HashMap<>();

    protected InputDevice(Map<Keybind, String> keybinds, InputPacket.DevicePacket devicePacket) {
        this.keybinds = keybinds;
        this.updateInput(devicePacket);
    }

    private String getKey(Keybind keybind) {
        return this.keybinds.get(keybind);
    }

    @Override
    public boolean isPressed(Keybind keybind) {
        return this.buttons.contains(getKey(keybind)) && !this.lastButtons.contains(getKey(keybind));
    }

    @Override
    public boolean isHolding(Keybind keybind) {
        return this.buttons.contains(getKey(keybind));
    }

    @Override
    public boolean isReleased(Keybind keybind) {
        return !this.buttons.contains(getKey(keybind)) && this.lastButtons.contains(getKey(keybind));
    }

    public double getLastHoldTime(Keybind keybind) {
        return this.holdDurations.getOrDefault(getKey(keybind), 0.0);
    }

    @Override
    public void updateInput(InputPacket.DevicePacket devicePacket) {
        this.lastButtons = this.buttons;
        this.buttons = devicePacket.buttons;

        // Check which buttons were just pressed
        long now = System.currentTimeMillis();
        for (String button : this.buttons) {
            if (!this.lastButtons.contains(button)) {
                // Button pressed now
                this.pressTimestamps.put(button, now);
            }
        }

        // Check which buttons were just released
        for (String oldButton : this.lastButtons) {
            if (!this.buttons.contains(oldButton)) {
                Long pressedAt = this.pressTimestamps.remove(oldButton);
                if (pressedAt != null) {
                    double heldTime = (now - pressedAt) / 1000.0;
                    this.holdDurations.put(oldButton, heldTime);
                }
            }
        }
    }

    @Override
    public void runInputs(TeamStrategy teamStrategy, Player player) {
        for (Keybind keybind : this.keybinds.keySet()) {
            if (this.isPressed(keybind)) {
                keybind.justPressed(teamStrategy, player);
            }

            if (this.isHolding(keybind)) {
                Arrays.stream(Keybind.values()).filter(Keybind::isKickType).forEach(k -> {
                    if (this.isHolding(k)) {
                        if (!this.kickTypes.containsKey(keybind))
                            this.kickTypes.put(keybind, new HashSet<>());

                        this.kickTypes.get(keybind).add(k);
                    }
                });

                keybind.holding(teamStrategy, player);
            }

            if (this.isReleased(keybind)) {
                keybind.justReleased(teamStrategy, player, this.kickTypes.getOrDefault(keybind, new HashSet<>()), getLastHoldTime(keybind));

                if (this.kickTypes.containsKey(keybind))
                    this.kickTypes.get(keybind).clear();
            }
        }
    }
}
