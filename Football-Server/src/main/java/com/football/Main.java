package com.football;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server(9090);

        // Use ServletContextHandler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Configure WebSocket mapping
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/game", (req, resp) -> new GameServer());
        });

        server.start();
        System.out.println("⚽ Server running on ws://localhost:9090/game");
        server.join();
    }
}

