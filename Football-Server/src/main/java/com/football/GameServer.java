package com.football;

import com.football.game.Ball;
import com.football.util.json.JsonUtil;
import com.football.util.math.geometry.Translation2d;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebSocket
public class GameServer {
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private static final Game game = new Game();

    // scheduled executor for ball updates
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    static {
        executor.scheduleAtFixedRate(() -> {
            game.update();
            broadcast(game.toJson());
        }, 0, (long) (1000 * Constants.PERIOD), TimeUnit.MILLISECONDS);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        System.out.println("Client connected");
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        sessions.remove(session);
        System.out.println("Client disconnected");
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            JsonObject obj = JsonUtil.gson.fromJson(message, JsonObject.class);
            String type = obj.get("type").getAsString();

            if ("move".equals(type)) {
                double dx = obj.get("dx").getAsDouble();
                double dy = obj.get("dy").getAsDouble();

                // Move the ball or the controlled player
                ((Ball) game.getElements().get(0)).move(new Translation2d(dx, dy).times(10)); // you need to implement this
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String message) {
        for (Session s : sessions) {
            try {
                if (s.isOpen()) s.getRemote().sendString(message);
            } catch (IOException ignored) {}
        }
    }
}
