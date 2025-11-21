package com.football.game;

import com.football.client.Client;
import com.football.game.players.*;
import com.football.util.math.MathUtil;
import com.football.util.math.geometry.Translation2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class Team {

    private transient final Client client;
    private transient final Game game;

    private final List<Player> players;
    private transient final Formation formation;
    private final TeamStrategy teamStrategy;

    private transient final int sideMultiplier;

    public Team(Client client, Game game) {
        this.client = client;
        this.game = game;

        this.sideMultiplier = this.client.equals(this.game.getClient1()) ? 1 : -1;

        this.players = new ArrayList<>();
        this.formation = Formation.FOUR_THREE_THREE;
        this.teamStrategy = new TeamStrategy(game, this);

        initialize();
    }

    private void initialize() {
        this.players.add(new Goalkeeper(this, this.game.getBall(), this.formation.getGoalkeeper().times(sideMultiplier)));

        for (Translation2d position : this.formation.getDefenders()) {
            this.players.add(new Defender(this, this.game.getBall(),position.times(sideMultiplier)));
        }
        for (Translation2d position : this.formation.getMidfielders()) {
            this.players.add(new Midfielder(this, this.game.getBall(),position.times(sideMultiplier)));
        }
        for (Translation2d position : this.formation.getAttackers()) {
            this.players.add(new Attacker(this, this.game.getBall(),position.times(sideMultiplier)));
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getSideMultiplier() {
        return sideMultiplier;
    }

    public Player getClosestPlayerToBall(Predicate<Player> filter) {
        return this.players.stream()
                .filter(filter)
                .min(Comparator.comparingDouble(p -> p.getPosition().getDistance(this.game.getBall().getPosition())))
                .orElse(null);
    }

    public Player getClosestPlayerToBall() {
        return this.getClosestPlayerToBall(p -> true);
    }

    public boolean hasBall() {
        return this.players.contains(this.game.getBall().getCarrier());
    }

    public Client getClient() {
        return client;
    }

    public void update() {
        this.teamStrategy.update();

        if (!this.hasBall()) {
            Player closest = getClosestPlayerToBall();

            if (this.game.getBall().shouldBePickedUpBy(closest)) {
                this.game.getBall().setCarrier(closest);
            }
        }

        for (Player player : this.players) {
            player.update();
        }
    }

    public Translation2d getOwnGoalPosition() {
        return new Translation2d(-Game.MAX_X, 0).times(this.sideMultiplier);
    }

    public Team getOpponent() {
        if (this.client.equals(this.game.getClient1())) {
            if (this.game.getClient2() == null) {
                return null;
            }
            return this.game.getClient2().getTeam();
        } else if (this.client.equals(this.game.getClient2())) {
            if (this.game.getClient1() == null) {
                return null;
            }
            return this.game.getClient1().getTeam();
        }
        return null;
    }
}
