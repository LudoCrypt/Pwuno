package net.ludocrypt.pwuno.packets;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryonet.Client;

import net.ludocrypt.pwuno.server.Card;
import net.ludocrypt.pwuno.server.PrivatePlayer;
import net.ludocrypt.pwuno.server.PwunoServer.CommonData;
import net.ludocrypt.pwuno.server.PwunoServer.PlayerConnection;

public class PlayersRequest {
    public boolean fromClient = true;
    public List<PrivatePlayer> response = new ArrayList<PrivatePlayer>();
    public CommonData commonData;

    public static void request(Client client) {
        PlayersRequest request = new PlayersRequest();
        client.sendTCP(request);
    }

    public static void ship(PlayerConnection connection, List<PlayerConnection> players, CommonData commonData) {
        PlayersRequest response = new PlayersRequest();
        response.commonData = commonData;

        for (PlayerConnection c : players) {

            List<Card> hand = new ArrayList<Card>();

            if (c.getID() == connection.getID()) {
                hand = c.hand;
            }

            response.response.add(new PrivatePlayer(c.getID(), c.playerName, hand, c.hand.size()));
        }

        connection.sendTCP(response);
    }

    public static void shipToClients(List<PlayerConnection> players) {
        for (PlayerConnection c : players) {
            PlayersRequest request = new PlayersRequest();
            request.fromClient = false;
            c.sendTCP(request);
        }
    }
}