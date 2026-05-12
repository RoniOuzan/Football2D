package com.football.client.json;

import com.football.GameManager;
import com.football.client.Client;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@ToString
@AllArgsConstructor
public class SpectatePacket implements DataPacket {
    public UUID uuid;

    @Override
    public void handle(Client client) {
        GameManager.getInstance().spectateGame(client, this.uuid);
    }
}
