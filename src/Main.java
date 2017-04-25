import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String... args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("Usage:\njava Main <port> [<node-ip> <node-port>]\nLast two arguments are optional. If not provided, the program will assume this is the first node.");
            return;
        }

        int port = Integer.parseUnsignedInt(args[0]);

        Node node = new Node(port);
        try {
            node.start();
        } catch (UnknownHostException | NoSuchAlgorithmException  e) {
            e.printStackTrace();
            System.err.println("Could not generate host information, aborting...");
            return;
        }
    }
}
