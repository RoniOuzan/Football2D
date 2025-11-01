package com.football.game;

import com.football.util.math.geometry.Translation2d;

public class BotResult {
    private final Translation2d targetPosition;
    private final double speed;

    public BotResult(Translation2d targetPosition, double speed) {
        this.targetPosition = targetPosition;
        this.speed = speed;
    }

    public Translation2d getTargetPosition() {
        return targetPosition;
    }

    public double getSpeed() {
        return speed;
    }
}
