package it.unitn.ds1.messages;

/**
 * Represents a message sent from a node to its holder to show the intention to get
 * the privilege either for itself or others.
 */
public class RequestMessage extends Message {


    /**
     * Creates a Request Message with the information about the sender
     * @param senderId The id of the node that sends this message.
     */
    public RequestMessage(Integer senderId) {
        super(senderId);
    }
}
