package net.ludocrypt.pwuno.server;

import java.util.Random;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class Card implements Comparable<Card> {

    private final int number;
    private final CardSuit suit;
    private final CardType type;

    public Card(int number, CardSuit suit, CardType type) {
        this.number = number;
        this.suit = suit;
        this.type = type;
    }

    public static Card draw(Random random) {
        // 4/108 chance to draw a Wild card
        if (random.nextDouble() * 108 < 4) {
            return new Card(-1, CardSuit.WILD, CardType.WILD);
        }

        // 4/108 chance to draw a Wild Draw Four card
        if (random.nextDouble() * 108 < 4) {
            return new Card(-1, CardSuit.WILD, CardType.PLUS4);
        }

        // Randomly choose a suit
        CardSuit suit = CardSuit.random(random);

        // 19/25 chance to draw a numbered card
        if (random.nextDouble() * 25 < 19) {
            int i = 0;

            // 18/19 chance to draw a number between 1 and 9
            if (random.nextDouble() * 19 > 1) {
                i = random.nextInt(9) + 1; // Random number between 1 and 9
            }

            // Return a numbered card with the chosen suit
            return new Card(i, suit, CardType.NUMBER);
        }

        // If it's not a numbered card, it must be a special card (Skip, Reverse, Draw Two)
        // Randomly select a special card type from CardType (Skip, Reverse, Draw Two)
        CardType type = CardType.values()[random.nextInt(CardType.values().length - 3) + 3];

        // Return the special card with the chosen suit
        return new Card(-1, suit, type);
    }

    public static Card draw() {
        return draw(new Random());
    }

    public boolean canPlayOn(Card other, boolean drawLocked) {

        // Must play Plus Two or Plus Four cards when drawlocked
        if (drawLocked) {
            if (this.type.isDrawFour()) {
                return true;
            }

            if (this.type.isDrawTwo()) {
                return true;
            }

            return false;
        }

        // Wild cards can always be played
        if (this.type.isWild()) {
            return true;
        }

        // Wild Plus Four cards can always be played
        if (this.type.isDrawFour()) {
            return true;
        }

        // Numbers match numbers
        if (this.type.isNumbered()) {
            if (other.type.isNumbered()) {
                if (this.number == other.number) {
                    return true;
                }
            }
        }

        // Suits match suits
        if (this.suit.equals(other.suit)) {
            return true;
        }

        // Plus Two cards can be played on other Plus Two cards
        if (this.type.isDrawTwo() && other.type.isDrawTwo()) {
            return true;
        }

        // Plus Two cards can be played on other Plus Four cards
        if (this.type.isDrawTwo() && other.type.isDrawFour()) {
            return true;
        }

        // Plus Two cards can be played on two cards
        if (this.type.isNumbered() && this.number == 2 && other.type.isDrawTwo()) {
            return true;
        }

        // Plus Two cards can be played on two cards
        if (other.type.isNumbered() && other.number == 2 && this.type.isDrawTwo()) {
            return true;
        }

        // Plus Four cards can be played on four cards
        if (this.type.isNumbered() && this.number == 4 && other.type.isDrawFour()) {
            return true;
        }

        // Skip cards can be played on other Skip cards
        if (this.type.isSkip() && other.type.isSkip()) {
            return true;
        }

        // Reverse cards can be played on other Reverse cards
        if (this.type.isReverse() && other.type.isReverse()) {
            return true;
        }

        return false;
    }

    public Card collapse(CardSuit suit) {

        if (!this.suit.equals(CardSuit.WILD)) {
            throw new UnsupportedOperationException("This card is non-collapsable.");
        }

        if (suit.equals(CardSuit.WILD)) {
            throw new UnsupportedOperationException("Cannot collapse to a wild suit.");
        }

        return new Card(number, suit, type);
    }

    public int getNumber() {
        if (type != CardType.NUMBER)
            throw new UnsupportedOperationException(String.format("This (%s) type of card does not have a number.", this.type));

        return number;
    }

    public boolean isSuit(CardSuit suit) {
        return this.suit == suit;
    }

    public CardSuit getSuit() {
        return suit;
    }

    public boolean isType(CardType type) {
        return this.type == type;
    }

    public CardType getType() {
        return type;
    }

    public String getSpriteName(boolean drawLocked) {
        return (drawLocked && !(this.type.isDrawTwo() || this.type.isDrawFour()) ? "Disabled " : "") + suit.colorName + " " + (type.isNumbered() ? number : type.displayName);
    }

    @Override
    public String toString() {
        return type.isNumbered() ? number + " of " + suit.displayName : suit.displayName + " " + type.displayName;
    }

    @Override
    public int hashCode() {
        return 31 * number + 19 * suit.hashCode() + 11 * type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Card other = (Card) obj;
        return number == other.number && suit == other.suit && type == other.type;
    }

    @Override
    public int compareTo(Card other) {
        int i = other.suit.ordinal() - this.suit.ordinal();
        if (i != 0) {
            return i;
        }

        if (this.suit != CardSuit.WILD) {
            int j = this.type.ordinal() - other.type.ordinal();
            if (j != 0) {
                return j;
            }
        }

        return Integer.compare(this.number, other.number);
    }

    public enum CardSuit {
        WILD("Wild", "Wild"), SPADES("Spades", "Blue"), HEARTS("Hearts", "Yellow"), CLUBS("Clubs", "Red"), DIAMONDS("Diamonds", "Green");

        private final String displayName;
        private final String colorName;

        CardSuit(String displayName, String colorName) {
            this.displayName = displayName;
            this.colorName = colorName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public String getColorName() {
            return colorName;
        }

        public static CardSuit random(Random random) {
            return values()[random.nextInt(values().length - 1) + 1];
        }

        public static CardSuit random() {
            return random(new Random());
        }

        public static CardSuit byColor(String color) {
            return switch (color) {
                case "Blue" -> CardSuit.SPADES;
                case "Yellow" -> CardSuit.HEARTS;
                case "Red" -> CardSuit.CLUBS;
                case "Green" -> CardSuit.DIAMONDS;
                default -> throw new IllegalArgumentException("Unexpected value: " + color);
            };
        }
    }

    public enum CardType {
        NUMBER("Number"), WILD("Wild"), PLUS4("Draw Four"), PLUS2("Draw Two"), SKIP("Skip"), REVERSE("Reverse");

        private final String displayName;

        CardType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public boolean isNumbered() {
            return this == NUMBER;
        }

        public boolean isWild() {
            return this == WILD;
        }

        public boolean isDrawTwo() {
            return this == PLUS2;
        }

        public boolean isDrawFour() {
            return this == PLUS4;
        }

        public boolean isSkip() {
            return this == SKIP;
        }

        public boolean isReverse() {
            return this == REVERSE;
        }
    }

    public static class CardSerializer extends Serializer<Card> {

        @Override
        public Card read(Kryo kryo, Input input, Class<Card> classType) {
            int number = input.readInt();
            CardSuit suit = CardSuit.values()[input.readInt()];
            CardType type = CardType.values()[input.readInt()];
            return new Card(number, suit, type);
        }

        @Override
        public void write(Kryo kryo, Output output, Card card) {
            output.writeInt(card.number);
            output.writeInt(card.suit.ordinal());
            output.writeInt(card.type.ordinal());
        }
    }
}
