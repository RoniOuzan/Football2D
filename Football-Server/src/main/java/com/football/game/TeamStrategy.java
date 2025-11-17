package com.football.game;

import com.football.game.players.*;
import com.football.util.math.geometry.Translation2d;

import java.util.*;

public class TeamStrategy {

    private static final int AMOUNT_OF_STEPS = 25;
    private static final double STEPS_X = Game.LENGTH / AMOUNT_OF_STEPS;
    private static final double STEPS_Y = Game.WIDTH / AMOUNT_OF_STEPS;

    private static final double WALL_DISTANCE_THRESHOLD = 12;
    private static final double WALL_WEIGHT = 50;

    private static final double PLAYER_DISTANCE_WEIGHT = 100;      // penalty for being close to teammates
    private static final double OPPONENT_DISTANCE_WEIGHT = 30;     // penalty for being close to opponents
    private static final double FORMATION_WEIGHT = 0.5;           // penalty for being far from formation
    private static final double SELF_WEIGHT = 1.0;                // penalty for moving too far from current pos
    private static final double BALL_WEIGHT = 0.1;                // attraction/repulsion to ball

    // Marking constants
    private static final double MARKING_IDEAL_DISTANCE = 8.0;     // ideal distance to the marked opponent
    private static final double MARKING_WEIGHT = 3.0;            // penalty per unit away from ideal

    // Space exploit bonus (attackers)
    private static final double SPACE_MIN = 10.0;
    private static final double SPACE_MAX = 30.0;
    private static final double SPACE_BONUS = 10.0;

    private transient final Team team;
    private transient final List<Player> players;
    private transient final Ball ball;
    private transient final double sideMultiplier;

    private final Map<Translation2d, Double> scores;

    public TeamStrategy(Game game, Team team) {
        this.team = team;
        this.players = team.getPlayers();
        this.ball = game.getBall();
        this.sideMultiplier = team.getSideMultiplier();

        this.scores = new HashMap<>();
    }

    /**
     * Player-agnostic score for a pose (walls, empty space, teammates, opponents).
     */
    private double calculateScore(Translation2d pose) {
        double score = 0;

        // Avoid clustering with teammates
        for (Player player : this.players) {
            double distance = pose.getDistance(player.getPosition());
            distance = Math.max(distance, 1.0);
            score -= PLAYER_DISTANCE_WEIGHT / distance;
        }

        // Avoid clustering with opponents (keep passing lanes and avoid marked areas)
        for (Player opp : this.team.getOpponent().getPlayers()) {
            double distance = pose.getDistance(opp.getPosition());
            distance = Math.max(distance, 1.0);
            score -= OPPONENT_DISTANCE_WEIGHT / distance;
        }

        // Avoid walls (non-linear)
        double distToWallX = Game.MAX_X - Math.abs(pose.getX());
        double distToWallY = Game.MAX_Y - Math.abs(pose.getY());
        if (distToWallX < WALL_DISTANCE_THRESHOLD) {
            double factor = (1 - distToWallX / WALL_DISTANCE_THRESHOLD);
            score -= WALL_WEIGHT * Math.pow(factor, 4);
        }
        if (distToWallY < WALL_DISTANCE_THRESHOLD) {
            double factor = (1 - distToWallY / WALL_DISTANCE_THRESHOLD);
            score -= WALL_WEIGHT * Math.pow(factor, 4);
        }

        // Offside
        double offsideLine = getOffsideLine();
        if (!Double.isNaN(offsideLine)) {
            if (this.sideMultiplier == 1)
                System.out.println(pose.getX() + " | " + offsideLine);
            if ((this.sideMultiplier == 1 && pose.getX() > offsideLine - 1) ) { // (this.sideMultiplier == -1 && pose.getX() < offsideLine + 1)
                score -= 500; // huge penalty; avoid this target
            }
        }

        // Mild attraction to ball for overall heatmap
        score -= BALL_WEIGHT * pose.getDistance(this.ball.getPosition());

        return score;
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
        double teamShiftX = getTeamShiftX();
        double roleShiftY = getRoleShift(player);
        Translation2d shiftedFormation = player.getOriginalPosition()
                .plus(new Translation2d(teamShiftX, roleShiftY));

        double score = baseScore
                - SELF_WEIGHT * distanceFromSelf
                - FORMATION_WEIGHT * target.getDistance(shiftedFormation);

        // Marking / guarding: defenders and midfielders try to be near a dangerous opponent
        if (player instanceof Defender || player instanceof Midfielder) {
            score += getMarkingScore(player, target);
        }

        // If attacker: apply offside prevention and opportunistic space exploitation
        if (player instanceof Attacker) {
            // Space exploitation bonus if target is in open space relative to opponents
            score += getSpaceExploitScore(target);
        }

        return score;
    }

    /**
     * Small positive/negative value encouraging player to be near an opponent at an "ideal" marking distance.
     * Negative absolute diff penalizes being far from ideal (we return negative value when diff large).
     */
    private double getMarkingScore(Player player, Translation2d target) {
        // Find the closest opponent to this player (by current player position)
        Player closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Player opp : this.team.getOpponent().getPlayers()) {
            double d = opp.getPosition().getDistance(player.getPosition());
            if (d < bestDist) {
                bestDist = d;
                closest = opp;
            }
        }
        if (closest == null) return 0;

        // Ideal: stay around MARKING_IDEAL_DISTANCE from that opponent
        double actualDist = target.getDistance(closest.getPosition());
        double diff = Math.abs(actualDist - MARKING_IDEAL_DISTANCE);

        // The smaller diff is, the better (i.e., small diff yields small penalty)
        // We want to reward being close to ideal -> subtracting a small penalty means higher score
        return -diff * MARKING_WEIGHT;
    }

    /**
     * Attackers get a small bonus for occupying space that's relatively far from opponents
     * but not ridiculously far (SPACE_MIN..SPACE_MAX).
     */
    private double getSpaceExploitScore(Translation2d target) {
        double bonus = 0;
        for (Player opp : this.team.getOpponent().getPlayers()) {
            double d = target.getDistance(opp.getPosition());
            if (d > SPACE_MIN && d < SPACE_MAX) {
                bonus += SPACE_BONUS;
            }
        }
        return bonus;
    }

    /**
     * Compute the offside line = second-last opponent Y coordinate (assuming higher Y is further up the pitch).
     * Returns NaN if not enough opponents.
     */
    private double getOffsideLine() {
        // Collect opponent Y positions excluding their goalkeeper if possible
        List<Double> xs = this.team.getOpponent().getPlayers().stream()
                .map(p -> p.getPosition().getX())
                .sorted()
                .toList();

        double line;
        if (this.sideMultiplier == 1) {
            line = xs.get(xs.size() - 2);
            if (line > 0) {
                return line;
            }
        } else {
            line = xs.get(1);
            if (line < 0) {
                return line;
            }
        }
        return Double.NaN;
    }

    /**
     * Public getter for player target
     */
    public Translation2d getTargetPosition(Player player) {
        if (player instanceof Goalkeeper) {
            return player.getOriginalPosition();
        }

        Optional<Map.Entry<Translation2d, Double>> bestEntry = scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> addPlayerScore(player, e.getKey(), e.getValue())));

        return bestEntry.map(Map.Entry::getKey).orElse(player.getPosition());
    }

    /**
     * Team lateral shift based on ball X position.
     */
    private double getTeamShiftX() {
        // Control how strong the shift is (tune as needed)
        return (this.ball.getPosition().getX() / Game.MAX_X) * 6.0;
    }

    /**
     * How much each role reacts to ball depth (Y axis).
     */
    private double getRoleShift(Player player) {
        double ballDepth = this.ball.getPosition().getY() / Game.MAX_Y;

        // How much each role reacts to ball depth (tune these)
        double ATT_SHIFT = 8.0;
        double MID_SHIFT = 28.0;
        double DEF_SHIFT = 24.0;

        if (player instanceof Attacker) {
            return ballDepth * ATT_SHIFT;
        } else if (player instanceof Midfielder) {
            return ballDepth * MID_SHIFT;
        } else if (player instanceof Defender) {
            return ballDepth * DEF_SHIFT;
        }
        return 0.0;
    }

    /**
     * Precompute the baseline (player-agnostic) heatmap.
     */
    public void update() {
        scores.clear();
        for (double i = -Game.MAX_X + (STEPS_X / 2); i < Game.MAX_X; i += STEPS_X) {
            for (double j = -Game.MAX_Y + (STEPS_Y / 2); j < Game.MAX_Y; j += STEPS_Y) {
                Translation2d pose = new Translation2d(i, j);
                scores.put(pose, calculateScore(pose));
            }
        }
    }
}
