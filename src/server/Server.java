package server;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Mailman;

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

        Mailman.init(node, port);

        try {
            LocateRegistry.getRegistry().rebind(args[0], new InitiatorPeer(node.getDistributedHashTable()));
        } catch (RemoteException e) {
            System.out.println("Could not connect to rmiregistry. TestApp will not be available on this server.");
        }

        try {
            System.out.println("Node running on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + " with id " + Integer.toUnsignedString(node.getInfo().getId()) + " and access point " + args[0] + ".");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

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
                System.out.println("Starting the process of joining the network...");
                if (!node.bootstrap(new NodeInfo(address, bootstrapPort))) {
                    System.err.println("Node bootstrapping failed. Exiting..");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Joined the network successfully.");
        node.initializeStabilization();
    }
}
