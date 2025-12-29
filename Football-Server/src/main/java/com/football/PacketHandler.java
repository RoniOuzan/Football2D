package com.football;

import com.football.client.Client;
import com.football.client.json.*;
import com.football.util.json.JsonUtil;

import java.util.Map;

public class PacketHandler {

    private static final Map<String, Class<? extends DataPacket>> packetTypes = Map.of(
            "input", InputPacket.class,
            "create", CreatePacket.class,
            "join", JoinPacket.class
    );

    public PacketHandler() {
    }

    public void handlePacket(Client client, String message) {
        BasePacket base = JsonUtil.fromJson(message, BasePacket.class);

        Class<? extends DataPacket> clazz = packetTypes.get(base.type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown packet type: " + base.type);
        }

        DataPacket packet = JsonUtil.fromJson(base.data, clazz);
        packet.handle(client);
    }
}
