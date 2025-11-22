package com.football.game;

import com.football.client.ClientInput;
import com.football.game.players.*;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

import java.util.*;

public class TeamStrategy {

    private static final int AMOUNT_OF_STEPS = 40;
    private static final double STEPS_X = Game.LENGTH / AMOUNT_OF_STEPS;
    private static final double STEPS_Y = Game.WIDTH / AMOUNT_OF_STEPS;

    private static final double WALL_DISTANCE_THRESHOLD = 12;
    private static final double WALL_WEIGHT = 50;

    private static final double PLAYER_DISTANCE_WEIGHT = 100;      // penalty for being close to teammates
    private static final double OPPONENT_DISTANCE_WEIGHT = 30;     // penalty for being close to opponents
    private static final double FORMATION_WEIGHT = 0.5;           // penalty for being far from formation
    private static final double SELF_WEIGHT = 0.3;                // penalty for moving too far from current pos
    private static final double BALL_WEIGHT = 0.1;                // attraction/repulsion to ball

    // Space exploit bonus (attackers)
    private static final double SPACE_MIN = 5.0;
    private static final double SPACE_MAX = 20.0;
    private static final double SPACE_OPPONENT_BONUS = 5;

    private static final double BALL_CHASE_LATENCY = 0.3;

    private transient final Team team;
    private transient final List<Player> players;
    private transient final Ball ball;
    private transient final double sideMultiplier;

    private transient Player ballChaser = null;

    private transient Player chosenPlayer = null;
    @SuppressWarnings(value = {"unused"})
    private int chosenPlayerIndex = -1; // for json

    private final Map<Translation2d, Double> scores;
    private double defenseLine;
    private double offsideLine;

    public TeamStrategy(Game game, Team team) {
        this.team = team;
        this.players = team.getPlayers();
        this.ball = game.getBall();
        this.sideMultiplier = team.getSideMultiplier();

        this.scores = new HashMap<>();
    }

    public double getDefenseLine() {
        return defenseLine;
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
        if (!Double.isNaN(this.offsideLine)) {
            if (MathUtil.isBetween(pose.getX() * this.sideMultiplier, Math.abs(this.offsideLine) - 1, Game.MAX_X)) {
                score -= 500; // huge penalty; avoid this target
            }
        }

        if (this.team.hasBall()) {
            score += getSpaceExploitScore(pose);
        } else {
            score += getGoalThreatScore(pose);
            score += getBlockGoalScore(pose);
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
        Translation2d teamShift = getTeamShift();
        Translation2d shiftedFormation = player.getFormationPosition().plus(teamShift);

        return baseScore
                - SELF_WEIGHT * distanceFromSelf
                - FORMATION_WEIGHT * target.getDistance(shiftedFormation)
                + player.getTargetScore(player, target, this);
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
        Translation2d goal = new Translation2d(-Game.MAX_X * this.sideMultiplier, 0);
        Translation2d ballPos = ball.getPosition();

        // If ball far from goal → no need to defend deeply
        double ballDistance = ballPos.getDistance(goal);
        if (ballDistance > 35)
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
        double ballX = ball.getPosition().getX();
        double goalX = -Game.MAX_X * this.sideMultiplier;
        double ballDistToGoal = Math.abs(ballX - goalX);

        double score = 0;
        if (ballDistToGoal < 50) {
            if (target.getX() * sideMultiplier < goalX) {
                score += 20;
            }

            Translation2d delta = new Translation2d(goalX, 0).minus(this.ball.getPosition());
            Translation2d blockPosition = this.ball.getPosition().plus(delta.times(0.5));

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

    private double computeDefensiveLineX() {
        double line = MathUtil.clamp(this.ball.getPosition().getX() * this.sideMultiplier - 20, -Game.MAX_X + 10, 0);

        // If opponent controls the ball → drop deeper
        if (this.team.getOpponent().hasBall()) {
            line -= 4;
        }

        return line * this.sideMultiplier;
    }

    /**
     * Compute the offside line = second-last opponent Y coordinate (assuming higher Y is further up the pitch).
     * Returns NaN if not enough opponents.
     */
    private double getOffsideLine() {
        // Collect opponent Y positions excluding their goalkeeper if possible
        List<Double> xs = this.team.getOpponent().getPlayers().stream()
                .map(p -> p.getPosition().getX() * this.sideMultiplier)
                .sorted()
                .toList();

        double ballX = this.ball.getPosition().getX() * this.sideMultiplier;
        double line = xs.get(xs.size() - 2);
        return MathUtil.clamp(line, Math.max(ballX, 0), Game.MAX_X) * this.sideMultiplier;
    }

    /**
     * Public getter for player target
     */
    public Translation2d getTargetPosition(Player player) {
        if (player.equals(this.ballChaser)) {
            return this.ball.getPosition(); // chase the ball position before 0.5 seconds, so it will have a bit of delay
        }

        Optional<Map.Entry<Translation2d, Double>> bestEntry = this.scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> addPlayerScore(player, e.getKey(), e.getValue())));

        return bestEntry.map(Map.Entry::getKey).orElse(player.getPosition());
    }

    /**
     * Team lateral shift based on ball X position.
     */
    private Translation2d getTeamShift() {
        // Control how strong the shift is (tune as needed)
        return new Translation2d((this.ball.getPosition().getX() / Game.MAX_X) * 30,
                (this.ball.getPosition().getY() / Game.MAX_Y) * 20);
    }

    private Player chooseBallChaser() {
        return this.players.stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.ball.getPosition(BALL_CHASE_LATENCY))))
                .orElse(null);
    }

    public Player getBallChaser() {
        return ballChaser;
    }

    /**
     * Precompute the baseline (player-agnostic) heatmap.
     */
    public void update() {
        this.setChosenPlayer(choosePlayer());
        this.ballChaser = chooseBallChaser();

        this.defenseLine = computeDefensiveLineX();
        this.offsideLine = getOffsideLine();

        this.scores.clear();
        for (double i = -Game.MAX_X + (STEPS_X / 2); i < Game.MAX_X; i += STEPS_X) {
            for (double j = -Game.MAX_Y + (STEPS_Y / 2); j < Game.MAX_Y; j += STEPS_Y) {
                Translation2d pose = new Translation2d(i, j);
                this.scores.put(pose, calculateScore(pose));
            }
        }

        this.handleControlledMovement(this.chosenPlayer, this.team.getClient().getInput());
        for (Player player : this.players) {
            if (!player.equals(this.chosenPlayer)) {
                if (player instanceof Goalkeeper gk) {
                    gk.handleTarget(this);
                    continue;
                }

                player.moveTowards(this.getTargetPosition(player), 1);
            }
        }
    }

    public void handleControlledMovement(Player player,ClientInput input) {
        double velocity = input.isHolding("shift") ? Player.SPRINT_VELOCITY : Player.WALK_VELOCITY;

        Translation2d targetVelocity = input.getRequestedVelocity().times(velocity);

        if (player.equals(this.ballChaser) && this.ball.getCarrier() == null) {
            Translation2d chaseVelocity = player.getVelocityToPosition(this.ball.getPredictedPosition(0.5), 1);
            targetVelocity = targetVelocity.times(0.4).plus(chaseVelocity);
        }

        player.setVelocity(targetVelocity);

        if (player.hasBall()) {
            if (input.isHolding("e")) {
                Player playerToPass = getPlayerToPass(player);
                player.pass(playerToPass);
                this.setChosenPlayer(playerToPass);
            } else if (input.isHolding("f")) {
                Player playerToPass = getPlayerToPass(player);
                player.through(playerToPass);
                this.setChosenPlayer(playerToPass);
            } else if (input.isHolding("r")) {
                player.shoot();
            }
        }
    }

    private Player getPlayerToPass(Player player) {
        return this.team.getPlayers().stream()
                .filter(p -> !p.equals(player))
                .min(Comparator.comparingDouble(p -> {
                    Translation2d delta = p.getPosition().minus(player.getPosition());
                    double angleDiff = Math.abs(delta.getAngle().minus(player.getWantedDirection()).getRadians());

                    return 1 * angleDiff + 0.05 * delta.getNorm();
                }))
                .orElse(null);
    }

    public void setChosenPlayer(Player chosenPlayer) {
        this.chosenPlayer = chosenPlayer;
        this.chosenPlayerIndex = this.players.indexOf(chosenPlayer);
    }

    private Player choosePlayer() {
        if (this.team.hasBall()) {
            return this.ball.getCarrier();
        }

        if (this.chosenPlayer == null || this.chosenPlayer instanceof Goalkeeper || this.team.getClient().getInput().isPressed("q")) {
            return this.team.getClosestPlayerToBall(p -> !p.equals(this.chosenPlayer) && !(p instanceof Goalkeeper));
        }

        return this.chosenPlayer;
    }
}
