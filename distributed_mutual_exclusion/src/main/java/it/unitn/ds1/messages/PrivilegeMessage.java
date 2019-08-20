package it.unitn.ds1.messages;

/**
 * Represents a token whose possession allows to enter the critical section.
 * It is a message sent from the node that holds the privilege to one of its
 * neighbors who has requested the privilege.
 */
public class PrivilegeMessage extends Message {


    /**
     * Creates a Privilege Message with the information about the sender.
     * @param senderId The id of the node that sends this message.
     */
    public PrivilegeMessage(Integer senderId) {
        super(senderId);
    }
}
