package com.football.game;

import com.football.client.Client;
import com.football.game.strategy.camera.CameraManager;
import com.football.game.strategy.TeamStrategy;
import lombok.Getter;

public class Spectator {
    @Getter
    private final Client client;
    private final CameraManager cameraManager;

    public Spectator(Client client, TeamStrategy teamStrategy) {
        this.client = client;
        this.cameraManager = new CameraManager(teamStrategy);
    }

    public void update() {
        this.cameraManager.update();
    }
}
