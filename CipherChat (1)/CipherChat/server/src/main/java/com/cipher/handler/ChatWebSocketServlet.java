package com.cipher.handler;

import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;

/**
 * Servlet that upgrades HTTP to WebSocket connections.
 * Mapped to /chat in ChatServer.
 */
public class ChatWebSocketServlet extends JettyWebSocketServlet {

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(java.time.Duration.ofMinutes(10));
        factory.setMaxTextMessageSize(64 * 1024); // 64 KB
        factory.setCreator((JettyWebSocketCreator) (req, resp) -> new ChatWebSocketHandler());
    }
}
