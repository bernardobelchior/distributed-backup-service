package server;

import common.IInitiatorPeer;

public class InitiatorPeer implements IInitiatorPeer {
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
    public boolean state() {
        return false;
    }
}
