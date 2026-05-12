package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.MathUtil;
import lombok.Getter;

public class DefensiveLineCalculator {

    private static final double MINUS_DISTANCE_IF_NO_BALL = 4;

    private final TeamStrategy teamStrategy;
    @Getter
    private double defenseLine;

    public DefensiveLineCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    public void update() {
        double line = MathUtil.clamp(this.teamStrategy.ball.getPosition().getX() * this.teamStrategy.sideMultiplier - 20, -Game.MAX_X + 10, 0);

        // If opponent controls the ball → drop deeper
        if (this.teamStrategy.team.getOpponent().hasBall()) {
            line -= MINUS_DISTANCE_IF_NO_BALL;
        }

        this.defenseLine = line * this.teamStrategy.sideMultiplier;
    }
}
