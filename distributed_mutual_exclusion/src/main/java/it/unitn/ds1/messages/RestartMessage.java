package it.unitn.ds1.messages;


/**
 * Represents a message sent from a node to each of its neighbors to reconstruct the information,
 * required for the algorithm, that got lost after the failure of the node itself.
 */
public class RestartMessage extends Message {


    /**
     * Creates a Restart Message with the information about the sender.
     * @param senderId The id of the node that sends this message.
     */
    public RestartMessage(Integer senderId) {
        super(senderId);
    }
}
