package com.cipher.client;

import com.cipher.network.ServerConnection;
import com.cipher.ui.ChatWindow;
import com.cipher.ui.LoginDialog;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the CipherChat client.
 *
 * Shows a login dialog, connects to the server via WebSocket,
 * then opens the main chat window.
 *
 * Run: java -jar cipher-client-jar-with-dependencies.jar
 * Or via IntelliJ: run this main() directly.
 */
public class ChatClient {

    private static final Logger LOG = Logger.getLogger(ChatClient.class.getName());

    public static void main(String[] args) {
        // Dark window decorations
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);

            if (!login.isConfirmed()) {
                System.exit(0);
            }

            String username  = login.getUsername();
            String password  = login.getPassword();
            String serverUri = login.getServerUri();

            // Show a connecting spinner
            JDialog connecting = new JDialog((JFrame) null, "Connecting…", false);
            JLabel msg = new JLabel("  Connecting to " + serverUri + "…  ");
            msg.setFont(new Font("Monospaced", Font.PLAIN, 13));
            msg.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            connecting.add(msg);
            connecting.pack();
            connecting.setLocationRelativeTo(null);
            connecting.setVisible(true);

            try {
                ServerConnection conn = new ServerConnection(serverUri);

                // Open chat window once connected
                conn.onConnected(() -> SwingUtilities.invokeLater(() -> {
                    connecting.dispose();
                    conn.login(username, password);
                    new ChatWindow(username, conn);
                }));

                conn.onError(err -> {
                    if (!conn.isOpen()) {
                        SwingUtilities.invokeLater(() -> {
                            connecting.dispose();
                            JOptionPane.showMessageDialog(null,
                                "Could not connect to server:\n" + err +
                                "\n\nMake sure the server is running on " + serverUri,
                                "Connection Failed", JOptionPane.ERROR_MESSAGE);
                            System.exit(1);
                        });
                    }
                });

                conn.connectBlocking();

            } catch (Exception e) {
                connecting.dispose();
                LOG.log(Level.SEVERE, "Failed to connect", e);
                JOptionPane.showMessageDialog(null,
                    "Connection failed: " + e.getMessage() +
                    "\n\nIs the server running?",
                    "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
