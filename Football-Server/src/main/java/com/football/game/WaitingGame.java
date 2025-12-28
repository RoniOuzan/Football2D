package com.football.game;

import com.football.client.Client;

import java.util.ArrayList;
import java.util.List;

public class WaitingGame {
    private final List<Client> clients;

    public WaitingGame(Client client) {
        this.clients = new ArrayList<>();
        this.clients.add(client);
    }

    public boolean isReadyForGame() {
        return this.clients.size() >= 2;
    }

    public Game getGame() {
        if (!isReadyForGame()) return null;
        return new Game(this.clients.get(0), 0, this.clients.get(1), 1);
    }
}
