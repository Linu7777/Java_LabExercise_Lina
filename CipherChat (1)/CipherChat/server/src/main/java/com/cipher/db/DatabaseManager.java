package com.cipher.db;

import com.cipher.model.ChatMessage;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton that manages the SQLite database.
 * Tables: users, messages
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:cipher_chat.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            initSchema();
            LOG.info("SQLite database ready at cipher_chat.db");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise database", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ── Schema ────────────────────────────────────────────────────────────────
    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    username  TEXT    NOT NULL UNIQUE,
                    password  TEXT    NOT NULL,
                    created_at TEXT   NOT NULL DEFAULT (datetime('now'))
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender     TEXT NOT NULL,
                    recipient  TEXT,
                    content    TEXT NOT NULL,
                    timestamp  TEXT NOT NULL,
                    is_dm      INTEGER NOT NULL DEFAULT 0
                )""");

            // Seed some users if table is empty
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                seedUsers();
            }
        }
    }

    private void seedUsers() throws SQLException {
        String[] users = {"alice", "bob", "dana", "erin", "frank"};
        for (String u : users) {
            registerUser(u, "pass123");
        }
        LOG.info("Seeded default users: alice, bob, dana, erin, frank (password: pass123)");
    }

    // ── User operations ───────────────────────────────────────────────────────
    public boolean registerUser(String username, String password) {
        String sql = "INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            ps.setString(2, hashPassword(password));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "registerUser failed", e);
            return false;
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(hashPassword(password));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "authenticateUser failed", e);
        }
        return false;
    }

    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ── Message operations ────────────────────────────────────────────────────
    public void saveMessage(ChatMessage msg) {
        String sql = "INSERT INTO messages (sender, recipient, content, timestamp, is_dm) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, msg.getSender());
            ps.setString(2, msg.getRecipient());
            ps.setString(3, msg.getContent());
            ps.setString(4, msg.getTimestamp() != null ? msg.getTimestamp() : Instant.now().toString());
            ps.setInt(5, msg.getRecipient() != null ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "saveMessage failed", e);
        }
    }

    /** Fetch the last N global (broadcast) messages. */
    public List<ChatMessage> getRecentGlobalMessages(int limit) {
        String sql = """
            SELECT id, sender, content, timestamp
            FROM messages
            WHERE is_dm = 0
            ORDER BY id DESC LIMIT ?
            """;
        List<ChatMessage> msgs = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChatMessage m = new ChatMessage(
                    ChatMessage.Type.MESSAGE,
                    rs.getString("sender"),
                    null,
                    rs.getString("content"),
                    rs.getString("timestamp")
                );
                m.setId(rs.getLong("id"));
                msgs.add(0, m); // reverse so oldest first
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getRecentGlobalMessages failed", e);
        }
        return msgs;
    }

    /** Fetch DM history between two users. */
    public List<ChatMessage> getDmHistory(String userA, String userB, int limit) {
        String sql = """
            SELECT id, sender, recipient, content, timestamp
            FROM messages
            WHERE is_dm = 1
              AND ((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?))
            ORDER BY id DESC LIMIT ?
            """;
        List<ChatMessage> msgs = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userA); ps.setString(2, userB);
            ps.setString(3, userB); ps.setString(4, userA);
            ps.setInt(5, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChatMessage m = new ChatMessage(
                    ChatMessage.Type.MESSAGE,
                    rs.getString("sender"),
                    rs.getString("recipient"),
                    rs.getString("content"),
                    rs.getString("timestamp")
                );
                m.setId(rs.getLong("id"));
                msgs.add(0, m);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "getDmHistory failed", e);
        }
        return msgs;
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    /** Very simple hash — use BCrypt in production. */
    private String hashPassword(String password) {
        int hash = 7;
        for (char c : password.toCharArray()) hash = hash * 31 + c;
        return Integer.toHexString(hash);
    }

    public void close() {
        try { if (connection != null) connection.close(); }
        catch (SQLException ignored) {}
    }
}
