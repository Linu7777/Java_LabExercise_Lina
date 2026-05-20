import java.util.*;

public class PokerHandEvaluator {

    public static String evaluate(List<Card> hand) {
        hand.sort(Comparator.comparingInt(Card::getRankValue));
        boolean flush = isFlush(hand);
        boolean straight = isStraight(hand);
        int[] ranks = getRankCounts(hand);

        if (flush && straight && hand.get(4).getRankValue() == 14) return "Royal Flush (250x)";
        if (flush && straight) return "Straight Flush (50x)";
        if (hasCount(ranks, 4)) return "Four of a Kind (25x)";
        if (hasFullHouse(ranks)) return "Full House (9x)";
        if (flush) return "Flush (6x)";
        if (straight) return "Straight (5x)";
        if (hasCount(ranks, 3)) return "Three of a Kind (3x)";
        if (countPairs(ranks) == 2) return "Two Pair (2x)";
        if (countPairs(ranks) == 1 && hasHighPair(hand)) return "Jacks or Better (1x)";
        return "No Win";
    }

    private static boolean isFlush(List<Card> hand) {
        String suit = hand.get(0).getSuit();
        return hand.stream().allMatch(c -> c.getSuit().equals(suit));
    }

    private static boolean isStraight(List<Card> hand) {
        int first = hand.get(0).getRankValue();
        for (int i = 1; i < 5; i++) {
            if (hand.get(i).getRankValue() != first + i) return false;
        }
        return true;
    }

    private static int[] getRankCounts(List<Card> hand) {
        int[] counts = new int[15];
        for (Card c : hand) counts[c.getRankValue()]++;
        return counts;
    }

    private static boolean hasCount(int[] ranks, int count) {
        for (int r : ranks) if (r == count) return true;
        return false;
    }

    private static boolean hasFullHouse(int[] ranks) {
        return hasCount(ranks, 3) && hasCount(ranks, 2);
    }

    private static int countPairs(int[] ranks) {
        int pairs = 0;
        for (int r : ranks) if (r == 2) pairs++;
        return pairs;
    }

    private static boolean hasHighPair(List<Card> hand) {
        for (Card c : hand) {
            if (c.getRankValue() >= 11) return true; // J, Q, K, A
        }
        return false;
    }
}