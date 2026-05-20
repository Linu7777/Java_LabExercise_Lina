import java.util.*;

public class Deck {
    private List<Card> cards = new ArrayList<>();

    public Deck() {
        String[] suits = {"♥", "♦", "♣", "♠"};
        String[] ranks = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};
        int[] values = {2,3,4,5,6,7,8,9,10,11,12,13,14};

        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                cards.add(new Card(suit, ranks[i], values[i]));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card deal() {
        return cards.isEmpty() ? null : cards.remove(0);
    }
}