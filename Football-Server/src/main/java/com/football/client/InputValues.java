package com.football.client;

import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@ToString
public class InputValues implements Cloneable {
    public Set<String> keys;

    public InputValues() {
        this.keys = new HashSet<>();
    }

    public boolean isHoldingKey(String key) {
        return this.keys.contains(key);
    }

    @Override
    public InputValues clone() throws CloneNotSupportedException {
        InputValues input = (InputValues) super.clone();
        input.keys = new HashSet<>(this.keys);
        return input;
    }
}
