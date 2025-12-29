package com.football.client.json;

import com.football.GameManager;
import com.football.client.Client;
import com.football.util.math.geometry.Translation2d;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@ToString
@AllArgsConstructor
public class CreatePacket implements DataPacket {

    @Override
    public void handle(Client client) {
        GameManager.getInstance().createWaitingGame(client);
    }
}
