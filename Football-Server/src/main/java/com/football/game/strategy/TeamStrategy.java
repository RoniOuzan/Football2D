package com.football.game.strategy;

import com.football.client.inputs.InputHandler;
import com.football.game.Ball;
import com.football.game.Game;
import com.football.game.team.Team;
import com.football.game.players.Player;
import com.football.util.math.geometry.Rotation2d;
import com.football.util.math.geometry.Translation2d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamStrategy {

    protected transient final Team team;
    protected transient final int inputSlot;
    protected transient final List<Player> players;
    protected transient final Ball ball;
    protected transient final double sideMultiplier;

    @SuppressWarnings(value = {"unused", "FieldCanBeLocal"})
    private int chosenPlayerIndex = -1; // for json

    protected final Map<Translation2d, Double> scores;

    private transient final HeatmapGenerator heatmapGenerator;
    private transient final OffsideCalculator offsideCalculator;
    private transient final DefensiveLineCalculator defensiveLineCalculator;
    private final CameraManager cameraManager;
    private transient final PlayersMovementHandle playersMovementHandle;

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
     * Precompute the baseline (player-agnostic) heatmap.
     */
    public void update() {
        this.defensiveLineCalculator.update();
        this.offsideCalculator.update();
        this.heatmapGenerator.update();

        this.cameraManager.update();
        this.playersMovementHandle.update();
        this.chosenPlayerIndex = this.players.indexOf(this.playersMovementHandle.getChosenPlayer());
    }

    public InputHandler getInput() {
        return this.team.getClient().getInput(this.inputSlot);
    }

    public double getDefenseLine() {
        return this.defensiveLineCalculator.getDefenseLine();
    }

    public double getOffsideLine() {
        return this.offsideCalculator.getOffsideLine();
    }

    public Team getTeam() {
        return this.team;
    }

    public Player getBallChaser() {
        return this.playersMovementHandle.getBallChaser();
    }

    public void playerPassedTo(Player player) {
        this.playersMovementHandle.playerPassedTo(player);
    }

    public Player getChosenPlayer() {
        return this.playersMovementHandle.getChosenPlayer();
    }

    public CameraManager getCameraManager() {
        return this.cameraManager;
    }

    public Translation2d getRequestedVelocity() {
        Rotation2d orientation = this.cameraManager.getPosition().getTranslation().toTranslation2d()
                .minus(this.getChosenPlayer().getPosition()).getAngle().plus(Rotation2d.kCCW_Pi_2);
        return getInput().getRequestedVelocity().rotateBy(orientation);
    }
}
