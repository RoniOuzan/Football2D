package com.football;

import com.football.client.BotClient;
import com.football.client.Client;
import com.football.client.messages.AlertMessage;
import com.football.client.messages.Message;
import com.football.game.Game;
import com.football.game.Joinable;
import com.football.game.NullGame;
import com.football.game.WaitingGame;

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
    public static final double GAME_REAL_TIME = 300; // 6 Minutes
    public static final double GAME_TIME = 90 * 60;

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
                    c.sendMessage(new AlertMessage("OOPS! one of the clients has quit the game!", "red", 1, "medium"));
                });
                this.games.remove(g);
            } else if (g.getSpectators().stream().anyMatch(s -> s.getClient().equals(client))) {
                g.removeSpectator(client);
            }
        }
    }

    public void startGame(Game game) {
        this.games.add(game);
        game.start();

        game.getClients().forEach(c -> this.clients.put(c, game));
    }

    public void startBotGame(Client client) {
        Game game = new Game(client, new BotClient());
        this.games.add(game);

        game.start();
        this.clients.put(client, game);
    }

    public void startOnlyBotGame(Client client) {
        Game game = new Game(new BotClient(), new BotClient());
        this.games.add(game);

        game.start();
        game.addSpectator(client);
        this.clients.put(client, game);
    }

    public void createWaitingGame(Client client) {
        WaitingGame game = new WaitingGame(client);
        this.waitingGames.add(game);
        this.clients.put(client, game);
    }

    public void joinGame(Client client, UUID uuid) {
        WaitingGame game = this.waitingGames.stream().filter(g -> g.getUUID().equals(uuid)).findFirst().orElse(null);

        if (game == null) {
            System.out.println("Waiting game not found!");
            return;
        }

        game.addClient(client);
        this.clients.put(client, game);
    }

    public void spectateGame(Client client, UUID uuid) {
        Game game = this.games.stream().filter(g -> g.getUUID().equals(uuid)).findFirst().orElse(null);

        if (game == null) {
            System.out.println("No such game was found!");
            return;
        }

        game.addSpectator(client);
        this.clients.put(client, game);
    }

    public void update() {
        this.clients.keySet().forEach(Client::update);

        this.waitingGames.stream().filter(WaitingGame::isReadyForGame)
                .forEach(g -> this.startGame(g.getOnlineGame()));
        this.waitingGames.removeIf(WaitingGame::isReadyForGame);

        this.updateGames();

        for (Client client : this.clients.keySet()) {
            client.sendMessage(getJson(client));
        }
    }

    public void updateGames() {
        for (Game game : this.games) {
            game.update();

            if (game.getPhase() == Game.Phase.FINISH) {
                // TODO
            }
        }
    }

    public Joinable getGameForClient(Client client) {
        return this.clients.get(client);
    }

    public Message getJson(Client client) {
        Joinable game = this.clients.get(client);
        if (game instanceof Game g) {
            return new Message("game", g.toJson(client));
        }
        return new Message("lobby", this);
    }
}
