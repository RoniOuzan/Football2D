package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public class Defender extends Player {

    public Defender(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public void handleMovement() {
        // Stay between ball and own goal
        Translation2d goalPos = team.getOwnGoalPosition();
        Translation2d target = ball.getPosition().minus(goalPos).times(0.5).plus(goalPos);

        // Mark closest opponent
        Player closestOpponent = null;
        if (this.team.getOpponent() != null) {
             closestOpponent = this.team.getOpponent().getPlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.getPosition().minus(this.position).getNorm()))
                    .orElse(null);
        }

        if (closestOpponent != null) {
            Translation2d opponentPos = closestOpponent.getPosition();
            target = opponentPos.plus(goalPos.minus(opponentPos).times(0.3));
        }

        // Move toward target
        setVelocity(target.minus(this.position).times(5));
    }
}


