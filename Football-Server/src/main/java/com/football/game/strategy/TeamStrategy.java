package com.football.game.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.football.client.inputs.InputHandler;
import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.players.Goalkeeper;
import com.football.game.players.Player;
import com.football.game.Team;
import com.football.game.strategy.camera.CameraManager;
import com.football.util.math.geometry.Translation2d;
import lombok.Getter;
import lombok.Setter;

/**
 * Orchestrates the tactical behavior and data processing for a specific team.
 * <p>
 * This class acts as a coordinator, managing specialized calculators for offside lines, 
 * defensive positioning, heatmaps, and player movement handles.
 */
public class TeamStrategy {

    @Getter
    protected transient final Team team;
    protected transient final int inputSlot;
    protected transient final List<Player> players;
    @Getter
    protected transient final Ball ball;
    @Setter
    protected transient double sideMultiplier;

    @SuppressWarnings(value = {"unused", "FieldCanBeLocal"})
    private int chosenPlayerIndex = -1; // for JSON

    protected final Map<Translation2d, Double> scores;

    private transient final HeatmapGenerator heatmapGenerator;
    private transient final OffsideCalculator offsideCalculator;
    private transient final DefensiveLineCalculator defensiveLineCalculator;
    @Getter
    private final CameraManager cameraManager;
    private transient final PlayersMovementHandle playersMovementHandle;

    /**
     * Initializes a new team strategy coordinator.
     *
     * @param game      The current game instance.
     * @param team      The team this strategy belongs to.
     * @param inputSlot The input slot index associated with this team's controller.
     */
    public TeamStrategy(Game game, Team team, int inputSlot) {
        this.team = team;
        this.inputSlot = inputSlot;
        this.players = team.getPlayers();
        this.ball = game.getBall();
        this.sideMultiplier = team.getSideMultiplier();

        this.scores = new HashMap<>();

        this.heatmapGenerator = new HeatmapGenerator(this);
        this.offsideCalculator = new OffsideCalculator(this);
        this.defensiveLineCalculator = new DefensiveLineCalculator(this);
        this.cameraManager = new CameraManager(this);
        this.playersMovementHandle = new PlayersMovementHandle(this);
    }

    /**
     * Performs the per-tick update logic for all tactical subsystems.
     * <p>
     * Updates camera positioning, calculates defensive and offside boundaries, 
     * refreshes tactical heatmaps, and processes player movement logic.
     *
     * @param state The current state of the game (e.g., PLAYING, KICKOFF).
     */
    public void update(Game.State state) {
        this.cameraManager.update();
        if (state != Game.State.PLAYING) return;

        // Calculation
        this.defensiveLineCalculator.update();
        this.offsideCalculator.update();

        // Preparing the heatmap
        this.heatmapGenerator.update(this.scores);

        this.playersMovementHandle.update();
        this.chosenPlayerIndex = this.players.indexOf(this.playersMovementHandle.getChosenPlayer());
    }

    /**
     * Retrieves the input handler (Human or Bot) associated with this team.
     *
     * @return The {@link InputHandler} for the assigned input slot.
     */
    public InputHandler getInput() {
        return this.team.getClient().getInput(this.inputSlot);
    }

    /**
     * Gets the current vertical Y-coordinate (or X in 2D space) representing the defensive line.
     */
    public double getDefenseLine() {
        return this.defensiveLineCalculator.getDefenseLine();
    }

    /**
     * Gets the current offside line coordinate based on the opponent's last defender.
     */
    public double getOffsideLine() {
        return this.offsideCalculator.getOffsideLine();
    }

    /**
     * Returns the player currently designated as the "ball chaser" by the movement handler.
     */
    public Player getBallChaser() {
        return this.playersMovementHandle.getBallChaser();
    }

    /**
     * Notifies the movement handler that a pass has been initiated toward a specific player.
     *
     * @param player The intended receiver of the pass.
     */
    public void playerPassedTo(Player player) {
        this.playersMovementHandle.playerPassedTo(player);
    }

    /**
     * Gets the player currently under active control (highlighted by the user/bot cursor).
     */
    public Player getChosenPlayer() {
        return this.playersMovementHandle.getChosenPlayer();
    }

    /**
     * Sets the player to be actively controlled.
     *
     * @param player The player to switch control to.
     */
    public void setChosenPlayer(Player player) {
        this.playersMovementHandle.setChosenPlayer(player);
    }

    /**
     * Suggests the best candidate for a player switch.
     * <p>
     * Usually selects the player closest to the ball, excluding the currently 
     * controlled player and the goalkeeper.
     *
     * @return The {@link Player} recommended for the next control switch.
     */
    public Player getDefaultPlayerToSwitchTo() {
        return this.team.getClosestPlayerToBall(p -> !p.equals(this.getChosenPlayer()) && !(p instanceof Goalkeeper));
    }

    /**
     * Translates the raw input movement vector into a camera-oriented velocity vector.
     *
     * @return A {@link Translation2d} representing the directional intent of the controller.
     */
    public Translation2d getRequestedVelocity() {
        return this.cameraManager.getOrientedTranslation(this.getInput().getRequestedVelocity());
    }
}
