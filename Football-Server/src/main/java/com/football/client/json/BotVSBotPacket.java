package com.football.client.json;

import com.football.GameManager;
import com.football.client.Client;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class BotVSBotPacket implements DataPacket {

    @Override
    public void handle(Client client) {
        GameManager.getInstance().startOnlyBotGame(client);
    }
}
