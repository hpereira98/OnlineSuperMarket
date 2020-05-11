package server;

import middleware.gateway.Gateway;
import middleware.server.ServerMessageListener;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class Server {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

        // Setting socket info
        SocketInfo serverInfo = new SocketInfo("localhost", port);

        // Creating Server Message Listener
        ServerMessageListener serverMessageListener = new ServerMessageListener(serverInfo);

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("Servers", "System"));
        // Adding listener to connector
        SpreadConnector.addListener(serverMessageListener);
        // Initializing connector
        SpreadConnector.initialize();

        new Thread(Orderer.initialize(serverMessageListener)).start();

        new Gateway(port, OnlineSuperMarketSkeleton.class);

        // Sleeping
        while(true) Thread.sleep(10000);
    }
}