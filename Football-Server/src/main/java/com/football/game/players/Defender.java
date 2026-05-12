package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

/**
 * Specialization of {@link Player} representing a defensive unit.
 * Focuses on marking opponents and maintaining the team's defensive line.
 */
public class Defender extends Player {

    private static final double MARKING_IDEAL_DISTANCE = 5.0;
    private static final double MARKING_WEIGHT = 1.0;

    /** Constant used to reduce marking urgency as the opponent gets further away. */
    private static final double MARKING_DISTANCE_DECAY = 0.2;

    /** Multiplier for marking weight when the opponent has the ball. */
    private static final double POSSESSION_MARKING_MULTIPLIER = 1.0;

    /** Multiplier for marking weight when the opponent does NOT have the ball. */
    private static final double NON_POSSESSION_MARKING_MULTIPLIER = 0.2;

    /** How strictly the defender adheres to the defensive line (higher = more strict). */
    private static final double DEFENSIVE_LINE_STRICTNESS = 0.5;

    public Defender(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    /**
     * Evaluates a potential target position based on defensive priorities:
     * Marking the nearest opponent and staying aligned with the defensive line.
     */
    @Override
    public double getTargetScore(Player player, Translation2d target, TeamStrategy strategy) {
        return getMarkingScore(player, target, strategy.getTeam().getOpponent()) + getDefensiveLineScore(target, strategy.getDefenseLine());
    }

    /**
     * Calculates a utility score for marking the closest opponent.
     * Penalizes positions that are too far from the ideal marking distance.
     */
    private double getMarkingScore(Player player, Translation2d target, Team opponent) {
        Player closest = opponent.getPlayers().stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(player.getPosition())))
                .orElse(null);

        if (closest == null) return 0;

        double targetDist = target.getDistance(closest.getPosition());

        // Deviation from the ideal "cushion" distance
        double idealDiff = Math.abs(targetDist - MARKING_IDEAL_DISTANCE);

        double baseDistance = player.getPosition().getDistance(closest.getPosition());
        
        // Stronger marking when the opponent is physically closer to the defender
        double distanceFactor = 1.0 / (1.0 + baseDistance * MARKING_DISTANCE_DECAY);

        // Tighten marking significantly if the opponent team is in possession
        double possessionFactor = opponent.hasBall() ? POSSESSION_MARKING_MULTIPLIER : NON_POSSESSION_MARKING_MULTIPLIER;

        double weight = MARKING_WEIGHT * distanceFactor * possessionFactor;
        return -idealDiff * weight;
    }

    /**
     * Penalizes positions based on their distance from the team's tactical defensive line.
     */
    private double getDefensiveLineScore(Translation2d target, double defenseLine) {
        return -Math.abs(target.getX() - defenseLine) * DEFENSIVE_LINE_STRICTNESS;
    }
}
