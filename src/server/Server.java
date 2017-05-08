package server;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;
import server.dht.DistributedHashTable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.NoSuchAlgorithmException;

public class Server {

    public static void main(String... args) {
        if (args.length != 2 && args.length != 4) {
            System.err.println("Usage:\njava server.Server <access-point> <port> [<node-ip> <node-port>]\n" +
                    "Last two arguments are optional. If not provided, the program will assume this is the first node.");
            return;
        }


        int port = Integer.parseUnsignedInt(args[1]);

        Node node;

        try {
            node = new Node(port);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Could not create node, aborting...");
            return;
        }

        DistributedHashTable<byte[]> dht = new DistributedHashTable<>(node);
        Mailman.init(port, dht);

        /* Joining an existing network */
        if (args.length == 4) {
            InetAddress address;

            try {
                address = InetAddress.getByName(args[2]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }

            int bootstrapPort = Integer.parseUnsignedInt(args[3]);

            try {
                dht.bootstrapNode(new NodeInfo(address, bootstrapPort));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            /* TODO: Start the stabilization process.
            * I should do it because I am the first and this ensures that the stabilization process only runs once. */
        }

        try {
            LocateRegistry.getRegistry().rebind(args[0], new InitiatorPeer(dht));
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

}
