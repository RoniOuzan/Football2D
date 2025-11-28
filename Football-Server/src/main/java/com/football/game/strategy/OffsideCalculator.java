package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.MathUtil;

import java.util.List;

public class OffsideCalculator {

    private final TeamStrategy teamStrategy;
    private double offsideLine;

    public OffsideCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Compute the offside line = second-last opponent Y coordinate (assuming higher Y is further up the pitch).
     * Returns NaN if not enough opponents.
     */
    public void update() {
        // Collect opponent Y positions excluding their goalkeeper if possible
        List<Double> xs = this.teamStrategy.team.getOpponent().getPlayers().stream()
                .map(p -> p.getPosition().getX() * this.teamStrategy.sideMultiplier)
                .sorted()
                .toList();

        double ballX = this.teamStrategy.ball.getPosition().getX() * this.teamStrategy.sideMultiplier;
        double line = xs.get(xs.size() - 2);
        this.offsideLine = MathUtil.clamp(line, Math.max(ballX, 0), Game.MAX_X) * this.teamStrategy.sideMultiplier;
    }

    public double getOffsideLine() {
        return this.offsideLine;
    }
}
