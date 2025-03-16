package net.ludocrypt.pwuno.server;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;

public class PwunoClient {

    private Client client;

    public PwunoClient(String ip, Listener listener) throws IOException {
        client = new Client();
        Network.register(client);

        client.addListener(listener);

        client.start();
        client.connect(5000, ip.substring(0, ip.indexOf(":")), Integer.parseInt(ip.substring(ip.indexOf(":") + 1, ip.length())));
    }

    public Client getClient() {
        return client;
    }

}
