package server;

import server.chord.Node;
import server.chord.NodeInfo;
import server.communication.Operation;
import server.dht.DistributedHashTable;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 5;

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

        DistributedHashTable<byte[]> dht = new DistributedHashTable<>(node);

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

        new Thread(() -> listenForConnections(port, dht)).start();
    }

    public static void listenForConnections(int port, DistributedHashTable<?> dht) {
        ExecutorService socketPool = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_CONNECTIONS);

        SSLServerSocket serverSocket;
        try {
            serverSocket = (SSLServerSocket)
                    SSLServerSocketFactory.getDefault().createServerSocket(port);
        } catch (IOException e) {
            System.err.println("Error creating server socket.");
            e.printStackTrace();
            return;
        }

        serverSocket.setNeedClientAuth(true);

        while (true) {
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                socketPool.submit(() -> listenForMessages(socket, dht));
            } catch (IOException e) {
                System.err.println("Error accepting socket.");
                e.printStackTrace();
            }
        }
    }

    private static void listenForMessages(SSLSocket socket, DistributedHashTable<?> dht) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            try {
                ((Operation) objectInputStream.readObject()).run(dht);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
