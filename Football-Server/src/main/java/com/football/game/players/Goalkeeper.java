package com.football.game.players;

import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.Team;
import com.football.game.strategy.TeamStrategy;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

public class Goalkeeper extends Player {

    public Goalkeeper(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    public void jumpTo(Translation2d position) {
        setVelocity(getVelocityToPosition(position, 2), 30, 30, 30);
    }

    public void handleTarget(TeamStrategy strategy) {
        Team team = strategy.getTeam();
        Translation2d goalCenter = team.getOwnGoalPosition();
        Translation2d predictedBall = this.ball.getPredictedPosition(0.3).toTranslation2d();

        double ballDistanceToGoal = this.ball.getPosition().getDistance(goalCenter);

        if (this.ball.getCarrier() == null && this.ball.getVelocity().getNorm() > 4.0) {
            Translation2d ballVel = this.ball.getVelocity2d().normalized();
            Translation2d dirToGoal = goalCenter.minus(this.ball.getPosition2d()).normalized();

            // dot > 0.8 → angle < ~36 degrees → toward goal
            if (ballVel.dot(dirToGoal) > 0.8) {
                // Compute intersection with goal line (simple version)
                double t = (this.position.getX() - this.ball.getPosition().getX()) / ballVel.getX(); // time to reach goal line

                if (t > 0) { // valid
                    double impactY = this.ball.getPosition().getY() + ballVel.getY() * t;

                    // clamp to goal posts height
                    impactY = MathUtil.clamp(impactY,
                            goalCenter.getY() - Game.GOAL_WIDTH / 2,
                            goalCenter.getY() + Game.GOAL_WIDTH / 2);

                    this.jumpTo(new Translation2d(this.position.getX(), impactY));
                    return;
                }
            }
        }

        if (ballDistanceToGoal < 20 && team.getOpponent().hasBall() &&
                team.getPlayers().stream().noneMatch(p -> p.getPosition().getDistance(this.ball.getPosition2d()) < ballDistanceToGoal)) {
            this.moveTowards(predictedBall, 1); // charge the ball
            return;
        }

        if (!team.getOpponent().hasBall() && strategy.getBallChaser().equals(this) &&
                team.getOpponent().getClosestPlayerToBall().getPosition().getDistance(predictedBall) < ballDistanceToGoal) {
            this.moveTowards(predictedBall, 1);
            return;
        }

        // further ball → further GK
        double keeperDepth = MathUtil.clamp(ballDistanceToGoal * 0.2,
                3,
                team.hasBall() ? 20 : 10);
        Translation2d aimPoint = predictedBall.minus(goalCenter).normalized();
        this.moveTowards(goalCenter.plus(aimPoint.times(keeperDepth)), 0.7);
    }
}

