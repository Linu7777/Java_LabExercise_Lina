package com.cipher.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Modal login dialog shown before the main chat window.
 * Returns username/password or null if cancelled.
 */
public class LoginDialog extends JDialog {

    // ── Palette ──────────────────────────────────────────────────────────────
    static final Color BG       = new Color(0x0F0F14);
    static final Color CARD     = new Color(0x16161E);
    static final Color ACCENT   = new Color(0x5B4BF5);
    static final Color TEXT     = new Color(0xECECF4);
    static final Color DIM      = new Color(0x7878A0);
    static final Color BORDER   = new Color(0x252535);

    private JTextField  usernameField;
    private JPasswordField passwordField;
    private JTextField  serverField;
    private String      resultUsername;
    private String      resultPassword;
    private String      resultServer;
    private boolean     confirmed = false;

    public LoginDialog(Frame parent) {
        super(parent, "CipherChat — Sign In", true);
        setResizable(false);
        buildUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(40, 50, 40, 50));
        setContentPane(root);

        // Title
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(BG);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("cipher");
        logo.setFont(new Font("Georgia", Font.BOLD, 32));
        logo.setForeground(ACCENT);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("real-time messenger");
        sub.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sub.setForeground(DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(logo);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(sub);
        root.add(titlePanel, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel();
        form.setBackground(BG);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(28, 0, 0, 0));

        form.add(fieldLabel("Username"));
        usernameField = styledTextField("e.g. alice");
        form.add(usernameField);
        form.add(Box.createVerticalStrut(14));

        form.add(fieldLabel("Password"));
        passwordField = new JPasswordField();
        styleField(passwordField, "pass123");
        form.add(passwordField);
        form.add(Box.createVerticalStrut(14));

        form.add(fieldLabel("Server"));
        serverField = styledTextField("ws://localhost:8080/chat");
        serverField.setText("ws://localhost:8080/chat");
        form.add(serverField);
        form.add(Box.createVerticalStrut(24));

        // Sign in button
        JButton signIn = new JButton("Sign In") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT.brighter() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        signIn.setPreferredSize(new Dimension(300, 42));
        signIn.setMaximumSize(new Dimension(300, 42));
        signIn.setBorderPainted(false);
        signIn.setContentAreaFilled(false);
        signIn.setFocusPainted(false);
        signIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signIn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signIn.addActionListener(e -> doLogin());
        form.add(signIn);

        JLabel hint = new JLabel("New users are registered automatically");
        hint.setFont(new Font("Monospaced", Font.PLAIN, 10));
        hint.setForeground(DIM);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(Box.createVerticalStrut(10));
        form.add(hint);

        root.add(form, BorderLayout.CENTER);

        // Enter key
        getRootPane().setDefaultButton(signIn);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setForeground(DIM);
        l.setBorder(new EmptyBorder(0, 2, 4, 0));
        return l;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField f = new JTextField();
        styleField(f, placeholder);
        return f;
    }

    private void styleField(JTextField f, String placeholder) {
        f.setFont(new Font("Serif", Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setBackground(CARD);
        f.setCaretColor(ACCENT);
        f.setPreferredSize(new Dimension(300, 38));
        f.setMaximumSize(new Dimension(300, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(6, 12, 6, 12)));
    }

    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword()).trim();
        String s = serverField.getText().trim();
        if (u.isEmpty()) {
            shake();
            return;
        }
        resultUsername = u;
        resultPassword = p.isEmpty() ? "pass123" : p;
        resultServer   = s.isEmpty() ? "ws://localhost:8080/chat" : s;
        confirmed = true;
        dispose();
    }

    private void shake() {
        Point orig = getLocation();
        Timer t = new Timer(30, null);
        int[] count = {0};
        t.addActionListener(e -> {
            int dx = (count[0] % 2 == 0) ? 8 : -8;
            setLocation(orig.x + dx, orig.y);
            if (++count[0] >= 8) { t.stop(); setLocation(orig); }
        });
        t.start();
    }

    public boolean isConfirmed()   { return confirmed; }
    public String  getUsername()   { return resultUsername; }
    public String  getPassword()   { return resultPassword; }
    public String  getServerUri()  { return resultServer; }
}
