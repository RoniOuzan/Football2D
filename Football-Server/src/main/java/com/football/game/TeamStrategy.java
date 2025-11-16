package com.football.game;

import com.football.game.players.Goalkeeper;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

import java.util.*;

public class TeamStrategy {

    private static final int AMOUNT_OF_STEPS = 25;
    private static final double STEPS_X = Game.LENGTH / AMOUNT_OF_STEPS;
    private static final double STEPS_Y = Game.WIDTH / AMOUNT_OF_STEPS;

    private static final double WALL_DISTANCE_THRESHOLD = 12;
    private static final double WALL_WEIGHT = 50;

    private static final double PLAYER_DISTANCE_WEIGHT = 100;      // penalty for being close to teammates
    private static final double FORMATION_WEIGHT = 0.5;           // penalty for being far from formation
    private static final double SELF_WEIGHT = 1.0;                // penalty for moving too far from current pos
    private static final double BALL_WEIGHT = 0.1;                // attraction/repulsion to ball

    private transient final List<Player> players;
    private transient final Ball ball;

    private final Map<Translation2d, Double> scores;

    public TeamStrategy(Game game, Team team) {
        this.players = team.getPlayers();
        this.ball = game.getBall();
        this.scores = new HashMap<>();
    }

    private double calculateScore(Translation2d pose) {
        Translation2d teamShift = getTeamShift();
        Translation2d shiftedPose = pose.plus(teamShift);

        double score = 0;

        // Everything now uses shiftedPose instead of pose:
        for (Player player : this.players) {
            double distance = shiftedPose.getDistance(player.getPosition());
            distance = Math.max(distance, 1);
            score -= PLAYER_DISTANCE_WEIGHT / distance;
        }

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

        score -= BALL_WEIGHT * pose.getDistance(ball.getPosition());

        return score;
    }


    private double addPlayerScore(Player player, Translation2d target, double baseScore) {
        double distanceFromSelf = target.getDistance(player.getPosition());
        double distanceFromFormation = target.getDistance(player.getOriginalPosition());

        return baseScore
                - SELF_WEIGHT * distanceFromSelf
                - FORMATION_WEIGHT * distanceFromFormation;
    }

    public Translation2d getTargetPosition(Player player) {
        if (player instanceof Goalkeeper) {
            return player.getOriginalPosition();
        }

        Optional<Map.Entry<Translation2d, Double>> bestEntry = scores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> addPlayerScore(player, e.getKey(), e.getValue())));

        return bestEntry.map(Map.Entry::getKey).orElse(player.getPosition());
    }

    private Translation2d getTeamShift() {
        Translation2d ballPos = this.ball.getPosition();

        // Normalize field coordinates into [-1, +1]
        double shiftX = ballPos.getX() / Game.MAX_X;
        double shiftY = ballPos.getY() / Game.MAX_Y;

        // Control how strong the shift is
        double SHIFT_AMOUNT = 10;

        return new Translation2d(shiftX * SHIFT_AMOUNT, shiftY * SHIFT_AMOUNT);
    }


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
