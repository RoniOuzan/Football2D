package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.util.math.geometry.Translation2d;

public class Attacker extends Player {

    public Attacker(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public void handleMovement() {
        Player carrier = this.ball.getCarrier();

        Translation2d target;

        if (carrier != null && this.team.getOpponent() != null) {
            target = findBestPassingLane(carrier);
        } else {
            // Move toward the ball if no one has it
            target = this.ball.getPosition().minus(this.position).normalize();
        }

        setVelocity(target.times(7));
    }

    private Translation2d findBestPassingLane(Player carrier) {
        Translation2d myPos = getPosition();
        Translation2d ballPos = carrier.getPosition();

        // Generate candidate positions around opponent goal
        double goalX = 50; // opponent goal x-coordinate
        Translation2d[] candidates = new Translation2d[] {
                new Translation2d(goalX, 10),
                new Translation2d(goalX, 0),
                new Translation2d(goalX, -10),
                new Translation2d(goalX - 5, 5),
                new Translation2d(goalX - 5, -5)
        };

        Translation2d best = candidates[0];
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Translation2d candidate : candidates) {
            double distanceFromCarrier = candidate.minus(ballPos).getNorm();
            double distanceFromOpponents = this.team.getOpponent().getPlayers().stream()
                    .mapToDouble(p -> candidate.minus(p.getPosition()).getNorm())
                    .min()
                    .orElse(0);

            // Score favors positions closer to ball carrier but far from opponents
            double score = -distanceFromCarrier + 2 * distanceFromOpponents;

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best.minus(myPos).normalize();
    }
}


