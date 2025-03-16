package net.ludocrypt.pwuno.packets;

import com.esotericsoftware.kryonet.Client;

public class LoginRequest {
    public String playerName;

    public static void send(Client client, String playerName) {
        LoginRequest login = new LoginRequest();
        login.playerName = playerName;

        client.sendTCP(login);
    }
}
