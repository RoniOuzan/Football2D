package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.MathUtil;
import lombok.Getter;

public class DefensiveLineCalculator {

    /** The distance the defensive line drops back when the opponent has possession. */
    private static final double OPPONENT_POSSESSION_DROP_DISTANCE = 4.0;

    /** The distance the defensive line tries to maintain behind the ball's X position. */
    private static final double BALL_OFFSET_DISTANCE = 20.0;

    /** The buffer distance from the team's own goal line to prevent the defense from sitting inside the net. */
    private static final double OWN_GOAL_LINE_BUFFER = 10.0;

    /** The furthest forward the defensive line will push (0 represents the halfway line). */
    private static final double MAX_DEFENSIVE_LINE = 0.0;

    private final TeamStrategy teamStrategy;
    @Getter
    private double defenseLine;

    public DefensiveLineCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Calculates the target X-coordinate for the team's defensive line.
     * <p>
     * The line tracks the ball's position with an offset, clamped between the
     * team's own goal area and the halfway line. If the opponent has the ball,
     * the defense drops deeper to prevent long balls and through-passes.
     */
    public void update() {
        // Calculate baseline position relative to the ball
        double ballXRelative = this.teamStrategy.ball.getPosition().getX() * this.teamStrategy.sideMultiplier;
        double minLine = -Game.MAX_X + OWN_GOAL_LINE_BUFFER;

        double line = MathUtil.clamp(ballXRelative - BALL_OFFSET_DISTANCE, minLine, MAX_DEFENSIVE_LINE);

        // If opponent controls the ball → drop deeper
        if (this.teamStrategy.team.getOpponent().hasBall()) {
            line -= OPPONENT_POSSESSION_DROP_DISTANCE;
        }

        this.defenseLine = line * this.teamStrategy.sideMultiplier;
    }
}
