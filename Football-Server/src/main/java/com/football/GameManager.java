package com.football;

import com.football.client.Client;
import com.football.client.Message;
import com.football.game.Game;
import com.football.game.Joinable;
import com.football.game.NullGame;
import com.football.game.WaitingGame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        this.clients.remove(client);
    }

    public void startGame(Game game) {
        this.games.add(game);
        game.start();

        game.getClients().forEach(c -> this.clients.put(c, game));
    }

    public void update() {
        this.clients.keySet().forEach(Client::update);

        this.waitingGames.stream().filter(WaitingGame::isReadyForGame)
                .forEach(g -> this.startGame(g.getGame()));
        this.waitingGames.removeIf(WaitingGame::isReadyForGame);

        this.games.forEach(Game::update);

        for (Client client : this.clients.keySet()) {
            try {
                client.sendMessage(getJson(client));
            } catch (IOException e) {
                e.printStackTrace();
                GameServer.removeClient(client.getSession());
            }
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
