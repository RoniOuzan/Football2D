package com.football.client;

import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@ToString
public class InputValues implements Cloneable {
    // Keyboard keys pressed
    public Set<String> keys = new HashSet<>();
    // Controller buttons pressed (normalized to Xbox style)
    public Set<String> buttons = new HashSet<>();

    public double leftX = 0;
    public double leftY = 0;
    public double rightX = 0;
    public double rightY = 0;
    public double LT = 0;
    public double RT = 0;

    public boolean isHoldingKey(String key) {
        return this.keys.contains(key);
    }

    public boolean isHoldingButton(String button) {
        return this.buttons.contains(button);
    }

    /** Deep clone of the object */
    @Override
    public InputValues clone() {
        try {
            InputValues copy = (InputValues) super.clone();
            copy.keys = new HashSet<>(this.keys);
            copy.buttons = new HashSet<>(this.buttons);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
