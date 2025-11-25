package com.football.client;

import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToString
public abstract class ClientInput implements Cloneable {

    private final Map<Keybind, String> keybinds;

    protected Set<String> buttons = new HashSet<>();
    private Set<String> lastButtons = new HashSet<>();

    private final Map<String, Long> pressTimestamps = new HashMap<>();
    private final Map<String, Double> holdDurations = new HashMap<>();

    protected ClientInput(Map<Keybind, String> keybinds, InputPacket.DevicePacket devicePacket) {
        this.keybinds = keybinds;
        this.updateInput(devicePacket);
    }

    public abstract Translation2d getRequestedVelocity();

    private String getKey(Keybind keybind) {
        return this.keybinds.get(keybind);
    }

    public boolean isHolding(Keybind keybind) {
        return this.buttons.contains(getKey(keybind));
    }

    public boolean isPressed(Keybind keybind) {
        return this.buttons.contains(getKey(keybind)) && !this.lastButtons.contains(getKey(keybind));
    }

    public boolean isReleased(Keybind keybind) {
        return !this.buttons.contains(getKey(keybind)) && this.lastButtons.contains(getKey(keybind));
    }

    public double getLastHoldTime(Keybind keybind) {
        return this.holdDurations.getOrDefault(getKey(keybind), 0.0);
    }

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
    public ClientInput clone() {
        try {
            ClientInput copy = (ClientInput) super.clone();
            copy.buttons = new HashSet<>(this.buttons);
            copy.lastButtons = new HashSet<>(this.lastButtons);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
