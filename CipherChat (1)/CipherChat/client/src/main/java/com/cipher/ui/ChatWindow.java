package com.cipher.ui;

import com.cipher.model.ChatMessage;
import com.cipher.network.ServerConnection;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main chat window — connects to the server and provides a full-featured UI.
 */
public class ChatWindow extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    static final Color BG_DARK    = new Color(0x0F0F14);
    static final Color BG_SIDEBAR = new Color(0x16161E);
    static final Color BG_MAIN    = new Color(0x1A1A24);
    static final Color BG_INPUT   = new Color(0x12121A);
    static final Color BUBBLE_ME  = new Color(0x5B4BF5);
    static final Color BUBBLE_SYS = new Color(0x1E1E2A);
    static final Color BUBBLE_THEM= new Color(0x252535);
    static final Color ACCENT     = new Color(0x7C6CF8);
    static final Color TEXT       = new Color(0xECECF4);
    static final Color DIM        = new Color(0x7878A0);
    static final Color ONLINE     = new Color(0x4ADE80);
    static final Color DIVIDER    = new Color(0x252535);
    static final Color SYSTEM_CLR = new Color(0xF59E0B);

    static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault());

    // ── State ─────────────────────────────────────────────────────────────────
    private final String          username;
    private final ServerConnection conn;

    private JPanel    chatPanel;
    private JTextArea inputArea;
    private JLabel    statusLabel;
    private JLabel    connIndicator;
    private DefaultListModel<String> userListModel;
    private JList<String>            userList;
    private String    dmTarget = null; // null = global chat

    public ChatWindow(String username, ServerConnection conn) {
        super("CipherChat — " + username);
        this.username = username;
        this.conn     = conn;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 700);
        setMinimumSize(new Dimension(800, 540));
        setLocationRelativeTo(null);

        buildUI();
        wireCallbacks();
        setVisible(true);
    }

    // ── UI Construction ───────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildMain(),     BorderLayout.CENTER);
        root.add(buildUserPanel(),BorderLayout.EAST);
    }

    // ── Left sidebar ──────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(220, 0));
        side.setBackground(BG_SIDEBAR);
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DIVIDER));

        // Logo
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_SIDEBAR);
        top.setBorder(new EmptyBorder(20, 18, 14, 18));
        JLabel logo = new JLabel("cipher");
        logo.setFont(new Font("Georgia", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        top.add(logo, BorderLayout.WEST);
        side.add(top, BorderLayout.NORTH);

        // Channels
        JPanel channels = new JPanel();
        channels.setLayout(new BoxLayout(channels, BoxLayout.Y_AXIS));
        channels.setBackground(BG_SIDEBAR);
        channels.setBorder(new EmptyBorder(0, 0, 0, 0));

        channels.add(sectionLabel("CHANNELS"));
        channels.add(channelRow("# general", true, null));

        channels.add(Box.createVerticalStrut(12));
        channels.add(sectionLabel("DIRECT MESSAGES"));

        side.add(channels, BorderLayout.CENTER);

        // Self label at bottom
        JPanel selfPanel = new JPanel(new BorderLayout(8, 0));
        selfPanel.setBackground(new Color(0x0F0F14));
        selfPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        JPanel dot = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x5B4BF5));
                g2.fillOval(0, 4, 28, 28);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia", Font.BOLD, 11));
                String init = username.substring(0,1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, (28-fm.stringWidth(init))/2, 4+fm.getAscent()+(28-fm.getHeight())/2);
                g2.dispose();
            }
            { setPreferredSize(new Dimension(32,36)); setOpaque(false); }
        };
        selfPanel.add(dot, BorderLayout.WEST);
        JPanel selfInfo = new JPanel();
        selfInfo.setLayout(new BoxLayout(selfInfo, BoxLayout.Y_AXIS));
        selfInfo.setBackground(new Color(0x0F0F14));
        JLabel selfName = new JLabel(username);
        selfName.setFont(new Font("Georgia", Font.BOLD, 12));
        selfName.setForeground(TEXT);
        connIndicator = new JLabel("● Connected");
        connIndicator.setFont(new Font("Monospaced", Font.PLAIN, 10));
        connIndicator.setForeground(ONLINE);
        selfInfo.add(selfName);
        selfInfo.add(connIndicator);
        selfPanel.add(selfInfo, BorderLayout.CENTER);
        side.add(selfPanel, BorderLayout.SOUTH);

        return side;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 10));
        l.setForeground(DIM);
        l.setBorder(new EmptyBorder(6, 18, 4, 0));
        return l;
    }

    private JPanel channelRow(String label, boolean active, String dmUser) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row.setBackground(active ? new Color(0x1E1E30) : BG_SIDEBAR);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Serif", Font.PLAIN, 13));
        l.setForeground(active ? TEXT : DIM);
        row.add(l);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dmTarget = dmUser;
                updateChatHeader();
            }
        });
        return row;
    }

    // ── Main chat area ────────────────────────────────────────────────────────
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_MAIN);
        main.add(buildHeader(), BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_MAIN);
        chatPanel.setBorder(new EmptyBorder(16, 16, 8, 16));

        JScrollPane scroll = new JScrollPane(chatPanel);
        scroll.setBorder(null);
        scroll.setBackground(BG_MAIN);
        scroll.getViewport().setBackground(BG_MAIN);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);
        main.add(buildInputBar(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout(10, 0));
        hdr.setBackground(BG_SIDEBAR);
        hdr.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(14, 20, 14, 20)));

        statusLabel = new JLabel("# general — Global Chat");
        statusLabel.setFont(new Font("Georgia", Font.BOLD, 15));
        statusLabel.setForeground(TEXT);
        hdr.add(statusLabel, BorderLayout.WEST);

        JLabel hint = new JLabel("Click a user to DM · Enter to send · Shift+Enter for newline");
        hint.setFont(new Font("Monospaced", Font.PLAIN, 10));
        hint.setForeground(DIM);
        hdr.add(hint, BorderLayout.EAST);
        return hdr;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_INPUT);
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
            new EmptyBorder(12, 16, 12, 16)));

        inputArea = new JTextArea(2, 1);
        inputArea.setFont(new Font("Serif", Font.PLAIN, 14));
        inputArea.setForeground(TEXT);
        inputArea.setBackground(BG_MAIN);
        inputArea.setCaretColor(ACCENT);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new CompoundBorder(
            new LineBorder(DIVIDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)));
        inputArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    doSend();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        bar.add(inputScroll, BorderLayout.CENTER);

        JButton sendBtn = styledButton("Send ↵", BUBBLE_ME);
        sendBtn.addActionListener(e -> doSend());
        bar.add(sendBtn, BorderLayout.EAST);
        return bar;
    }

    // ── Right user panel ──────────────────────────────────────────────────────
    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBackground(BG_SIDEBAR);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DIVIDER));

        JLabel title = new JLabel("Online Users");
        title.setFont(new Font("Monospaced", Font.BOLD, 11));
        title.setForeground(DIM);
        title.setBorder(new EmptyBorder(16, 14, 8, 14));
        panel.add(title, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(BG_SIDEBAR);
        userList.setForeground(TEXT);
        userList.setFont(new Font("Serif", Font.PLAIN, 13));
        userList.setSelectionBackground(new Color(0x1E1E30));
        userList.setSelectionForeground(TEXT);
        userList.setCellRenderer(new UserCellRenderer());
        userList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null && !selected.equals(username)) {
                    dmTarget = selected;
                    updateChatHeader();
                }
            }
        });

        JScrollPane sp = new JScrollPane(userList);
        sp.setBorder(null);
        sp.setBackground(BG_SIDEBAR);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // ── Wire server callbacks (called on EDT) ─────────────────────────────────
    private void wireCallbacks() {
        conn.onMessage(msg -> SwingUtilities.invokeLater(() -> appendMessage(msg)));

        conn.onHistory(history -> SwingUtilities.invokeLater(() -> {
            chatPanel.removeAll();
            appendSystemNote("Welcome back, " + username + "! Last 50 messages loaded.");
            history.forEach(this::appendMessage);
            scrollToBottom();
        }));

        conn.onUserList(users -> SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.stream().sorted().forEach(userListModel::addElement);
        }));

        conn.onError(err -> SwingUtilities.invokeLater(() -> {
            appendSystemNote("⚠ " + err);
            if (err.contains("Invalid password")) {
                connIndicator.setText("● Auth failed");
                connIndicator.setForeground(Color.RED);
            }
        }));

        conn.onDisconnected(() -> SwingUtilities.invokeLater(() -> {
            connIndicator.setText("○ Disconnected");
            connIndicator.setForeground(DIM);
            appendSystemNote("⚡ Disconnected from server. Restart to reconnect.");
        }));
    }

    // ── Sending ───────────────────────────────────────────────────────────────
    private void doSend() {
        String text = inputArea.getText().trim();
        if (text.isEmpty() || !conn.isOpen()) return;
        inputArea.setText("");
        if (dmTarget != null) {
            conn.sendDirectMessage(username, dmTarget, text);
        } else {
            conn.sendGlobalMessage(username, text);
        }
    }

    // ── Rendering messages ────────────────────────────────────────────────────
    private void appendMessage(ChatMessage msg) {
        boolean mine   = username.equals(msg.getSender());
        boolean system = "system".equals(msg.getSender());

        if (system) { appendSystemNote(msg.getContent()); scrollToBottom(); return; }

        String time = msg.getTimestamp() != null
            ? TIME_FMT.format(Instant.parse(msg.getTimestamp()))
            : "now";

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_MAIN);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel bubble = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mine ? BUBBLE_ME : BUBBLE_THEM);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(9, 14, 9, 14));

        if (!mine) {
            JLabel sender = new JLabel(msg.getSender());
            sender.setFont(new Font("Georgia", Font.BOLD, 11));
            sender.setForeground(ACCENT);
            bubble.add(sender);
            bubble.add(Box.createVerticalStrut(2));
        }

        String dm = msg.getRecipient() != null ? " → " + msg.getRecipient() : "";
        JLabel content = new JLabel("<html><body style='width:280px'>" + escapeHtml(msg.getContent()) + "</body></html>");
        content.setFont(new Font("Serif", Font.PLAIN, 14));
        content.setForeground(TEXT);
        bubble.add(content);

        JLabel timeLabel = new JLabel(time + dm);
        timeLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        timeLabel.setForeground(mine ? new Color(0xB0A8FF) : DIM);
        timeLabel.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(timeLabel);

        JPanel wrap = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        wrap.setBackground(BG_MAIN);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        wrap.add(bubble);
        row.add(wrap);

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(6));
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom();
    }

    private void appendSystemNote(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.ITALIC, 11));
        l.setForeground(SYSTEM_CLR);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setBorder(new EmptyBorder(4, 0, 4, 0));
        chatPanel.add(l);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) chatPanel.getParent().getParent();
            JScrollBar sb = sp.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    private void updateChatHeader() {
        if (dmTarget == null) {
            statusLabel.setText("# general — Global Chat");
        } else {
            statusLabel.setText("DM → " + dmTarget + "   (click # general in sidebar to go back)");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton styledButton(String label, Color bg) {
        JButton b = new JButton(label) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(100, 42));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // ── User list cell renderer ───────────────────────────────────────────────
    private class UserCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel dot  = new JLabel("●");
        private final JLabel name = new JLabel();

        UserCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
            setOpaque(true);
            dot.setFont(new Font("Dialog", Font.PLAIN, 10));
            dot.setForeground(ONLINE);
            name.setFont(new Font("Serif", Font.PLAIN, 13));
            add(dot); add(name);
        }

        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                int idx, boolean sel, boolean focus) {
            name.setText(value);
            name.setForeground(value.equals(username) ? ACCENT : TEXT);
            setBackground(sel ? new Color(0x1E1E30) : BG_SIDEBAR);
            return this;
        }
    }
}
