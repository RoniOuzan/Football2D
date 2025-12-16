package com.football.game;

import com.football.client.Client;
import com.football.util.json.JsonUtil;
import com.football.util.math.geometry.Translation2d;

import java.util.Arrays;
import java.util.List;

public class Game {

    public enum State {
        PLAYING,
        GOAL,
    }

    public static final double LENGTH = 100;
    public static final double MAX_X = LENGTH / 2;
    public static final double WIDTH = 64;
    public static final double MAX_Y = WIDTH / 2;

    public static final double GOAL_WIDTH = 7.3;
    public static final double POST_RADIUS = 0.35;
    public static final List<Translation2d> POSTS = Arrays.asList(
            new Translation2d(-Game.MAX_X,  Game.GOAL_WIDTH / 2),
            new Translation2d(-Game.MAX_X, -Game.GOAL_WIDTH / 2),
            new Translation2d(Game.MAX_X,  Game.GOAL_WIDTH / 2),
            new Translation2d(Game.MAX_X, -Game.GOAL_WIDTH / 2)
    );
    public static final double GOAL_DEPTH = 2.3;

    public static final int TEAM_1 = 1;
    public static final int TEAM_2 = -1;

    private final Team team1;
    private final Team team2;

    private final Ball ball;

    private int score1 = -1;
    private int score2 = -1;

    private State state;
    private long stateChanged;
    private long startTime = -1;

    public Game(Client client1, int inputSlot1, Client client2, int inputSlot2) {
        this.ball = new Ball(this);

        this.team1 = new Team(this, client1, true, inputSlot1);
        this.team2 = new Team(this, client2, false, inputSlot2);
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.stateChanged = System.currentTimeMillis();
        this.state = State.PLAYING;
        this.score1 = 0;
        this.score2 = 0;
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
    public int getScore1() {
        return score1;
    }
    public int getScore2() {
        return score2;
    }

    public double getMatchTime() {
        return (System.currentTimeMillis() - this.startTime) / 1000.0;
    }

    public void resetField() {
        this.ball.reset();
        this.team1.resetPlayers();
        this.team2.resetPlayers();
    }

    public void update() {
        this.team1.update();
        this.team2.update();

        this.ball.update(this.team1, this.team2);

        switch (this.state) {
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
            case GOAL -> {
                if (getLastTimeChanged() >= 3) {
                    this.resetField();
                    this.setState(State.PLAYING);
                }
            }
        }
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
        this.stateChanged = System.currentTimeMillis();
    }

    private double getLastTimeChanged() {
        return (System.currentTimeMillis() - this.stateChanged) / 1000.0;
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
