package it.unitn.ds1.messages;

import akka.actor.ActorRef;
import java.util.List;

/**
 * Message sent from the main routine to all nodes in order to communicate
 * the neighbor list and the identity of the protocol starter
 */
public class BootstrapMessage extends Message {

    private List<ActorRef> neighbors;
    private boolean isStarter;

    public BootstrapMessage(List<ActorRef> neighbors, boolean isStarter) {
        // Bootstrap message is sent only by the main routine and not from specific nodes
        super(null);
        this.neighbors = neighbors;
        this.isStarter = isStarter;
    }

    public List<ActorRef> getNeighbors() {
        return neighbors;
    }

    public boolean isStarter() {
        return isStarter;
    }
}
