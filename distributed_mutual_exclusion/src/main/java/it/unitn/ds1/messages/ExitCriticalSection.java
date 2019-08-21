package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Represents a message sent from a node to itself to schedule the exit from the
 * critical section.
 */
public class ExitCriticalSection implements Serializable {

    /**
     * Creates a Exit Critical Section message.
     */
    public ExitCriticalSection() {
    }
}
