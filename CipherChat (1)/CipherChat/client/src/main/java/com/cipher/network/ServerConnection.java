package com.cipher.network;

import com.cipher.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket client that connects to the CipherChat server.
 * Exposes simple callbacks so the UI layer stays clean.
 */
public class ServerConnection extends WebSocketClient {

    private static final Logger LOG = Logger.getLogger(ServerConnection.class.getName());
    private static final Gson GSON = new Gson();

    private Consumer<ChatMessage>       onMessage;
    private Consumer<List<ChatMessage>> onHistory;
    private Consumer<List<String>>      onUserList;
    private Consumer<String>            onError;
    private Runnable                    onConnected;
    private Runnable                    onDisconnected;

    public ServerConnection(String serverUri) throws Exception {
        super(new URI(serverUri));
    }

    // ── Callbacks (set before connect()) ─────────────────────────────────────
    public void onMessage(Consumer<ChatMessage> cb)       { this.onMessage    = cb; }
    public void onHistory(Consumer<List<ChatMessage>> cb) { this.onHistory    = cb; }
    public void onUserList(Consumer<List<String>> cb)     { this.onUserList   = cb; }
    public void onError(Consumer<String> cb)              { this.onError      = cb; }
    public void onConnected(Runnable cb)                  { this.onConnected  = cb; }
    public void onDisconnected(Runnable cb)               { this.onDisconnected = cb; }

    // ── WebSocketClient overrides ─────────────────────────────────────────────
    @Override
    public void onOpen(ServerHandshake handshake) {
        LOG.info("Connected to server");
        if (onConnected != null) onConnected.run();
    }

    @Override
    public void onMessage(String raw) {
        try {
            ChatMessage msg = GSON.fromJson(raw, ChatMessage.class);
            if (msg == null) return;

            switch (msg.getType()) {
                case HISTORY -> {
                    Type listType = new TypeToken<List<ChatMessage>>(){}.getType();
                    List<ChatMessage> history = GSON.fromJson(msg.getContent(), listType);
                    if (onHistory != null) onHistory.accept(history);
                }
                case USER_LIST -> {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List<String> users = GSON.fromJson(msg.getContent(), listType);
                    if (onUserList != null) onUserList.accept(users);
                }
                case ERROR -> {
                    if (onError != null) onError.accept(msg.getContent());
                }
                default -> {
                    if (onMessage != null) onMessage.accept(msg);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse server message: " + raw, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.info("Disconnected from server: " + reason);
        if (onDisconnected != null) onDisconnected.run();
    }

    @Override
    public void onError(Exception ex) {
        LOG.log(Level.WARNING, "WebSocket error", ex);
        if (onError != null) onError.accept(ex.getMessage());
    }

    // ── API ───────────────────────────────────────────────────────────────────
    public void login(String username, String password) {
        ChatMessage msg = new ChatMessage(ChatMessage.Type.LOGIN, username, null, password);
        send(GSON.toJson(msg));
    }

    public void sendGlobalMessage(String sender, String text) {
        ChatMessage msg = new ChatMessage(ChatMessage.Type.MESSAGE, sender, null, text);
        send(GSON.toJson(msg));
    }

    public void sendDirectMessage(String sender, String recipient, String text) {
        ChatMessage msg = new ChatMessage(ChatMessage.Type.MESSAGE, sender, recipient, text);
        send(GSON.toJson(msg));
    }
}
