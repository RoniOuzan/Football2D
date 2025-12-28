package com.football.game;

import com.football.client.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaitingGame implements Joinable {
    private final UUID uuid;
    private final List<Client> clients;

    public WaitingGame(Client client) {
        this.uuid = UUID.randomUUID();

        this.clients = new ArrayList<>();
        this.clients.add(client);
    }

    public UUID getUUID() {
        return uuid;
    }

    public boolean isReadyForGame() {
        return this.clients.size() >= 2;
    }

    public Game getGame() {
        if (!isReadyForGame()) return null;
        return new Game(this.clients.get(0), 0, this.clients.get(1), 1);
    }
}
