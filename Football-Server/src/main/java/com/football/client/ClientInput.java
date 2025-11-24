package com.football.client;

import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ToString
public abstract class ClientInput implements Cloneable {

    private final Map<Keybind, String> keybinds;

    protected Set<String> buttons = new HashSet<>();
    private Set<String> lastButtons = new HashSet<>();

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

    public void updateInput(InputPacket.DevicePacket devicePacket) {
        this.lastButtons = this.buttons;

        this.buttons = devicePacket.buttons;
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
