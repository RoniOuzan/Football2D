package com.football;

import com.football.client.Client;
import com.football.client.json.BasePacket;
import com.football.client.json.InputPacket;
import com.football.util.json.JsonUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebSocket
public class GameServer {
    private static final Map<Session, Client> clients = new ConcurrentHashMap<>();
    private static final PacketHandler packetHandler = new PacketHandler();

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    static {
        executor.scheduleAtFixedRate(() -> {
            try {
                GameManager.getInstance().update();
                broadcast(GameManager.getInstance().getJson());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, (long) (GameManager.PERIOD * 1000), TimeUnit.MILLISECONDS);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        Client client = new Client(session);
        clients.put(session, client);
        System.out.println("Client connected " + session.hashCode());

        GameManager.getInstance().addClient(client);
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        GameManager.getInstance().removeClient(getClient(session));

        clients.remove(session);
        System.out.println("Client disconnected: " + session.hashCode()
                + " status=" + status
                + " reason=" + reason);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Client client = getClient(session);
        packetHandler.handlePacket(client, message);
    }

    private static void broadcast(String message) {
        for (Client client : clients.values()) {
            Session s = client.getSession();
            try {
                if (s.isOpen()) {
                    s.getRemote().sendString(message);
                } else {
                    clients.remove(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
                clients.remove(s);
            }
        }

    }

    private static Client getClient(Session session) {
        return clients.get(session);
    }
}
