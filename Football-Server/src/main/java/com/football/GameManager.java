package com.football;

import com.football.client.Client;
import com.football.game.Game;
import com.football.game.Team;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private static GameManager instance = null;

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    private transient Client client1 = null;
    private transient Client client2 = null;

    private transient Game game = null;

    public GameManager() {
    }

    public void addClient(Client client) {
        this.client1 = client;
        this.client2 = client;
//        if (this.client1 == null) {
//            this.client1 = client;
//        } else if (this.client2 == null) {
//            this.client2 = client;
//        }
    }

    public void removeClient(Client client) {
        if (client.equals(this.client1)) {
            this.client1 = null;
        } else if (client.equals(this.client2)) {
            this.client2 = null;
        }

        this.game = null;
    }

    public void update() {
        if (this.client1 != null && this.game == null && this.client1.getInputs().size() >= 1) {
            this.game = new Game(this.client1, 0, this.client2, 0);
            this.game.start();
        }

        if (this.game == null) return;

//        this.client1.update();
//        this.client2.update();

        this.game.update();
    }

    public String getJson() {
        if (this.game == null) {
            return "";
        }
        return this.game.toJson();
    }
}
