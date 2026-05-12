package com.football.game.strategy;

import com.football.game.Game;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

/**
 * Evaluates the tactical value (score) of a specific coordinate on the pitch.
 * This is used by the AI to determine the best target positions for off-ball movement,
 * balancing offensive space exploitation, defensive goal-side positioning, and general spacing.
 */
public class ScoreCalculator {

    private static final double WALL_DISTANCE_THRESHOLD = 12;
    private static final double WALL_WEIGHT = 50;
    private static final double WALL_PENALTY_EXPONENT = 4;

    private static final double PLAYER_DISTANCE_WEIGHT = 100; // penalty for being close to teammates
    private static final double OPPONENT_DISTANCE_WEIGHT = 30; // penalty for being close to opponents
    private static final double BALL_WEIGHT = -0.1; // attraction/repulsion to ball
    private static final double MIN_PROXIMITY_DIST = 1.0; // prevents division by zero in clustering logic

    // Space exploit bonus (attackers)
    private static final double SPACE_MIN = 5.0;
    private static final double SPACE_MAX = 20.0;
    private static final double SPACE_OPPONENT_BONUS = 5;

    // Offside logic
    private static final double OFFSIDE_LINE_BUFFER = 1.0;
    private static final double OFFSIDE_PENALTY = -500.0;

    // Defensive positioning
    private static final double GOAL_THREAT_BALL_DIST_THRESHOLD = 35.0;
    private static final double GOAL_THREAT_URGENCY_DIVISOR = 40.0;
    private static final double GOAL_THREAT_WEIGHT = 30.0;
    private static final double MIN_PERPENDICULAR_DIST = 0.5;
    private static final double EPSILON = 1e-5;

    // Goal blocking
    private static final double BLOCK_GOAL_BALL_THRESHOLD = 40.0;
    private static final double BLOCK_GOAL_BASE_SCORE = 20.0;
    private static final double BLOCK_POSITION_RATIO = 0.5;
    private static final double BLOCK_DISTANCE_FAR = 20.0;
    private static final double BLOCK_DISTANCE_NEAR = 10.0;
    private static final double BLOCK_PROXIMITY_SCALER = 2.0;

    private final TeamStrategy teamStrategy;

    public ScoreCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

    /**
     * Aggregates various tactical scores into a single utility value for a given position.
     * Considering everything that effects how good is the position in the current situation of the game.
     */
    protected double calculateScore(Translation2d pose) {
        double score = 0;

        score = calculatePlayersScore(pose, score);
        score = calculateWallsScore(pose, score);

        score = calculateOffsideScore(pose, score);

        if (this.teamStrategy.team.hasBall()) {
            score += getSpaceExploitScore(pose);
        } else {
            score += getGoalThreatScore(pose);
            score += getBlockGoalScore(pose);
        }

        score = calculateBallScore(pose, score);

        return score;
    }

    /**
     * Applies penalties for being too close to other players to maintain proper field spacing.
     */
    private double calculatePlayersScore(Translation2d pose, double score) {
        // Avoid clustering with teammates
        for (Player player : this.teamStrategy.players) {
            double distance = pose.getDistance(player.getPosition());
            distance = Math.max(distance, MIN_PROXIMITY_DIST);
            score -= PLAYER_DISTANCE_WEIGHT / distance;
        }

        // Avoid clustering with opponents (keep passing lanes and avoid marked areas)
        for (Player opp : this.teamStrategy.team.getOpponent().getPlayers()) {
            double distance = pose.getDistance(opp.getPosition());
            distance = Math.max(distance, MIN_PROXIMITY_DIST);
            score -= OPPONENT_DISTANCE_WEIGHT / distance;
        }

        return score;
    }

    /**
     * Discourages players from hugging the touchlines or goal lines.
     */
    private double calculateWallsScore(Translation2d pose, double score) {
        double distToWallX = Game.MAX_X - Math.abs(pose.getX());
        double distToWallY = Game.MAX_Y - Math.abs(pose.getY());
        if (distToWallX < WALL_DISTANCE_THRESHOLD) {
            double factor = (1 - distToWallX / WALL_DISTANCE_THRESHOLD);
            score -= WALL_WEIGHT * Math.pow(factor, WALL_PENALTY_EXPONENT);
        }
        if (distToWallY < WALL_DISTANCE_THRESHOLD) {
            double factor = (1 - distToWallY / WALL_DISTANCE_THRESHOLD);
            score -= WALL_WEIGHT * Math.pow(factor, WALL_PENALTY_EXPONENT);
        }

        return score;
    }

    /**
     * Penalizes positions that would result in an offside violation.
     */
    private double calculateOffsideScore(Translation2d pose, double score) {
        if (!Double.isNaN(this.teamStrategy.getOffsideLine())) {
            double absOffsideLine = Math.abs(this.teamStrategy.getOffsideLine());
            
            if (MathUtil.isBetween(pose.getX() * this.teamStrategy.sideMultiplier, absOffsideLine - OFFSIDE_LINE_BUFFER, Game.MAX_X)) {
                return score + OFFSIDE_PENALTY; // huge penalty; avoid this target
            }
        }
        return score;
    }

    /**
     * Simple attraction/repulsion score based on ball proximity.
     */
    private double calculateBallScore(Translation2d pose, double score) {
        return score + BALL_WEIGHT * pose.getDistance(this.teamStrategy.ball.getPosition2d());
    }

    /**
     * Attackers get a small bonus for occupying space that's relatively far from opponents
     * but not ridiculously far (SPACE_MIN…SPACE_MAX).
     */
    private double getSpaceExploitScore(Translation2d target) {
        double bonus = 0;
        for (Player opp : this.teamStrategy.team.getOpponent().getPlayers()) {
            double d = target.getDistance(opp.getPosition());
            if (d > SPACE_MIN && d < SPACE_MAX) {
                bonus += SPACE_OPPONENT_BONUS;
            }
        }
        return bonus;
    }

    /**
     * Encourages defensive positioning on the line between the ball and the center of the goal.
     * The urgency of this positioning increases as the ball approaches the goal.
     */
    private double getGoalThreatScore(Translation2d target) {
        Translation2d goal = this.teamStrategy.team.getOwnGoalPosition();
        Translation2d ballPos = this.teamStrategy.ball.getPosition2d();

        // If ball far from goal, no need to defend deeply
        double ballDistance = ballPos.getDistance(goal);
        if (ballDistance > GOAL_THREAT_BALL_DIST_THRESHOLD)
            return 0;

        // Defender line direction: ball -> goal center
        Translation2d lineDir = goal.minus(ballPos);
        double lineLength = lineDir.getNorm();

        // If somehow very close to the goal
        if (lineLength < EPSILON) // If the ball is effectively at the goal, the line is degenerate
            return 0;

        // Unit direction along the line from ball to goal
        Translation2d unitDir = lineDir.normalized();

        // Vector from ball -> target
        Translation2d ballToTarget = target.minus(ballPos);

        // Projection length of (ball -> target) onto line
        double projection = ballToTarget.dot(unitDir);

        // Closest point ON the line (ball -> goal)
        projection = MathUtil.clamp(projection, 0, lineLength);
        Translation2d closestPoint = ballPos.plus(unitDir.times(projection));

        // Perpendicular distance from target to the line
        double perpendicularDistance = target.getDistance(closestPoint);

        // Avoid division by 0
        double d = Math.max(perpendicularDistance, MIN_PERPENDICULAR_DIST);

        // Smooth urgency: closer ball is to goal -> stronger effect
        double urgency = MathUtil.clamp(1.0 - (ballDistance / GOAL_THREAT_URGENCY_DIVISOR), 0, 1);
        urgency = urgency * urgency;

        // Score is positive: being close to the defensive line is good
        return (GOAL_THREAT_WEIGHT / d) * urgency;
    }

    /**
     * Increases the utility of positions that block the path of the ball when it is near the team's goal.
     */
    private double getBlockGoalScore(Translation2d target) {
        // Encourage defenders to block the ball when it's near our goal
        double ballX = this.teamStrategy.ball.getPosition().getX();
        double goalX = -Game.MAX_X * this.teamStrategy.sideMultiplier;
        double ballDistToGoal = Math.abs(ballX - goalX);

        double score = 0;
        if (ballDistToGoal < BLOCK_GOAL_BALL_THRESHOLD) {
            if (target.getX() * this.teamStrategy.sideMultiplier < goalX) {
                score += BLOCK_GOAL_BASE_SCORE;
            }

            // Calculate the position in BLOCK_POSITION_RATIO percent from the ball to the player
            Translation2d delta = this.teamStrategy.team.getOwnGoalPosition().minus(this.teamStrategy.ball.getPosition2d());
            Translation2d blockPosition = this.teamStrategy.ball.getPosition2d().plus(delta.times(BLOCK_POSITION_RATIO));

            double distance = target.getDistance(blockPosition);
            if (distance > BLOCK_DISTANCE_FAR) {
                score -= (BLOCK_GOAL_BASE_SCORE / 2.0); // Penalty for being too far from block spot
            } else if (distance < BLOCK_DISTANCE_NEAR) {
                score += BLOCK_GOAL_BASE_SCORE;
            } else {
                score += (BLOCK_GOAL_BASE_SCORE - distance) * BLOCK_PROXIMITY_SCALER;
            }
        }
        return score;
    }
}
