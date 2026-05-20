package com.cipher.server;

import com.cipher.db.DatabaseManager;
import com.cipher.handler.ChatWebSocketHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Entry point for the CipherChat server.
 *
 * Starts a Jetty server on port 8080.
 * WebSocket endpoint: ws://localhost:8080/chat
 */
public class ChatServer {

    private static final Logger LOG = Logger.getLogger(ChatServer.class.getName());
    public static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        // Initialise database (creates tables + seeds users)
        DatabaseManager.getInstance();

        // Jetty server
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(PORT);
        server.addConnector(connector);

        // Servlet context — NO SESSIONS needed for plain WebSocket
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // *** This is the fix: initialise WebSocketComponents BEFORE starting ***
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.setMaxTextMessageSize(64 * 1024);
            wsContainer.setIdleTimeout(Duration.ofMinutes(10));
            wsContainer.addMapping("/chat", (req, resp) -> new ChatWebSocketHandler());
        });

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down CipherChat server...");
            try { server.stop(); } catch (Exception ignored) {}
            DatabaseManager.getInstance().close();
        }));

        server.start();
        LOG.info("╔══════════════════════════════════════════╗");
        LOG.info("║  CipherChat Server started on :" + PORT + "     ║");
        LOG.info("║  WebSocket: ws://localhost:" + PORT + "/chat    ║");
        LOG.info("║  Users: alice/bob/dana/erin/frank (pass123) ║");
        LOG.info("╚══════════════════════════════════════════╝");
        server.join();
    }
}
