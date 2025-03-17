package net.ludocrypt.pwuno.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import javax.swing.JComboBox;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.ludocrypt.pwuno.packets.ChooseSuitRequest;
import net.ludocrypt.pwuno.packets.DrawCardRequest;
import net.ludocrypt.pwuno.packets.LoginRequest;
import net.ludocrypt.pwuno.packets.LogoutRequest;
import net.ludocrypt.pwuno.packets.PlayCardRequest;
import net.ludocrypt.pwuno.packets.PlayersRequest;
import net.ludocrypt.pwuno.server.Card.CardSuit;

public class PwunoServer {
    public Server server;

    public CommonData commonData;

    public List<PlayerConnection> players;

    JComboBox<PlayerConnection> playersBox;

    public PwunoServer(int port, JComboBox<PlayerConnection> playersBox) throws IOException {
        this.playersBox = playersBox;
        server = new Server() {
            @Override
            protected Connection newConnection() {
                return new PlayerConnection();
            }
        };

        players = new ArrayList<>();
        commonData = new CommonData();

        Network.register(server);

        server.addListener(new Listener() {
            @Override
            public void received(Connection c, Object request) {

                if (c instanceof PlayerConnection player) {

                    if (request instanceof LoginRequest login) {

                        if (!commonData.gameStarted) {
                            player.playerName = login.playerName;
                            PlayersRequest.shipToClients(players);
                            System.out.println("Player connected: " + login.playerName);
                        } else {
                            System.out.println("Player kicked for joining mid-game: " + login.playerName);
                            disconnected(player);
                            LogoutRequest.send(player);
                        }

                    }

                    if (request instanceof PlayersRequest) {
                        PlayersRequest.ship(player, players, commonData);
                    }

                    if (request instanceof PlayCardRequest play) {
                        if (commonData.gameStarted) {

                            if (player.getID() == commonData.playerTurnId) {
                                Card cardToPlay = player.hand.get(play.card);

                                if (cardToPlay.canPlayOn(commonData.stack.peek(), commonData.drawLocked)) {
                                    commonData.stack.push(cardToPlay);

                                    // If the stack size exceeds 30, remove the first element
                                    if (commonData.stack.size() > 30) {
                                        commonData.stack.remove(0);
                                    }

                                    player.hand.remove(play.card);

                                    if (cardToPlay.getType().isReverse()) {
                                        commonData.reversed = !commonData.reversed;
                                    }

                                    if (!cardToPlay.isSuit(CardSuit.WILD)) {
                                        nextTurn(cardToPlay.getType().isSkip() ? 2 : 1);

                                        if (cardToPlay.getType().isDrawTwo() || cardToPlay.getType().isDrawFour()) {
                                            commonData.drawLocked = true;
                                            commonData.drawStack += cardToPlay.getType().isDrawTwo() ? 2 : 4;
                                        }
                                    }

                                    for (PlayerConnection c2 : players) {
                                        if (c2 != player) {
                                            PlayCardRequest playcard = new PlayCardRequest();
                                            c2.sendTCP(playcard);
                                        }
                                    }

                                    PlayersRequest.shipToClients(players);
                                } else {
                                    System.out.println("Player " + player.playerName + " (" + player.getID() + ") cannot play this card.");
                                }
                            } else {
                                System.out.println("Player " + player.playerName + " (" + player.getID() + ") it is not your turn.");
                            }
                        } else {
                            System.out.println("Player " + player.playerName + " (" + player.getID() + ") the game has not started.");
                        }
                    }

                    if (request instanceof ChooseSuitRequest chooseSuit) {
                        if (commonData.gameStarted) {
                            Collections.sort(player.hand);

                            if (player.getID() == commonData.playerTurnId) {

                                CardSuit suit = CardSuit.values()[chooseSuit.suit + 1];

                                Card peek = commonData.stack.peek();
                                commonData.stack.pop();
                                commonData.stack.push(peek.collapse(suit));

                                if (peek.getType().isDrawFour()) {
                                    commonData.drawLocked = true;
                                    commonData.drawStack += 4;
                                }

                                nextTurn(1);

                                PlayersRequest.shipToClients(players);

                            } else {
                                System.out.println("Player " + player.playerName + " (" + player.getID() + ") it is not your turn.");
                            }
                        } else {
                            System.out.println("Player " + player.playerName + " (" + player.getID() + ") the game has not started.");
                        }
                    }

                    if (request instanceof DrawCardRequest) {
                        if (commonData.gameStarted) {
                            if (player.getID() == commonData.playerTurnId) {

                                if (commonData.drawLocked) {
                                    for (int i = 0; i < commonData.drawStack - 1; i++) {
                                        player.hand.add(Card.draw());
                                    }

                                    commonData.drawLocked = false;
                                    commonData.drawStack = 0;
                                    nextTurn(1);
                                }

                                Collections.sort(player.hand);

                                player.hand.add(Card.draw());

                                PlayersRequest.shipToClients(players);

                            } else {
                                System.out.println("Player " + player.playerName + " (" + player.getID() + ") it is not your turn.");
                            }
                        } else {
                            System.out.println("Player " + player.playerName + " (" + player.getID() + ") the game has not started.");
                        }
                    }
                }
            }

            @Override
            public void connected(Connection c) {
                PlayerConnection player = (PlayerConnection) c;

                if (commonData.gameStarted) {
                    System.out.println("Player kicked for joining mid-game: " + player.playerName);
                    disconnected(c);
                    LogoutRequest.send(player);
                    return;
                }

                players.add(player);

                Collections.sort(players, Comparator.comparing(Connection::getID));
                updateDeck();

                PlayersRequest.shipToClients(players);
            }

            @Override
            public void disconnected(Connection c) {
                players.remove(c);

                Collections.sort(players, Comparator.comparing(Connection::getID));
                updateDeck();

                PlayersRequest.shipToClients(players);
            }
        });

        server.bind(port);
        server.start();
    }

    public void nextTurn(int c) {
        commonData.playerTurnIndex = modfix(commonData.playerTurnIndex - (commonData.reversed ? c : -c), players.size());
        commonData.playerTurnId = players.get(commonData.playerTurnIndex).getID();
    }

    public void updateDeck() {
        playersBox.removeAllItems();
        for (PlayerConnection player : players) {
            playersBox.addItem(player);
        }
    }

    private static int modfix(int a, int b) {
        int c = a % b;

        if (c < 0) {
            c += b;
        }

        return c;
    }

    public static class PlayerConnection extends Connection {

        public String playerName;
        public List<Card> hand = new ArrayList<>();

        @Override
        public String toString() {
            return this.playerName + "  (" + this.getID() + ")";
        }

    }

    public static class CommonData {

        public boolean gameStarted;
        public boolean reversed;
        public int playerTurnIndex;
        public int playerTurnId;
        public Stack<Card> stack;
        public boolean drawLocked;
        public int drawStack;

        public CommonData() {
            reversed = false;
            gameStarted = false;
            playerTurnIndex = 0;
            playerTurnId = 0;
            stack = new Stack<>();
            drawLocked = false;
            drawStack = 0;
        }

    }

}
