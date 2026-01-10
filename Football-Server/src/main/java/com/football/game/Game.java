package com.football.game;

import com.football.GameManager;
import com.football.client.Client;
import com.football.client.messages.AlertMessage;
import com.football.client.messages.CountdownMessage;
import com.football.client.messages.Message;
import com.football.game.team.Team;
import com.football.util.json.JsonUtil;
import com.football.util.math.geometry.Translation2d;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class Game implements Joinable {

    public enum State {
        WAIT,
        PLAYING,
        GOAL,
        STOP,
    }

    public enum Phase {
        FIRST_HALF(0),
        SECOND_HALF(45 * 60),
        EXTRA_TIME(90 * 60),
        FINISH(120 * 60);

        private final double startTime;

        Phase(double startTime) {
            this.startTime = startTime;
        }
    }

    public static final double WAIT_TIME = 3;

    public static final double LENGTH = 100;
    public static final double MAX_X = LENGTH / 2;
    public static final double WIDTH = 64;
    public static final double MAX_Y = WIDTH / 2;

    public static final double GOAL_WIDTH = 7.3;
    public static final double POST_RADIUS = 0.35;
    public static final double CROSSBAR_HEIGHT = 2.44;
    public static final List<Translation2d> POSTS = Arrays.asList(
            new Translation2d(-Game.MAX_X,  Game.GOAL_WIDTH / 2),
            new Translation2d(-Game.MAX_X, -Game.GOAL_WIDTH / 2),
            new Translation2d(Game.MAX_X,  Game.GOAL_WIDTH / 2),
            new Translation2d(Game.MAX_X, -Game.GOAL_WIDTH / 2)
    );
    public static final double GOAL_DEPTH = 2.3;

    public static final int TEAM_1 = 1;
    public static final int TEAM_2 = -1;

    private final UUID uuid;

    private final Team team1;
    private final Team team2;

    private final Ball ball;

    @SuppressWarnings("unused")
    private int score1 = -1;
    @SuppressWarnings("unused")
    private int score2 = -1;

    private long startTime = -1;

    private double matchTime = 0;
    private Phase phase;

    private State state;
    private transient long stateChanged;

    private final List<Spectator> spectators = new ArrayList<>();

    public Game(Client client1, int inputSlot1, Client client2, int inputSlot2) {
        this.uuid = UUID.randomUUID();

        this.ball = new Ball(this);

        this.team1 = new Team(this, client1, true, inputSlot1);
        this.team2 = new Team(this, client2, false, inputSlot2);
    }

    public Game(Client client1, Client client2) {
        this(client1, -1, client2, -1);
    }

    public void start() {
        this.startTime = System.currentTimeMillis();

        this.matchTime = 0;
        this.phase = Phase.FIRST_HALF;

        this.state = State.WAIT;
        this.stateChanged = System.currentTimeMillis();

        this.score1 = 0;
        this.score2 = 0;

        sendMessage(new CountdownMessage("Game starts in %d!", 3, 1));
    }

    public UUID getUUID() {
        return this.uuid;
    }
    public Ball getBall() {
        return this.ball;
    }
    public Team getTeam1() {
        return team1;
    }
    public Team getTeam2() {
        return team2;
    }
    public Phase getPhase() {
        return phase;
    }

    public void sendMessage(Message message) {
        this.getClients().forEach(c ->
                c.sendMessage(message)
        );
    }

    public double getRealTime() {
        return (System.currentTimeMillis() - this.startTime) / 1000.0;
    }

    private void updateMatchTime() {
        if (this.phase == Phase.FINISH) {
            return;
        }

        if (this.state != State.WAIT) {
            this.matchTime += GameManager.PERIOD / GameManager.GAME_REAL_TIME * GameManager.GAME_TIME;
        }

        Phase nextPhase = Phase.values()[this.phase.ordinal() + 1];
        if (this.matchTime >= nextPhase.startTime) {
            if (this.score1 == this.score2) { // draw
                this.phase = nextPhase;
            } else {
                this.phase = Phase.FINISH;
            }

            if (this.phase == Phase.FINISH) {
                AlertMessage message;
                if (this.score1 == this.score2) { // draw
                    message = new AlertMessage("DRAW!", "Score is tied!", "yellow", 1, "medium");
                } else if (this.score1 > this.score2) { // red
                    message = new AlertMessage("RED WON!", "Game Finished!!", "red", 1, "medium");
                } else { // blue
                    message = new AlertMessage("BLUE WON!", "Game Finished!!", "blue", 1, "medium");
                }
                sendMessage(message);
                this.setState(State.STOP);
                return;
            }

            if (this.phase == Phase.EXTRA_TIME) {
                this.sendMessage(new AlertMessage("Extra Time!", "yellow", 1, "large"));
            }
            this.resetField();
            this.setState(State.WAIT);
        }
    }

    public void resetField() {
        this.ball.reset();
        this.team1.resetPlayers();
        this.team2.resetPlayers();
    }

    public void update() {
        this.team1.update(this.state);
        this.team2.update(this.state);

        this.ball.update(this.team1, this.team2);

        switch (this.state) {
            case WAIT, GOAL -> {
                if (getLastTimeChanged() >= WAIT_TIME) {
                    this.resetField();
                    this.setState(State.PLAYING);
                }
            }
            case PLAYING -> {
                int isGoal = this.ball.isAtGoal();
                if (isGoal == TEAM_1) {
                    this.score1++;
                    setState(State.GOAL);
                } else if (isGoal == TEAM_2) {
                    this.score2++;
                    setState(State.GOAL);
                }
            }
            case STOP -> {
            }
        }

        this.spectators.forEach(Spectator::update);

        this.updateMatchTime();
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
        this.stateChanged = System.currentTimeMillis();

        if (this.state == State.WAIT) {
            this.getClients().forEach(c ->
                    c.sendMessage(new CountdownMessage("Game continues in %d!", 3, 1))
            );
        }
    }

    private double getLastTimeChanged() {
        return (System.currentTimeMillis() - this.stateChanged) / 1000.0;
    }

    public List<Client> getClients() {
        return Arrays.asList(this.team1.getClient(), this.team2.getClient());
    }

    public void addSpectator(Client client) {
        this.spectators.add(new Spectator(client, this.team1.getTeamStrategy())); // team1 for now
    }

    public void removeSpectator(Client client) {
        this.spectators.removeIf(s -> s.getClient().equals(client));
    }

    public List<Spectator> getSpectators() {
        return this.spectators;
    }

    public JsonObject toJson(Client client) {
        JsonObject json = JsonUtil.toJsonObject(this);

        json.addProperty("client", getClientNumber(client));

        return json;
    }

    private int getClientNumber(Client client) {
        if (this.team1.getClient().equals(client))
            return 1;
        else if (this.team2.getClient().equals(client))
            return 2;

        int index = IntStream.range(0, this.spectators.size())
                .filter(i -> this.spectators.get(i).getClient().equals(client))
                .findFirst()
                .orElse(-1);

        if (index == -1) {
            return 3;
        }
        return -index;
    }
}
