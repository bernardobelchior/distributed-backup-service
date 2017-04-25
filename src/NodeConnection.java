import java.io.IOException;
import java.net.Socket;

public class NodeConnection {
    Socket socket;

    NodeConnection(String address, int port) throws IOException {
        socket = new Socket(address, port);
    }
}
