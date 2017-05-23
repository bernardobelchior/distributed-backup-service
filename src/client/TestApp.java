package client;

import common.IInitiatorPeer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String... args) throws RemoteException {
        String peerAccessPoint = args[0];
        String operation = args[1].toUpperCase();
        String pathName;
        String hexKey;

        IInitiatorPeer initiatorPeer;

        try {
            Registry registry = LocateRegistry.getRegistry();
            initiatorPeer = (IInitiatorPeer) registry.lookup(peerAccessPoint);
        } catch (NotBoundException | RemoteException e) {
            System.out.println("Could not find connect to peer with access point: " + peerAccessPoint);
            return;
        }

        switch (operation) {
            case "BACKUP":
                if (args.length != 3) {
                    System.err.println("Invalid number of arguments for operation BACKUP.");
                    return;
                }
                pathName = args[2];

                try {
                    if (initiatorPeer.backup(pathName))
                        System.out.println("File backup successful.");
                    else
                        System.out.println("File backup failed.");
                } catch (RemoteException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "RESTORE":
                if (args.length != 4) {
                    System.err.println("Invalid number of arguments for operation RESTORE.");
                    return;
                }

                hexKey = args[2];
                pathName = args[3];
                try {
                    if (initiatorPeer.restore(hexKey, pathName))
                        System.out.println("File successfully restored.");
                    else
                        System.out.println("File recovery failed.");
                } catch (RemoteException ignored) {
                } catch (IOException e) {
                    System.err.println("Could not open file " + pathName);
                }
                break;
            case "DELETE":
                if (args.length != 3) {
                    System.err.println("Invalid number of arguments for operation DELETE.");
                    return;
                }
                hexKey = args[2];
                if (initiatorPeer.delete(hexKey))
                    System.out.println("File deletion successful.");
                else
                    System.out.println("File deletion failed.");
                break;
            case "STATE":
                System.out.println(initiatorPeer.state());
                break;
            default:
                System.out.println("Unrecognized option " + operation + ".");
                break;
        }
    }
}
