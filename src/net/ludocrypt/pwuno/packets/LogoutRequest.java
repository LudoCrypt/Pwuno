package net.ludocrypt.pwuno.packets;

import net.ludocrypt.pwuno.server.PwunoServer.PlayerConnection;

public class LogoutRequest {

    public static void send(PlayerConnection c) {
        LogoutRequest logout = new LogoutRequest();
        c.sendTCP(logout);
    }
}
