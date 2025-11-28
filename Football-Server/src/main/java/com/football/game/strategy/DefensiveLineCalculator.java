package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.MathUtil;

public class DefensiveLineCalculator {
    private final TeamStrategy teamStrategy;
    private double defenseLine;

    public DefensiveLineCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    public void update() {
        double line = MathUtil.clamp(this.teamStrategy.ball.getPosition().getX() * this.teamStrategy.sideMultiplier - 20, -Game.MAX_X + 10, 0);

        // If opponent controls the ball → drop deeper
        if (this.teamStrategy.team.getOpponent().hasBall()) {
            line -= 4;
        }

        this.defenseLine = line * this.teamStrategy.sideMultiplier;
    }

    public double getDefenseLine() {
        return this.defenseLine;
    }
}
