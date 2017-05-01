package server;

import server.chord.Node;
import server.chord.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Server {
    public static void main(String... args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("Usage:\njava server.Server <port> [<node-ip> <node-port>]\n" +
                    "Last two arguments are optional. If not provided, the program will assume this is the first node.");
            return;
        }

        int port = Integer.parseUnsignedInt(args[0]);

        Node node;

        try {
            node = new Node(port);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Could not create node, aborting...");
            return;
        }

        /* Joining an existing network */
        if (args.length == 3) {
            InetAddress address;

            try {
                address = InetAddress.getByName(args[1]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }

            int bootstrapPort = Integer.parseUnsignedInt(args[2]);

            try {
                node.bootstrap(new NodeInfo(address, bootstrapPort));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
