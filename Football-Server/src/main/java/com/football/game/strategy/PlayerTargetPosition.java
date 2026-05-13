package com.football.game.strategy;

import com.football.game.Game;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Calculates the optimal target position for a player on the pitch.
 * <p>
 * This class uses a utility-based approach, combining global tactical scores 
 * with player-specific constraints like formation anchors, role-based shifts, 
 * and movement costs.
 */
public class PlayerTargetPosition {

    /** Penalty factor for being far from the designated formation anchor. */
    private static final double FORMATION_WEIGHT = 0.4;
    /** Penalty factor for moving too far from the current position (encourages persistence). */
    private static final double SELF_WEIGHT = 0.3;

    private static final double X_SHIFT_MULTIPLIER = 30;
    private static final double Y_SHIFT_MULTIPLIER = 20;
    /** The look-ahead time (in seconds) for predicting ball position when chasing. */
    private static final double BALL_PREDICTION_DELAY = 0.3;

    private final TeamStrategy teamStrategy;

    /** The player currently assigned to break formation and chase the ball directly. */
    @Setter
    @Getter
    private transient Player ballChaser = null;

    public PlayerTargetPosition(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Calculates the best target position for a specific player.
     * 
     * @param player The player to calculate for.
     * @return A {@link Translation2d} representing the optimal coordinate on the pitch.
     */
    public Translation2d getTargetPosition(Player player) {
        if (player.equals(this.ballChaser)) {
            // If this player is the chaser, they head toward where the ball is predicted to be
            return this.teamStrategy.ball.getPosition(BALL_PREDICTION_DELAY).toTranslation2d();
        }

        Optional<Map.Entry<Translation2d, Double>> bestEntry = this.teamStrategy.scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> addPlayerScore(player, e.getKey(), e.getValue())));

        return bestEntry.map(Map.Entry::getKey).orElse(player.getPosition());
    }

    /**
     * Adjusts the global base score with player-specific tactical weights.
     * 
     * @param player    The player being evaluated.
     * @param target    The potential target coordinate.
     * @param baseScore The generic tactical score of that coordinate.
     * @return The finalized score for this specific player at this specific target.
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
     * Calculates a collective shift vector for the entire formation.
     * <p>
     * As the ball moves, the "anchor" for every player shifts to follow the play.
     */
    private Translation2d getTeamShift() {
        return new Translation2d((this.teamStrategy.ball.getPosition().getX() / Game.MAX_X) * X_SHIFT_MULTIPLIER,
                (this.teamStrategy.ball.getPosition().getY() / Game.MAX_Y) * Y_SHIFT_MULTIPLIER);
    }
}
