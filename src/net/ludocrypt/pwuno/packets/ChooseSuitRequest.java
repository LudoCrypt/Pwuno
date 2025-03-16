package net.ludocrypt.pwuno.packets;

import com.esotericsoftware.kryonet.Client;

public class ChooseSuitRequest {

    public int suit;

    public static void request(Client client, int suit) {
        ChooseSuitRequest request = new ChooseSuitRequest();
        request.suit = suit;
        client.sendTCP(request);
    }

}
