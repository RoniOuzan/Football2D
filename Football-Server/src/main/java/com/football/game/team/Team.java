package com.football.game.team;

import com.football.client.Client;
import com.football.game.Formation;
import com.football.game.Game;
import com.football.game.players.*;
import com.football.game.strategy.TeamStrategy;
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

    public Team(Game game, Client client, boolean isTeam1, int inputSlot) {
        this.client = client;
        this.game = game;

        this.sideMultiplier = isTeam1 ? 1 : -1;

        this.players = new ArrayList<>();
        this.formation = Formation.FOUR_THREE_THREE;
        this.teamStrategy = new TeamStrategy(game, this, inputSlot);

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

    public TeamStrategy getTeamStrategy() {
        return this.teamStrategy;
    }

    public int getSideMultiplier() {
        return sideMultiplier;
    }

    public Player getClosestPlayerToBall(Predicate<Player> filter) {
        return this.players.stream()
                .filter(filter)
                .min(Comparator.comparingDouble(p -> this.game.getBall().getPredictedPosition(1).getDistance(p.getPosition())))
                .orElse(null);
    }

    public Player getClosestPlayerToBall() {
        return this.getClosestPlayerToBall(p -> true);
    }

    public boolean hasBall() {
        return this.players.contains(this.game.getBall().getCarrier());
    }

    public void resetPlayers() {
        for (Player player : this.players) {
            player.resetPosition();
        }
    }

    public Client getClient() {
        return this.client;
    }

    public void update(Game.State state) {
        this.teamStrategy.update(state);

        if (state != Game.State.PLAYING) return;
        for (Player player : this.players) {
            player.update(this);
        }
    }

    public Translation2d getOwnGoalPosition() {
        return new Translation2d(-Game.MAX_X, 0).times(this.sideMultiplier);
    }

    public Team getOpponent() {
        return this.sideMultiplier == 1 ? this.game.getTeam2() : this.game.getTeam1();
    }
}
