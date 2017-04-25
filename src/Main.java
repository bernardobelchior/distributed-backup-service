import chord.Node;
import chord.NodeInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String... args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("Usage:\njava Main <port> [<node-ip> <node-port>]\nLast two arguments are optional. If not provided, the program will assume this is the first node.");
            return;
        }

        int port = Integer.parseUnsignedInt(args[0]);

        Node node;
        try {
            node = new Node(port);
        } catch (UnknownHostException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println("Could not create node, aborting...");
            return;
        }

        if (args.length == 3) {
            InetAddress address;

            try {
                address = InetAddress.getByName(args[2]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }

            int bootstrapPort = Integer.parseUnsignedInt(args[3]);
            try {
                node.bootstrap(new NodeInfo(address, bootstrapPort));
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
