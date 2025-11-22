package com.football.client;

import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

@ToString
public class InputValues implements Cloneable {
    public Set<String> keyboard;
    public Set<String> controller;
    public float[] axes;

    public InputValues() {
        this.keyboard = new HashSet<>();
        this.controller = new HashSet<>();
        this.axes = new float[0];
    }

    public boolean isHoldingKeyboard(String key) {
        return this.keyboard.contains(key);
    }

    public boolean isHoldingController(String button) {
        return this.controller.contains(button);
    }

    @Override
    public InputValues clone() throws CloneNotSupportedException {
        InputValues input = (InputValues) super.clone();
        input.keyboard = new HashSet<>(this.keyboard);
        input.controller = new HashSet<>(this.controller);
        input.axes = Arrays.copyOf(this.axes, this.axes.length);
        return input;
    }
}
