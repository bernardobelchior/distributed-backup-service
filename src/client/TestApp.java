package client;

import common.IInitiatorPeer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String... args) {
        String peerAccessPoint = args[0];
        String operation = args[1].toUpperCase();
        String pathName;

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
                pathName = args[2];

                try {
                    if (initiatorPeer.backup(pathName))
                        System.out.println("File backup successful.");
                    else
                        System.out.println("File backup failed.");
                } catch (RemoteException ignored) {
                } catch (IOException e) {
                    System.err.println("Could not open file " + pathName);
                }
                break;
            case "RESTORE":
                pathName = args[2];
                try {
                    if (initiatorPeer.restore(pathName))
                        System.out.println("File successfully restored.");
                    else
                        System.out.println("File recovery failed.");
                } catch (RemoteException ignored) {
                }catch (IOException e) {
                    System.err.println("Could not open file " + pathName);
                }
                break;
            case "DELETE":
                pathName = args[2];
                try {
                    if (initiatorPeer.delete(pathName))
                        System.out.println("File deletion successful.");
                    else
                        System.out.println("File deletion failed.");
                } catch (RemoteException ignored) {
                }catch (IOException e) {
                    System.err.println("Could not open file " + pathName);
                }
                break;
            case "STATE":
                try {
                    System.out.println(initiatorPeer.state());
                } catch (RemoteException ignored) {
                }
                break;
            default:
                System.out.println("Unrecognized option " + operation + ".");
                break;
        }
    }
}
