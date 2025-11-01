package com.football.game;

import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

import java.util.List;

public class StrategyChooser {

    private enum Strategy {
        ATTACKING, DEFENDING, COUNTER_ATTACKING, POSSESSION, CHASE_BALL
    }

    private Strategy strategy = Strategy.CHASE_BALL;

    private final Team team;
    private final Ball ball;
    private final List<Player> players;

    private final FieldHelper fieldHelper;

    private long lastTimeChanged = 0;

    public StrategyChooser(Team team, Ball ball) {
        this.team = team;
        this.ball = ball;
        this.players = team.getPlayers();

        this.fieldHelper = new FieldHelper(this.team.getSideMultiplier());
    }

    private Strategy chooseStrategy() {
        if (this.ball.getCarrier() == null) {
            return Strategy.CHASE_BALL;
        }

        if (System.currentTimeMillis() - this.lastTimeChanged < 500) {
            return this.strategy;
        }
        this.lastTimeChanged = System.currentTimeMillis();

        if (!this.team.getOpponent().isBallInThisTeam() && this.strategy != Strategy.COUNTER_ATTACKING) {
            if (this.fieldHelper.isBetween(this.ball.getPosition().getX(), 2.0 / 3.0, 1)) {
                return Strategy.ATTACKING;
            }
            if (this.fieldHelper.isBetween(this.ball.getPosition().getX(), 1.0 / 3.0, 2.0 / 3.0)) {
                return Strategy.POSSESSION;
            }

            return Strategy.COUNTER_ATTACKING;
        }

        return Strategy.DEFENDING;
    }

    public void update() {
        this.strategy = this.chooseStrategy();

        for (Player player : this.players) {
            if (!player.equals(this.team.getChosenPlayer())) {
                handlePlayer(player);
            }
        }
    }

    private void handlePlayer(Player player) {
        if (this.team.getOpponent() == null) {
            return;
        }

        BotResult botResult = switch (this.strategy) {
            case ATTACKING -> player.attacking();
            case DEFENDING -> player.defending();
            case COUNTER_ATTACKING -> player.counterAttack();
            case POSSESSION -> player.possession();
            case CHASE_BALL -> player.chaseBall();
        };

        if (botResult == null) {
            player.setVelocity(new Translation2d());
            return;
        }

        player.moveTo(botResult);
    }
}
