package net.ludocrypt.pwuno.server;

import java.util.List;

public class PrivatePlayer {

    public int id;
    public String playerName = "";
    public List<Card> cards;
    public int hand;

    public PrivatePlayer() {
    }

    public PrivatePlayer(int id, String playerName, List<Card> cards, int hand) {
        this.id = id;
        this.playerName = playerName;
        this.hand = hand;
        this.cards = cards;
    }

}
