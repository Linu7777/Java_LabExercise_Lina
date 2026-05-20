import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;        // This must come after java.awt.*
import java.util.List;     // Explicit import to avoid ambiguity

public class PokerGameGUI extends JFrame {
    private static final int STARTING_BALANCE = 1000;
    private int balance = STARTING_BALANCE;
    private int currentBet = 0;

    private JLabel balanceLabel;
    private JTextField betField;
    private JButton btnDeal, btnDraw;

    private JPanel cardPanel;
    private JLabel[] cardLabels = new JLabel[5];
    private JCheckBox[] keepCheckBoxes = new JCheckBox[5];

    private List<Card> hand = new ArrayList<>();
    private Deck deck;

    public PokerGameGUI() {
        setTitle("♠ Java Poker - 5 Card Draw ♠");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        updateBalance();
    }

    private void initComponents() {
        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        balanceLabel = new JLabel("Balance: $" + balance);
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 20));
        topPanel.add(balanceLabel);

        betField = new JTextField("10", 8);
        JButton btnBet = new JButton("Place Bet & Deal");
        btnBet.addActionListener(e -> placeBetAndDeal());

        topPanel.add(new JLabel("Bet: $"));
        topPanel.add(betField);
        topPanel.add(btnBet);

        add(topPanel, BorderLayout.NORTH);

        // Cards Panel
        cardPanel = new JPanel(new GridLayout(1, 5, 15, 10));
        cardPanel.setBorder(BorderFactory.createTitledBorder("Your Hand"));

        for (int i = 0; i < 5; i++) {
            JPanel slot = new JPanel(new BorderLayout());
            cardLabels[i] = new JLabel("?", SwingConstants.CENTER);
            cardLabels[i].setFont(new Font("Arial", Font.BOLD, 52));
            cardLabels[i].setPreferredSize(new Dimension(130, 180));
            cardLabels[i].setBorder(BorderFactory.createRaisedBevelBorder());

            keepCheckBoxes[i] = new JCheckBox("Keep");
            keepCheckBoxes[i].setHorizontalAlignment(SwingConstants.CENTER);

            slot.add(cardLabels[i], BorderLayout.CENTER);
            slot.add(keepCheckBoxes[i], BorderLayout.SOUTH);
            cardPanel.add(slot);
        }

        add(cardPanel, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel();
        btnDeal = new JButton("New Hand");
        btnDraw = new JButton("DRAW CARDS");
        btnDraw.setEnabled(false);
        btnDraw.setFont(new Font("Arial", Font.BOLD, 16));

        btnDeal.addActionListener(e -> startNewHand());
        btnDraw.addActionListener(e -> drawCards());

        bottomPanel.add(btnDeal);
        bottomPanel.add(btnDraw);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateBalance() {
        balanceLabel.setText("Balance: $" + balance);
    }

    private void placeBetAndDeal() {
        try {
            int bet = Integer.parseInt(betField.getText().trim());
            bet = Math.max(1, Math.min(bet, Math.min(50, balance)));

            if (bet > balance) {
                JOptionPane.showMessageDialog(this, "Insufficient balance!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            currentBet = bet;
            balance -= bet;
            updateBalance();
            startNewHand();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid bet amount.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startNewHand() {
        deck = new Deck();
        deck.shuffle();

        hand.clear();
        for (int i = 0; i < 5; i++) {
            hand.add(deck.deal());
        }

        displayHand();
        btnDraw.setEnabled(true);
        btnDeal.setEnabled(false);

        for (JCheckBox cb : keepCheckBoxes) {
            cb.setSelected(false);
        }
    }

    private void displayHand() {
        for (int i = 0; i < 5; i++) {
            Card card = hand.get(i);
            cardLabels[i].setText(card.toString());
            cardLabels[i].setForeground(
                    card.getSuit().equals("♥") || card.getSuit().equals("♦") ? Color.RED : Color.BLACK
            );
        }
    }

    private void drawCards() {
        for (int i = 0; i < 5; i++) {
            if (!keepCheckBoxes[i].isSelected()) {
                hand.set(i, deck.deal());
            }
        }

        displayHand();
        evaluateHand();
    }

    private void evaluateHand() {
        String result = PokerHandEvaluator.evaluate(hand);
        int multiplier = getMultiplier(result);
        int winnings = currentBet * multiplier;
        balance += winnings;

        String msg = result + "\n\nWinnings: $" + winnings + "\nNew Balance: $" + balance;

        JOptionPane.showMessageDialog(this, msg, "Result",
                winnings > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.PLAIN_MESSAGE);

        updateBalance();

        btnDraw.setEnabled(false);
        btnDeal.setEnabled(true);

        if (balance <= 0) {
            JOptionPane.showMessageDialog(this, "Game Over!\nYou're out of money.", "Game Over", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private int getMultiplier(String result) {
        if (result.contains("Royal")) return 250;
        if (result.contains("Straight Flush")) return 50;
        if (result.contains("Four")) return 25;
        if (result.contains("Full")) return 9;
        if (result.contains("Flush")) return 6;
        if (result.contains("Straight")) return 5;
        if (result.contains("Three")) return 3;
        if (result.contains("Two Pair")) return 2;
        if (result.contains("Jacks")) return 1;
        return 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PokerGameGUI().setVisible(true));
    }
}