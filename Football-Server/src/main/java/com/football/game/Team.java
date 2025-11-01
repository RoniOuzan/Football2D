package com.football.game;

import com.football.Game;
import com.football.client.Client;
import com.football.game.players.*;
import com.football.util.math.geometry.Translation2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class Team implements Element {

    private static final double TAKE_BALL_THRESHOLD = 1;

    private transient final Client client;
    private transient final Game game;

    private final List<Player> players;

    private transient Player chosenPlayer = null;
    private int chosenPlayerIndex = -1; // for json

    public Team(Client client, Game game) {
        this.client = client;
        this.game = game;

        this.players = new ArrayList<>();

        initialize();
    }

    private void initialize() {
        int multiplier = 1;
        this.players.add(new Goalkeeper(this, new Translation2d(Game.MAX_X * 0.9, 0).times(multiplier)));

        for (int i = 1; i <= 4; i++) {
            this.players.add(new Defender(this, getPosition(Game.MAX_X * 2 / 3, i, 4).times(multiplier)));
        }

        for (int i = 1; i <= 3; i++) {
            this.players.add(new Midfielder(this, getPosition(Game.MAX_X * 2.1 / 5, i, 3).times(multiplier)));
        }

        for (int i = 1; i <= 3; i++) {
            this.players.add(new Attacker(this, getPosition(Game.MAX_X * 1 / 5, i, 3).times(multiplier)));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    private Player choosePlayer() {
        Ball ball = this.game.getBall();
        if (ball.getCarrier() != null) {
            this.chosenPlayerIndex = this.players.indexOf(ball.getCarrier());
            return ball.getCarrier();
        }

        if (this.chosenPlayer == null || this.client.getInput().isPressed("q")) {
            Player closest = getClosestPlayerToBall(p -> !p.equals(this.chosenPlayer));

            this.chosenPlayerIndex = this.players.indexOf(closest);
            return closest;
        }

        this.chosenPlayerIndex = this.players.indexOf(this.chosenPlayer);
        return this.chosenPlayer;
    }

    private Player getClosestPlayerToBall(Predicate<Player> filter) {
        return this.players.stream()
                .filter(filter)
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.game.getBall().getPosition())))
                .orElse(null);
    }


    @Override
    public void update() {
        this.chosenPlayer = choosePlayer();

        this.chosenPlayer.handleControlledMovement(this.client.getInput());
        for (Player player : this.players) {
            if (!player.equals(this.chosenPlayer)) {
                player.handleMovement(this);
            }
        }

        if (this.game.getBall().getCarrier() == null) {
            Player closest = getClosestPlayerToBall(p -> true);

            if (closest.getPosition().getDistance(this.game.getBall().getPosition()) <= TAKE_BALL_THRESHOLD) {
                this.game.getBall().setCarrier(closest);
            }
        }

        for (Player player : this.players) {
            player.update();
        }
    }

    private static Translation2d getPosition(double x, int index, int rowLength) {
        return new Translation2d(x, (index * (Game.WIDTH / (rowLength + 1))) - Game.MAX_Y);
    }
}
