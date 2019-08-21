package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Represents a message that is part of Raymond's algorithm for distributed
 * mutual exclusion.
 */
public abstract class Message implements Serializable {

    private Integer senderId;

    /**
     * Creates a Message with the information about the sender.
     *
     * @param senderId The id of the node that sends this message.
     */
    public Message(Integer senderId) {
        this.senderId = senderId;
    }

    /**
     * Gets the id of the node that sent this message.
     *
     * @return An integer containing the id of the node that sent this message.
     */
    public Integer getSenderId() {
        return senderId;
    }
}
