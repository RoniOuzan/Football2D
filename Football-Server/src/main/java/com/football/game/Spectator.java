package com.football.game;

import com.football.client.Client;
import com.football.game.strategy.CameraManager;
import com.football.game.strategy.TeamStrategy;

public class Spectator {
    private final Client client;
    private CameraManager cameraManager;

    public Spectator(Client client, TeamStrategy teamStrategy) {
        this.client = client;
        this.cameraManager = new CameraManager(teamStrategy);
    }

    public Client getClient() {
        return this.client;
    }

    public void update() {
        this.cameraManager.update();
    }
}
