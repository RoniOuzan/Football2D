package com.football.game;

public class FieldHelper {

    private final int sideMultiplier;

    public FieldHelper(int sideMultiplier) {
        this.sideMultiplier = sideMultiplier;
    }

    public boolean isBetween(double x, double start, double end) {
        x *= this.sideMultiplier;
        double xPercent = (x + Game.MAX_X) / Game.LENGTH;

        return xPercent > start && xPercent < end;
    }
}
