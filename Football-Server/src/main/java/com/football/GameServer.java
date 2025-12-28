package com.football;

import com.football.client.Client;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, (long) (GameManager.PERIOD * 1000), TimeUnit.MILLISECONDS);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected " + session.hashCode());
        Client client = new Client(session);
        clients.put(session, client);
        GameManager.getInstance().addClient(client);
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        System.out.println("Client disconnected: " + session.hashCode()
                + " status=" + status
                + " reason=" + reason);

        clients.remove(session);
        GameManager.getInstance().removeClient(getClient(session));
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Client client = getClient(session);
        packetHandler.handlePacket(client, message);
    }

    private static Client getClient(Session session) {
        return clients.get(session);
    }
}
