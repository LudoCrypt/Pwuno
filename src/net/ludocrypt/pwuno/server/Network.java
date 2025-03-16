package net.ludocrypt.pwuno.server;

import java.util.ArrayList;
import java.util.Stack;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import net.ludocrypt.pwuno.packets.ChooseSuitRequest;
import net.ludocrypt.pwuno.packets.DrawCardRequest;
import net.ludocrypt.pwuno.packets.LoginRequest;
import net.ludocrypt.pwuno.packets.LogoutRequest;
import net.ludocrypt.pwuno.packets.PlayCardRequest;
import net.ludocrypt.pwuno.packets.PlayersRequest;
import net.ludocrypt.pwuno.server.Card.CardSerializer;
import net.ludocrypt.pwuno.server.PwunoServer.CommonData;

public class Network {

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        kryo.register(LoginRequest.class);
        kryo.register(LogoutRequest.class);
        kryo.register(PlayersRequest.class);
        kryo.register(PlayCardRequest.class);
        kryo.register(DrawCardRequest.class);
        kryo.register(ChooseSuitRequest.class);

        kryo.register(Stack.class);
        kryo.register(ArrayList.class);
        kryo.register(CommonData.class);
        kryo.register(PrivatePlayer.class);
        kryo.register(Card.class, new CardSerializer());
    }

}
