package com.football.client;

import com.football.util.math.geometry.Translation2d;
import lombok.ToString;

@ToString
public class ClientInput {
    private InputValues input;
    private InputValues lastInput;

    public ClientInput() {
        this.input = new InputValues();
        this.lastInput = new InputValues();
    }

    public InputValues getInput() {
        return input;
    }

    public void updateInput(InputValues input) {
        this.input = input;
    }

    public Translation2d getRequestedVelocity() {
        double x = 0;
        double y = 0;

        if (this.input.keys.contains("w"))
            y += 1;
        if (this.input.keys.contains("s"))
            y -= 1;
        if (this.input.keys.contains("d"))
            x += 1;
        if (this.input.keys.contains("a"))
            x -= 1;

        return new Translation2d(x, y).normalize();
    }

    public boolean isPressed(String key) {
        return this.input.isHoldingKey(key) && !this.lastInput.isHoldingKey(key);
    }

    public boolean isHolding(String key) {
        return this.input.isHoldingKey(key);
    }

    public boolean isReleased(String key) {
        return !this.input.isHoldingKey(key) && this.lastInput.isHoldingKey(key);
    }

    public void updateInput() {
        this.lastInput = this.input;
    }
}
