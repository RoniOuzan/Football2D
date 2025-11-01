package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Team;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

public class Midfielder extends Player {

    public Midfielder(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public void handleMovement() {
        Player carrier = this.ball.getCarrier();

        Translation2d target;

        if (carrier != null && this.team.getOpponent() != null) {
            // Support attacker: find a spot between carrier and opponent goal
            Translation2d goalPos = this.team.getOpponent().getOwnGoalPosition();
            target = carrier.getPosition().plus(goalPos.minus(carrier.getPosition()).times(0.5));

            // Slight random offset for variability
            target = target.plus(new Translation2d(MathUtil.random(-5,5), MathUtil.random(-3,3)));
        } else {
            // Move toward the ball if no one has it
            target = this.ball.getPosition().minus(this.position).normalize();
        }

        setVelocity(target.times(6));
    }
}

