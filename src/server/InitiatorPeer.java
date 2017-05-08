package server;

import common.IInitiatorPeer;
import server.dht.DistributedHashTable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class InitiatorPeer extends UnicastRemoteObject implements IInitiatorPeer {
    private final DistributedHashTable<?> dht;

    InitiatorPeer(DistributedHashTable<?> dht) throws RemoteException {
        super();
        this.dht = dht;
    }

    @Override
    public boolean backup(String pathName, int replicationDegree) {
        return false;
    }

    @Override
    public boolean restore(String filename) {
        return false;
    }

    @Override
    public boolean delete(String filename) {
        return false;
    }

    @Override
    public String state() {
        return dht.getState();
    }
}
