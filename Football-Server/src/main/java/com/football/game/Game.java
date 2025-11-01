package com.football.game;

import com.football.client.Client;
import com.football.util.json.JsonUtil;

public class Game {

    public static final double LENGTH = 100;
    public static final double MAX_X = LENGTH / 2;
    public static final double WIDTH = 64;
    public static final double MAX_Y = WIDTH / 2;

    private final Ball ball;
    private Client client1 = null;
    private Client client2 = null;

    public Game() {
        this.ball = new Ball();
    }

    public Ball getBall() {
        return this.ball;
    }

    public void setClient1(Client client1) {
        this.client1 = client1;
        this.client1.setTeam(new Team(client1, this));
    }

    public void setClient2(Client client2) {
        this.client2 = client2;
        this.client2.setTeam(new Team(client2, this));
    }

    public void addClient(Client client) {
        if (this.client1 == null) {
            setClient1(client);
        } else if (this.client2 == null) {
            setClient2(client);
        }
    }

    public void removeClient(Client client) {
        if (client.equals(this.client1)) {
            this.client1 = null;
        } else if (client.equals(this.client2)) {
            this.client2 = null;
        }
    }

    public Client getClient1() {
        return client1;
    }

    public Client getClient2() {
        return client2;
    }

    public void update() {
        if (this.client1 != null) this.client1.update();
        if (this.client2 != null) this.client2.update();

        this.ball.update();
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }
}
