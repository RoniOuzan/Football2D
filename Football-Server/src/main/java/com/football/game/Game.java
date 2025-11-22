package com.football.game;

import com.football.client.Client;
    import com.football.util.json.JsonUtil;

public class Game {

    public static final double LENGTH = 100;
    public static final double MAX_X = LENGTH / 2;
    public static final double WIDTH = 64;
    public static final double MAX_Y = WIDTH / 2;

    public static final double GOAL_WIDTH = 7.3;

    public static final int TEAM_1 = 1;
    public static final int TEAM_2 = -1;
    public static final int NULL_TEAM = 0;

    private final Team team1;
    private final Team team2;

    private final Ball ball;

    private int score1 = -1;
    private int score2 = -1;

    private long startTime = -1;

    public Game(Client client1, Client client2) {
        this.ball = new Ball(this);

        this.team1 = new Team(this, client1, true);
        this.team2 = new Team(this, client2, false);
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
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

    public double getMatchTime() {
        return (System.currentTimeMillis() - this.startTime) / 1000.0;
    }

    public void resetField() {
        this.ball.reset();
        this.team1.resetPlayers();
        this.team2.resetPlayers();
    }

    public void update() {
        this.ball.update(this.team1, this.team2);

        this.team1.update();
        this.team2.update();
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
