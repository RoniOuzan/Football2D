package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.Team;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

public class Goalkeeper extends Player {

    public Goalkeeper(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public void handleMovement() {
        Translation2d goalPos = this.team.getOwnGoalPosition();

        Translation2d ballPos = this.ball.getPosition();

        // Stay near goal, but follow ball along goal line
        Translation2d target = new Translation2d(
                goalPos.getX(),
                MathUtil.clamp(ballPos.getY(), goalPos.getY() - 10, goalPos.getY() + 10)
        );

        setVelocity(target.minus(this.position).times(4));
    }
}

