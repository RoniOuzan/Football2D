package com.football.game.strategy;

import com.football.client.inputs.InputHandler;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Goalkeeper;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;

import java.util.Comparator;

public class PlayersMovementHandle {

    private static final double BALL_CHASE_LATENCY = 0.3;

    private final TeamStrategy teamStrategy;
    private final PlayerTargetPosition playerTargetPosition;

    private boolean justPassed = false;
    private Player chosenPlayer = null;

    public PlayersMovementHandle(TeamStrategy teamStrategy) {
        this.teamStrategy = teamStrategy;
        this.playerTargetPosition = new PlayerTargetPosition(teamStrategy);
    }

    public void update() {
        this.playerTargetPosition.setBallChaser(chooseBallChaser());
        this.chosenPlayer = choosePlayer();

        this.handleControlledMovement(this.chosenPlayer, this.teamStrategy.team.getClient().getInput(this.teamStrategy.inputSlot));
        for (Player player : this.teamStrategy.players) {
            if (!player.equals(this.chosenPlayer)) {
                if (player instanceof Goalkeeper gk) {
                    gk.handleTarget(this.teamStrategy);
                    continue;
                }

                player.moveTowards(this.playerTargetPosition.getTargetPosition(player), 1);
            }
        }
    }

    public void handleControlledMovement(Player player, InputHandler input) {
        player.setVelocity(getTargetVelocity(player, input));

        this.teamStrategy.getInput().runInputs(this.teamStrategy, player);
    }

    private Translation2d getTargetVelocity(Player player, InputHandler input) {
        double velocity = input.isHolding(Keybind.SPRINT) ? Player.SPRINT_VELOCITY : Player.WALK_VELOCITY;

        if ((player.equals(this.playerTargetPosition.getBallChaser()) || this.justPassed) && this.teamStrategy.ball.getCarrier() == null) {
            return player.getVelocityToPosition(this.teamStrategy.ball.getPredictedPosition(0.5),
                    this.justPassed ? 0.8 : 1);
        }

        this.justPassed = false;
        return input.getRequestedVelocity().times(velocity);
    }

    private Player choosePlayer() {
        if (this.teamStrategy.team.hasBall()) {
            return this.teamStrategy.ball.getCarrier();
        }

        if (this.chosenPlayer == null || this.chosenPlayer instanceof Goalkeeper ||
                this.teamStrategy.team.getClient().getInput(this.teamStrategy.inputSlot).isPressed(Keybind.SWITCH_PLAYER)) {
            return this.teamStrategy.team.getClosestPlayerToBall(p -> !p.equals(this.chosenPlayer) && !(p instanceof Goalkeeper));
        }

        return this.chosenPlayer;
    }

    private Player chooseBallChaser() {
        return this.teamStrategy.players.stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.teamStrategy.ball.getPosition(BALL_CHASE_LATENCY))))
                .orElse(null);
    }

    public Player getBallChaser() {
        return this.playerTargetPosition.getBallChaser();
    }

    public Player getChosenPlayer() {
        return this.chosenPlayer;
    }

    public void playerPassedTo(Player player) {
        this.chosenPlayer = player;
        this.justPassed = true;
    }
}
