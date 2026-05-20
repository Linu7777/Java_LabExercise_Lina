package com.cipher.handler;

import com.cipher.db.DatabaseManager;
import com.cipher.model.ChatMessage;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all WebSocket events for every connected client.
 * Protocol (JSON):
 *   Client → Server:
 *     { type:"LOGIN",   sender:"username", content:"password" }
 *     { type:"MESSAGE", sender:"username", recipient:null|"user", content:"text" }
 *
 *   Server → Client:
 *     { type:"MESSAGE",   sender:..., content:..., timestamp:... }
 *     { type:"HISTORY",   sender:"server", content:"[]" (JSON array of messages) }
 *     { type:"USER_LIST", sender:"server", content:"[]" (JSON array of usernames) }
 *     { type:"ERROR",     sender:"server", content:"reason" }
 */
@WebSocket
public class ChatWebSocketHandler {

    private static final Logger LOG = Logger.getLogger(ChatWebSocketHandler.class.getName());
    private static final Gson GSON = new Gson();

    // Session → username for logged-in clients
    private static final ConcurrentHashMap<Session, String> SESSIONS = new ConcurrentHashMap<>();

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.info("New WS connection from " + session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String user = SESSIONS.remove(session);
        if (user != null) {
            LOG.info(user + " disconnected");
            broadcastUserList();
            broadcastSystem(user + " left the chat");
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        LOG.log(Level.WARNING, "WS error for " + SESSIONS.getOrDefault(session, "unknown"), error);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String raw) {
        try {
            ChatMessage msg = GSON.fromJson(raw, ChatMessage.class);
            if (msg == null || msg.getType() == null) { sendError(session, "Invalid message"); return; }

            switch (msg.getType()) {
                case LOGIN   -> handleLogin(session, msg);
                case MESSAGE -> handleMessage(session, msg);
                default      -> sendError(session, "Unknown message type");
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to process message: " + raw, e);
            sendError(session, "Server error: " + e.getMessage());
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────
    private void handleLogin(Session session, ChatMessage msg) {
        String username = msg.getSender();
        String password = msg.getContent();

        if (username == null || username.isBlank()) { sendError(session, "Username required"); return; }

        // Auto-register if user doesn't exist, else authenticate
        if (!db.userExists(username)) {
            db.registerUser(username, password != null ? password : "");
            LOG.info("Auto-registered new user: " + username);
        } else if (password != null && !db.authenticateUser(username, password)) {
            sendError(session, "Invalid password");
            return;
        }

        SESSIONS.put(session, username);
        LOG.info(username + " logged in");

        // Send message history
        sendHistory(session, username);

        // Notify everyone of updated user list
        broadcastUserList();
        broadcastSystem(username + " joined the chat");
    }

    private void handleMessage(Session session, ChatMessage msg) {
        String sender = SESSIONS.get(session);
        if (sender == null) { sendError(session, "Not authenticated — send LOGIN first"); return; }

        msg.setSender(sender);
        msg.setTimestamp(Instant.now().toString());

        // Persist
        db.saveMessage(msg);

        if (msg.getRecipient() != null && !msg.getRecipient().isBlank()) {
            // Direct message — send to recipient and back to sender
            sendToUser(msg.getRecipient(), msg);
            sendToSession(session, msg);
        } else {
            // Broadcast
            broadcast(msg);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void sendHistory(Session session, String username) {
        List<ChatMessage> history = db.getRecentGlobalMessages(50);
        ChatMessage histMsg = new ChatMessage(
            ChatMessage.Type.HISTORY, "server", username,
            GSON.toJson(history), Instant.now().toString()
        );
        sendToSession(session, histMsg);
    }

    private void broadcastUserList() {
        Set<String> users = ConcurrentHashMap.newKeySet();
        users.addAll(SESSIONS.values());
        ChatMessage msg = new ChatMessage(
            ChatMessage.Type.USER_LIST, "server", null,
            GSON.toJson(users), Instant.now().toString()
        );
        broadcast(msg);
    }

    private void broadcastSystem(String text) {
        ChatMessage msg = new ChatMessage(
            ChatMessage.Type.MESSAGE, "system", null, text, Instant.now().toString()
        );
        broadcast(msg);
    }

    private void broadcast(ChatMessage msg) {
        String json = GSON.toJson(msg);
        SESSIONS.keySet().forEach(s -> sendRaw(s, json));
    }

    private void sendToUser(String username, ChatMessage msg) {
        String json = GSON.toJson(msg);
        SESSIONS.forEach((s, u) -> { if (u.equalsIgnoreCase(username)) sendRaw(s, json); });
    }

    private void sendToSession(Session session, ChatMessage msg) {
        sendRaw(session, GSON.toJson(msg));
    }

    private void sendError(Session session, String reason) {
        ChatMessage err = new ChatMessage(ChatMessage.Type.ERROR, "server", null, reason, Instant.now().toString());
        sendRaw(session, GSON.toJson(err));
    }

    private void sendRaw(Session session, String json) {
        try {
            if (session.isOpen()) session.getRemote().sendString(json);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Send failed", e);
        }
    }
}
