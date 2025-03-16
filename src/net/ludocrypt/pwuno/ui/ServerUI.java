package net.ludocrypt.pwuno.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Random;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.ludocrypt.pwuno.packets.PlayersRequest;
import net.ludocrypt.pwuno.server.Card;
import net.ludocrypt.pwuno.server.Card.CardSuit;
import net.ludocrypt.pwuno.server.Card.CardType;
import net.ludocrypt.pwuno.server.PwunoServer;
import net.ludocrypt.pwuno.server.PwunoServer.PlayerConnection;

public class ServerUI {

    PwunoServer server;

    public ServerUI(int port) throws IOException {
        JFrame frame = new JFrame("Server Console");
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton startButton = new JButton("Start");
        JButton skipTurnButton = new JButton("Skip Turn");
        JButton restartButton = new JButton("Restart Game");

        buttonPanel.add(startButton);
        buttonPanel.add(skipTurnButton);
        buttonPanel.add(restartButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> {
            if (!server.commonData.gameStarted) {
                for (PlayerConnection c : server.players) {
                    for (int i = 0; i < 7; i++) {
                        c.hand.add(Card.draw());
                    }
                    Collections.sort(c.hand);
                }

                server.commonData.gameStarted = true;
                server.commonData.stack = new Stack<Card>();
                server.commonData.stack.push(new Card(new Random().nextInt(9) + 1, CardSuit.random(), CardType.NUMBER));
                server.commonData.playerTurnIndex = 0;
                server.commonData.playerTurnId = server.players.get(0).getID();

                PlayersRequest.shipToClients(server.players);
                startButton.setEnabled(false);
            } else {
                startButton.setEnabled(false);
            }
        });

        skipTurnButton.addActionListener(e -> {
            if (server.commonData.gameStarted) {

                server.nextTurn(1);
                PlayersRequest.shipToClients(server.players);

            } else {
                skipTurnButton.setEnabled(false);
            }
        });

        restartButton.addActionListener(e -> {
            if (server.commonData.gameStarted) {

                for (PlayerConnection c : server.players) {
                    c.hand.clear();

                    for (int i = 0; i < 7; i++) {
                        c.hand.add(Card.draw());
                    }

                    Collections.sort(c.hand);
                }

                server.commonData.gameStarted = true;
                server.commonData.stack = new Stack<Card>();
                server.commonData.stack.push(new Card(new Random().nextInt(9) + 1, CardSuit.random(), CardType.NUMBER));
                server.commonData.playerTurnIndex = 0;
                server.commonData.playerTurnId = server.players.get(0).getID();

                PlayersRequest.shipToClients(server.players);
            } else {
                restartButton.setEnabled(false);
            }
        });

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                logArea.append(String.valueOf((char) b));
            }
        });

        System.setOut(printStream);
        System.setErr(printStream);

        server = new PwunoServer(port);
    }
}
