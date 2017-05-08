package server.communication;

import server.chord.NodeInfo;
import server.dht.DistributedHashTable;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;

public class LookupOperation<T> implements Operation<T> {
    private BigInteger key;


    private NodeInfo origin;

    public LookupOperation(NodeInfo origin, BigInteger key) {
        this.origin = origin;
        this.key = key;
    }

    @Override
    public void run(DistributedHashTable<T> dht) {
        dht.lookup(this);
        if (dht.inRangeOfCurrentNode(key)) {
            try {
                Mailman.addOpenConnection(new Connection(origin));
            } catch (IOException e) {
                System.err.println("Could not open connection to origin node in Lookup for key " + DatatypeConverter.printHexBinary(key.toByteArray()));
                e.printStackTrace();
            }
        }
    }

    public BigInteger getKey() {
        return key;
    }

    public NodeInfo getOrigin() {
        return origin;
    }
}
