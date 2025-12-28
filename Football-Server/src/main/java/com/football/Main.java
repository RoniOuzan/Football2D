package com.football;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create Jetty server without specifying port yet
        Server server = new Server();

        // Configure connector to listen on all network interfaces
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(9090);
        connector.setHost("0.0.0.0");  // Listen on all interfaces
        server.addConnector(connector);

        // Use ServletContextHandler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Configure WebSocket mapping
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/game", (req, resp) -> new GameServer());
        });

        // Start server
        server.start();

        // Print actual LAN IP
        String lanIp = getLocalIp();
        System.out.println("⚽ Server running on ws://" + lanIp + ":9090/game");

        server.join();
    }

    public static String getLocalIp() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr.isSiteLocalAddress()) {
                    return addr.getHostAddress();
                }
            }
        }
        throw new RuntimeException("No LAN IP found");
    }
}

