package com.football.game.strategy;

import com.football.game.Game;
import com.football.game.players.Player;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

public class ScoreCalculator {

    private static final double WALL_DISTANCE_THRESHOLD = 12;
    private static final double WALL_WEIGHT = 50;

    private static final double PLAYER_DISTANCE_WEIGHT = 100;      // penalty for being close to teammates
    private static final double OPPONENT_DISTANCE_WEIGHT = 30;     // penalty for being close to opponents
    private static final double BALL_WEIGHT = 0.1;                // attraction/repulsion to ball

    // Space exploit bonus (attackers)
    private static final double SPACE_MIN = 5.0;
    private static final double SPACE_MAX = 20.0;
    private static final double SPACE_OPPONENT_BONUS = 5;

    private final TeamStrategy teamStrategy;

    public ScoreCalculator(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
    }

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

    private double calculatePlayersScore(Translation2d pose, double score) {
        // Avoid clustering with teammates
        for (Player player : this.teamStrategy.players) {
            double distance = pose.getDistance(player.getPosition());
            distance = Math.max(distance, 1.0);
            score -= PLAYER_DISTANCE_WEIGHT / distance;
        }

        // Avoid clustering with opponents (keep passing lanes and avoid marked areas)
        for (Player opp : this.teamStrategy.team.getOpponent().getPlayers()) {
            double distance = pose.getDistance(opp.getPosition());
            distance = Math.max(distance, 1.0);
            score -= OPPONENT_DISTANCE_WEIGHT / distance;
        }

        return score;
    }

    private double calculateWallsScore(Translation2d pose, double score) {
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

        return score;
    }

    private double calculateOffsideScore(Translation2d pose, double score) {
        if (!Double.isNaN(this.teamStrategy.getOffsideLine())) {
            if (MathUtil.isBetween(pose.getX() * this.teamStrategy.sideMultiplier, Math.abs(this.teamStrategy.getOffsideLine()) - 1, Game.MAX_X)) {
                return score - 500; // huge penalty; avoid this target
            }
        }
        return score;
    }

    private double calculateBallScore(Translation2d pose, double score) {
        return score - BALL_WEIGHT * pose.getDistance(this.teamStrategy.ball.getPosition2d());
    }

    /**
     * Attackers get a small bonus for occupying space that's relatively far from opponents
     * but not ridiculously far (SPACE_MIN..SPACE_MAX).
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
     * Small positive/negative value encouraging player to be near an opponent at an "ideal" marking distance.
     * Negative absolute diff penalizes being far from ideal (we return negative value when diff large).
     */

    private double getGoalThreatScore(Translation2d target) {
        Translation2d goal = new Translation2d(-Game.MAX_X * this.teamStrategy.sideMultiplier, 0);
        Translation2d ballPos = this.teamStrategy.ball.getPosition2d();

        // If ball far from goal → no need to defend deeply
        double ballDistance = ballPos.getDistance(goal);
        if (ballDistance > 35) // TODO: change it to percentage
            return 0;

        // Defender line direction: ball → goal center
        Translation2d lineDir = goal.minus(ballPos);
        double lineLength = lineDir.getNorm();

        // If somehow degenerate
        if (lineLength < 1e-5)
            return 0;

        // Unit direction along the line
        Translation2d unitDir = lineDir.div(lineLength);

        // Vector from ball → target
        Translation2d ballToTarget = target.minus(ballPos);

        // Projection length of (ball→target) onto line
        double projection = ballToTarget.dot(unitDir);

        // Closest point ON the line (ball → goal)
        projection = MathUtil.clamp(projection, 0, lineLength);
        Translation2d closestPoint = ballPos.plus(unitDir.times(projection));

        // Perpendicular distance from target to the line
        double perpendicularDistance = target.getDistance(closestPoint);

        // Avoid division by 0
        double d = Math.max(perpendicularDistance, 0.5);

        // Smooth urgency: closer ball is to goal → stronger effect
        double urgency = MathUtil.clamp(1.0 - (ballDistance / 40.0), 0, 1);
        urgency = urgency * urgency;

        // Score is positive: being close to the defensive line is good
        return (30 / d) * urgency;
    }

    private double getBlockGoalScore(Translation2d target) {
        // Encourage defenders to block the ball when it's near our goal
        double ballX = this.teamStrategy.ball.getPosition().getX();
        double goalX = -Game.MAX_X * this.teamStrategy.sideMultiplier;
        double ballDistToGoal = Math.abs(ballX - goalX);

        double score = 0;
        if (ballDistToGoal < 50) {
            if (target.getX() * this.teamStrategy.sideMultiplier < goalX) {
                score += 20;
            }

            Translation2d delta = new Translation2d(goalX, 0).minus(this.teamStrategy.ball.getPosition2d());
            Translation2d blockPosition = this.teamStrategy.ball.getPosition2d().plus(delta.times(0.5));

            double distance = target.getDistance(blockPosition);
            if (distance > 20) {
                score -= 10;
            } else if (distance < 10) {
                score += 20;
            } else {
                score += (20 - distance) * 2;
            }
        }
        return score;
    }
}
