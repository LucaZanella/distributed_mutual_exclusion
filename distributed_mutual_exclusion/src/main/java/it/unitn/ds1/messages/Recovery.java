package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Represents a message sent from a node to itself to schedule the beginning
 * of the recovery from a crash.
 */
public class Recovery implements Serializable {


    /**
     * Creates a Recovery message.
     */
    public Recovery() {
    }
}
