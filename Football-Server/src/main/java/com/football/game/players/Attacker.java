package com.football.game.players;

import com.football.game.Ball;
import com.football.game.BotResult;
import com.football.game.Game;
import com.football.game.Team;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

public class Attacker extends Player {

    private static final double ATTACK_SPEED = 8;
    private static final double DEFEND_SPEED = 5;
    private static final double COUNTER_ATTACK_SPEED = 10;
    private static final double POSSESSION_SPEED = 6;

    public Attacker(Team team, Ball ball, Translation2d position) {
        super(team, ball, position);
    }

    @Override
    public BotResult attacking() {
        // Move toward the opponent penalty area, adjust vertically for spread
        Translation2d goal = Game.OPPONENT_GOAL;
        double side = team.getSideMultiplier();

        // Target area near the penalty box (a few meters outside)
        Translation2d target = new Translation2d(
                goal.getX() - (8 * side), // stop ~8m outside box
                goal.getY() + (this.hashCode() % 3 - 1) * 3.0 // small Y offset per attacker
        );

        // If the ball is near, try to position yourself for a pass
        if (ball.getPosition().getDistance(this.getPosition()) < 15) {
            // Move slightly into open space
            double offsetY = MathUtil.random(-3, 3);
            target = target.plus(new Translation2d(0, offsetY));
        }

       return new BotResult(target, ATTACK_SPEED);
    }

    @Override
    public BotResult defending() {
        Translation2d target = new Translation2d(
                team.getOwnGoalPosition().getX() + 40,
                ((this.hashCode() % 5) - 2) * 8 + this.originalPosition.getY() // spread vertically
        );

       return new BotResult(target, DEFEND_SPEED);
    }

    @Override
    public BotResult counterAttack() {
        // Sprint forward toward opponent goal
        Translation2d goal = Game.OPPONENT_GOAL;
        Translation2d target = new Translation2d(
                goal.getX() - 15,
                goal.getY() + (this.hashCode() % 3 - 1) * 6.0
        );

       return new BotResult(target, COUNTER_ATTACK_SPEED);
    }

    @Override
    public BotResult possession() {
        // Gradually move forward, find open space for a potential pass
        Translation2d goal = team.getOpponent().getOwnGoalPosition();
        double side = team.getSideMultiplier();

        Translation2d target = new Translation2d(
                goal.getX() - 20, // a bit behind attacking position
                this.getPosition().getY() + (Math.random() - 0.5) * 4.0 // small drift to open space
        );

        // If ball is far back, don’t go too deep
        if ((side > 0 && ball.getPosition().getX() < Game.MAX_X * 0.6)
                || (side < 0 && ball.getPosition().getX() > -Game.MAX_X * 0.6)) {
            target = target.plus(new Translation2d(-10 * side, 0));
        }

        return new BotResult(target, POSSESSION_SPEED);
    }
}
