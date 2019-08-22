package it.unitn.ds1.messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a message sent from the main routine to all nodes in order to
 * communicate the neighbor list and the identity of the protocol starter.
 */
public class Bootstrap implements Serializable {

    private final List<ActorRef> neighbors;
    private final boolean isStarter;

    /**
     * Creates a Boostrap message with the information about the neighbors of a
     * node and if it is the starter of the protocol.
     *
     * @param neighbors The immediate neighbors in the spanning tree.
     * @param isStarter True if this node is the starter of the protocol, false
     * otherwise.
     */
    public Bootstrap(List<ActorRef> neighbors, boolean isStarter) {
        // Bootstrap message is sent only by the main routine and not from specific nodes
        this.neighbors = neighbors;
        this.isStarter = isStarter;
    }

    /**
     * Gets the immediate neighbors in the spanning tree.
     *
     * @return A list containing the immediate neighbors in the spanning tree.
     */
    public List<ActorRef> getNeighbors() {
        return neighbors;
    }

    /**
     * Gets a boolean that describes if the node that receives the message is
     * the starter of the protocol.
     *
     * @return True if the node is the starter of the protocol, false otherwise.
     */
    public boolean isStarter() {
        return isStarter;
    }
}
