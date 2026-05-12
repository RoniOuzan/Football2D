package com.football.game.strategy;

import com.football.game.Game;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class PlayerTargetPosition {

    private static final double FORMATION_WEIGHT = 0.5;           // penalty for being far from formation
    private static final double SELF_WEIGHT = 0.3;                // penalty for moving too far from current pos

    private final TeamStrategy teamStrategy;

    @Setter
    @Getter
    private transient Player ballChaser = null;

    public PlayerTargetPosition(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Public getter for player target
     */
    public Translation2d getTargetPosition(Player player) {
        if (player.equals(this.ballChaser)) {
            return this.teamStrategy.ball.getPosition(0.3).toTranslation2d(); // chase the ball position before 0.3 seconds, so it will have a bit of delay
        }

        Optional<Map.Entry<Translation2d, Double>> bestEntry = this.teamStrategy.scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> addPlayerScore(player, e.getKey(), e.getValue())));

        return bestEntry.map(Map.Entry::getKey).orElse(player.getPosition());
    }

    /**
     * Add player-specific modifiers:
     * - distance from current position (SELF_WEIGHT)
     * - distance from (role-shifted) formation anchor (FORMATION_WEIGHT)
     * - marking score for defenders/midfielders
     * - offside prevention and space exploitation for attackers
     */
    private double addPlayerScore(Player player, Translation2d target, double baseScore) {
        double distanceFromSelf = target.getDistance(player.getPosition());

        // Compute role-based formation anchor shift
        Translation2d teamShift = getTeamShift();
        Translation2d shiftedFormation = player.getFormationPosition().plus(teamShift);

        return baseScore
                - SELF_WEIGHT * distanceFromSelf
                - FORMATION_WEIGHT * target.getDistance(shiftedFormation)
                + player.getTargetScore(player, target, this.teamStrategy);
    }

    /**
     * Team lateral shift based on ball X position.
     */
    private Translation2d getTeamShift() {
        // Control how strong the shift is (tune as needed)
        return new Translation2d((this.teamStrategy.ball.getPosition().getX() / Game.MAX_X) * 30,
                (this.teamStrategy.ball.getPosition().getY() / Game.MAX_Y) * 20);
    }
}
