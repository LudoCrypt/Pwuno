package net.ludocrypt.pwuno.packets;

import com.esotericsoftware.kryonet.Client;

public class DrawCardRequest {

    public static void request(Client client) {
        DrawCardRequest request = new DrawCardRequest();
        client.sendTCP(request);
    }

}
