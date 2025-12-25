package com.football;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create Jetty server
        Server server = new Server();

        // Configure SSL with PKCS12 keystore
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("../Football-Client/192.168.1.73.p12"); // your converted PKCS12 file
        sslContextFactory.setKeyStorePassword("1234");         // password used when exporting PKCS12
        sslContextFactory.setKeyStoreType("PKCS12");

        // HTTP configuration for HTTPS
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(9090);

        // Create connector with SSL
        ServerConnector sslConnector = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConfig)
        );
        sslConnector.setPort(9090);
        sslConnector.setHost("0.0.0.0"); // listen on all interfaces
        server.addConnector(sslConnector);

        // Setup servlet context
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Setup WebSocket mapping
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/game", (req, resp) -> new GameServer());
        });

        // Start server
        server.start();

        String lanIp = getLocalIp();
        System.out.println("⚽ Server running on wss://" + lanIp + ":9090/game");

        server.join();
    }

    // Helper to find LAN IP
    public static String getLocalIp() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr.isSiteLocalAddress()) return addr.getHostAddress();
            }
        }
        throw new RuntimeException("No LAN IP found");
    }
}