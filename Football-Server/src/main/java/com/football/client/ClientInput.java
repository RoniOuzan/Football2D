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

        if (this.input.keyboard.contains("w"))
            y += 1;
        if (this.input.keyboard.contains("s"))
            y -= 1;
        if (this.input.keyboard.contains("d"))
            x += 1;
        if (this.input.keyboard.contains("a"))
            x -= 1;

        return new Translation2d(x, y).normalized();
    }

    public boolean isPressed(String key) {
        return this.input.isHoldingKeyboard(key) && !this.lastInput.isHoldingKeyboard(key);
    }

    public boolean isHolding(String key) {
        return this.input.isHoldingKeyboard(key);
    }

    public boolean isReleased(String key) {
        return !this.input.isHoldingKeyboard(key) && this.lastInput.isHoldingController(key);
    }

    public void updateInput() {
        this.lastInput = this.input;
    }
}
