package it.unitn.ds1.messages;

/**
 * Represents a message sent from a node to its neighbors to initialize the algorithm.
 * The first Initialize Message is sent from the node designed as the initial privileged.
 */
public class InitializeMessage extends Message {

    /**
     * Creates a Initialize Message with the information about the sender.
     * @param senderId The id of the node that sends this message.
     */
    public InitializeMessage(Integer senderId) {
        super(senderId);
    }
}
