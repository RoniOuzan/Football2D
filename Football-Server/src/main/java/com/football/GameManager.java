package com.football;

import com.football.client.Client;
import com.football.client.messages.AlertMessage;
import com.football.client.messages.Message;
import com.football.game.Game;
import com.football.game.Joinable;
import com.football.game.NullGame;
import com.football.game.WaitingGame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private static GameManager instance = null;

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public static final double FPS = 30;
    public static final double PERIOD = 1 / FPS;

    private transient final Map<Client, Joinable> clients = new ConcurrentHashMap<>();

    private final List<Game> games = new ArrayList<>();
    private final List<WaitingGame> waitingGames = new ArrayList<>();

    public GameManager() {
    }

    public void addClient(Client client) {
        this.clients.put(client, new NullGame());
    }

    public void removeClient(Client client) {
        Joinable game = this.clients.remove(client);

        if (game instanceof WaitingGame g) {
            g.removeClient(client);

            if (g.isEmpty()) {
                this.waitingGames.remove(g);
            }
        } else if (game instanceof Game g) {
            if (g.getClients().contains(client)) {
                g.getClients().stream().filter(c -> !c.equals(client)).forEach(c -> {
                    this.clients.put(c, new NullGame());
                    c.sendMessage(new AlertMessage("OOPS! one client quit!"));
                });
                this.games.remove(g);
            }
        }
    }

    public void startGame(Game game) {
        this.games.add(game);
        game.start();

        game.getClients().forEach(c -> this.clients.put(c, game));
    }

    public void createWaitingGame(Client client) {
        this.waitingGames.add(new WaitingGame(client));
    }

    public void joinGame(Client client, UUID uuid) {
        WaitingGame game = this.waitingGames.stream().filter(g -> g.getUUID().equals(uuid)).findFirst().orElse(null);

        if (game == null) {
            System.out.println("Waiting game not found!");
            return;
        }

        game.addClient(client);
    }

    public void update() {
        this.clients.keySet().forEach(Client::update);

        this.waitingGames.stream().filter(WaitingGame::isReadyForGame)
                .forEach(g -> this.startGame(g.getGame()));
        this.waitingGames.removeIf(WaitingGame::isReadyForGame);

        this.games.forEach(Game::update);

        for (Client client : this.clients.keySet()) {
            client.sendMessage(getJson(client));
        }
    }

    public Message getJson(Client client) {
        Joinable game = this.clients.get(client);
        if (game instanceof Game g) {
            return new Message("game", g.toJson(client));
        }
        return new Message("lobby", this);
    }
}
