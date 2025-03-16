package net.ludocrypt.pwuno.packets;

import com.esotericsoftware.kryonet.Client;

public class PlayCardRequest {

    public int card;

    public static void request(Client client, int card) {
        PlayCardRequest request = new PlayCardRequest();
        request.card = card;
        client.sendTCP(request);
    }

}
