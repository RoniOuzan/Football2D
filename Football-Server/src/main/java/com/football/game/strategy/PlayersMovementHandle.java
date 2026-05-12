package com.football.game.strategy;

import com.football.client.inputs.InputHandler;
import com.football.client.keybinds.Keybind;
import com.football.game.players.Goalkeeper;
import com.football.game.players.Player;
import com.football.util.math.geometry.Translation2d;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

/**
 * Manages the movement logic for all players on a team.
 * <p>
 * This class handles the distinction between the "Chosen Player" (controlled by input)
 * and the rest of the team (controlled by AI positioning), as well as determining
 * which player should autonomously chase a loose ball.
 */
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

    /**
     * Main update loop for team movement. 
     * Updates the ball chaser, switches the controlled player if necessary, 
     * and applies movement to both the controlled player and AI teammates.
     */
    public void update() {
        this.playerTargetPosition.setBallChaser(chooseBallChaser());
        this.chosenPlayer = choosePlayer();

        // Move the chosen player
        this.handleControlledMovement(this.chosenPlayer, this.teamStrategy.getInput());

        // Move the rest of the players
        for (Player player : this.teamStrategy.players) {
            if (!player.equals(this.chosenPlayer)) {
                if (player instanceof Goalkeeper gk) {
                    gk.handleTarget(this.teamStrategy);
                    continue;
                }

                player.moveTowards(this.playerTargetPosition.getTargetPosition(player), TEAMMATE_POSITIONING_SPEED_FACTOR);
            }
        }
    }

    /**
     * Processes movement and button inputs for the player currently under direct control.
     * 
     * @param player The player to move.
     * @param input  The input handler (Human or Bot) providing directions.
     */
    public void handleControlledMovement(Player player, InputHandler input) {
        if (player.hasBall()) {
            player.updateHasBall(getTargetVelocity(player, input));
        } else {
            player.setTargetVelocity(getTargetVelocity(player, input));
        }

        // Updates the button's actions
        this.teamStrategy.getInput().runInputs(this.teamStrategy, player);
    }

    /**
     * Calculates the velocity vector for a controlled player.
     * If the player is the designated ball chaser and the ball is loose, they move 
     * automatically toward the ball. Otherwise, they follow the input's requested direction.
     */
    private Translation2d getTargetVelocity(Player player, InputHandler input) {
        double velocity = input.isHolding(Keybind.SPRINT) ? Player.SPRINT_VELOCITY : Player.WALK_VELOCITY;

        // Auto-chase logic: if ball is loose and this player is the designated chaser
        if ((player.equals(this.playerTargetPosition.getBallChaser()) || this.justPassed) && this.teamStrategy.ball.getCarrier() == null) {
            return player.getVelocityToPosition(this.teamStrategy.ball.getPredictedPosition(BALL_CHASE_LATENCY).toTranslation2d(),
                    this.justPassed ? JUST_PASSED_SPEED_FACTOR : BALL_CHASE_SPEED_FACTOR);
        }

        this.justPassed = false;
        return this.teamStrategy.getRequestedVelocity().times(velocity);
    }

    /**
     * Determines which player should be currently controlled.
     * Priority: Ball Carrier > Manual Switch Request > Default/Closest Player.
     * @return The player that should receive control inputs.
     */
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

    /**
     * Identifies the player closest to the ball's predicted position.
     * @return The player designated to intercept a loose ball.
     */
    private Player chooseBallChaser() {
        return this.teamStrategy.players.stream()
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.teamStrategy.ball.getPosition(BALL_CHASE_LATENCY).toTranslation2d())))
                .orElse(null);
    }

    public Player getBallChaser() {
        return this.playerTargetPosition.getBallChaser();
    }

    /**
     * Triggers a state change indicating a pass was just made to a specific player.
     * This forces the receiver to be the 'chosen' player and gives them a speed boost.
     */
    public void playerPassedTo(Player player) {
        this.chosenPlayer = player;
        this.justPassed = true;
    }
}
