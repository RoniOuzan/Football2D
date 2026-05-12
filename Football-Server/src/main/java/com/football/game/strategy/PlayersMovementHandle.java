package com.football.game.strategy;

import com.football.client.inputs.InputHandler;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Goalkeeper;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

public class PlayersMovementHandle {

    private static final double BALL_CHASE_LATENCY = 0.3;
    private static final double BALL_CHASE_SPEED_FACTOR = 0.8;
    private static final double JUST_PASSED_SPEED_FACTOR = 1.0;
    private static final double TEAMMATE_POSITIONING_SPEED_FACTOR = 1.0;

    private final TeamStrategy teamStrategy;
    private final PlayerTargetPosition playerTargetPosition;

    private boolean justPassed = false;
    @Getter @Setter
    private Player chosenPlayer = null;

    public PlayersMovementHandle(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
        this.playerTargetPosition = new PlayerTargetPosition(teamStrategy);
    }

    public void update() {
        this.playerTargetPosition.setBallChaser(chooseBallChaser());
        this.chosenPlayer = choosePlayer();

        this.handleControlledMovement(this.chosenPlayer, this.teamStrategy.getInput());
        for (Player player : this.teamStrategy.players) {
            if (!player.equals(this.chosenPlayer)) {
                if (player instanceof Goalkeeper gk) {
                    gk.handleTarget(this.teamStrategy);
                    continue;
                }

                player.moveTowards(this.getPlayerTargetPosition(player), TEAMMATE_POSITIONING_SPEED_FACTOR);
            }
        }
    }

    public void handleControlledMovement(Player player, InputHandler input) {
        if (player.hasBall()) {
            player.updateHasBall(getTargetVelocity(player, input));
        } else {
            player.setTargetVelocity(getTargetVelocity(player, input));
        }

        this.teamStrategy.getInput().runInputs(this.teamStrategy, player);
    }

    private Translation2d getTargetVelocity(Player player, InputHandler input) {
        double velocity = input.isHolding(Keybind.SPRINT) ? Player.SPRINT_VELOCITY : Player.WALK_VELOCITY;

        if ((player.equals(this.playerTargetPosition.getBallChaser()) || this.justPassed) && this.teamStrategy.ball.getCarrier() == null) {
            return player.getVelocityToPosition(this.teamStrategy.ball.getPredictedPosition(BALL_CHASE_LATENCY).toTranslation2d(),
                    this.justPassed ? JUST_PASSED_SPEED_FACTOR : BALL_CHASE_SPEED_FACTOR);
        }

        this.justPassed = false;
        return this.teamStrategy.getRequestedVelocity().times(velocity);
    }

    private Player choosePlayer() {
        if (this.teamStrategy.team.hasBall()) {
            return this.teamStrategy.ball.getCarrier();
        }

        Player switchPlayer = this.teamStrategy.getInput().getPlayerToSwitchTo(this.teamStrategy);
        if (switchPlayer != null) {
            return switchPlayer;
        }  else if (this.chosenPlayer == null || this.chosenPlayer instanceof Goalkeeper) {
            return this.teamStrategy.getDefaultPlayerToSwitchTo();
        }

        return this.chosenPlayer;
    }

    private Player chooseBallChaser() {
        return this.teamStrategy.players.stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.teamStrategy.ball.getPosition(BALL_CHASE_LATENCY).toTranslation2d())))
                .orElse(null);
    }

    public Player getBallChaser() {
        return this.playerTargetPosition.getBallChaser();
    }

    public void playerPassedTo(Player player) {
        this.chosenPlayer = player;
        this.justPassed = true;
    }

    public Translation2d getPlayerTargetPosition(Player player) {
        return this.playerTargetPosition.getTargetPosition(player);
    }
}
