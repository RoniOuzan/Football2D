package com.football.game;

import com.football.client.Client;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaitingGame implements Joinable {
    private final UUID uuid;
    @Getter
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
        if (this.clients.size() < 2)
            return false;

        // Same client
        if (this.clients.get(0).equals(this.clients.get(1))) {
            return this.clients.get(0).getAmountOfInputs() >= 2;
        }
        return true;
    }

    public boolean isEmpty() {
        return this.clients.isEmpty();
    }

    public Game getOnlineGame() {
        if (!isReadyForGame())
            return null;

        Client client1 = this.clients.get(0);
        Client client2 = this.clients.get(1);

        if (client1.equals(client2)) {
            if (client2.getAmountOfInputs() < 2) {
                System.out.println("Cant start a game with yourself without another device.");
                return null;
            }
            return new Game(client1, 0, client2, 1);
        }

        return new Game(client1, client2);
    }

    public void addClient(Client client) {
        this.clients.add(client);
    }

    public void removeClient(Client client) {
        this.clients.remove(client);
    }
}
