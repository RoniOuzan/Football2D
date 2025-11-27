package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.game.TeamStrategy;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public class Defender extends Player {

    // Marking constants
    private static final double MARKING_IDEAL_DISTANCE = 5.0;     // ideal distance to the marked opponent
    private static final double MARKING_WEIGHT = 1.0;            // penalty per unit away from ideal

    public Defender(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public double getTargetScore(Player player, Translation2d target, TeamStrategy strategy) {
        return getMarkingScore(player, target, strategy.getTeam().getOpponent()) + getDefensiveLineScore(target, strategy.getDefenseLine());
    }

    private double getMarkingScore(Player player, Translation2d target, Team opponent) {
        Player closest = opponent.getPlayers().stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(player.getPosition())))
                .orElse(null);

        if (closest == null) return 0;

        double targetDist = target.getDistance(closest.getPosition());

        // How far the target position is from the ideal marking distance
        double idealDiff = Math.abs(targetDist - MARKING_IDEAL_DISTANCE);

        // ---- New continuous weight: stronger when opponent is closer ----
        double baseDistance = player.getPosition().getDistance(closest.getPosition());

        // Close opponent → weight ~1.2
        // Medium (8-12m) → weight ~0.8
        // Far (20m) → weight ~0.2
        double distanceFactor = 1.0 / (1.0 + baseDistance * 0.2);

        // ---- Ball possession factor ----
        // When opponent has the ball, we mark tighter (×2)
        // When we have the ball, there's still marking, but lighter (×0.5)
        double possessionFactor = opponent.hasBall() ? 1.0 : 0.2;

        double weight = MARKING_WEIGHT * distanceFactor * possessionFactor;

        return -idealDiff * weight;
    }

    private double getDefensiveLineScore(Translation2d target, double defenseLine) {
        return -Math.abs(target.getX() - defenseLine) * 0.5;  // penalty for leaving the line
    }
}


