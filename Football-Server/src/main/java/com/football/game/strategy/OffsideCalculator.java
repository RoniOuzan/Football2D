package com.football.game.strategy;

import com.football.game.Game;
import com.football.util.math.MathUtil;
import lombok.Getter;

import java.util.List;

/**
 * Calculates the offside line for the team.
 * <p>
 * According to football rules, a player is in an offside position if they are in the 
 * opponent's half and closer to the opponent's goal line than both the ball and 
 * the second-to-last opponent.
 */
public class OffsideCalculator {

    /** The index offset to find the second-last opponent in a sorted list of positions. */
    private static final int SECOND_LAST_DEFENDER_OFFSET = 2;

    /** The X-coordinate of the halfway line. Players cannot be offside in their own half. */
    private static final double HALFWAY_LINE_X = 0.0;

    private final TeamStrategy teamStrategy;

    @Getter
    private double offsideLine;

    public OffsideCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Updates the offside line based on the current positions of the opponent's players and the ball.
     * <p>
     * The calculation sorts the opponents' X-positions relative to the attacking direction, 
     * identifies the second-to-last defender, and ensures the line never drops behind the 
     * ball or the halfway line.
     */
    public void update() {
        // Collect opponent X positions relative to our attacking side
        List<Double> xs = this.teamStrategy.team.getOpponent().getPlayers().stream()
                .map(p -> p.getPosition().getX() * this.teamStrategy.sideMultiplier)
                .sorted()
                .toList();

        double ballX = this.teamStrategy.ball.getPosition().getX() * this.teamStrategy.sideMultiplier;

        // The offside line is determined by the second-to-last defender
        double secondLastDefenderX = xs.get(xs.size() - SECOND_LAST_DEFENDER_OFFSET);

        // The line cannot be further back than the ball or the halfway line
        double effectiveMin = Math.max(ballX, HALFWAY_LINE_X);
        
        this.offsideLine = MathUtil.clamp(secondLastDefenderX, effectiveMin, Game.MAX_X) * this.teamStrategy.sideMultiplier;
    }
}
