package it.unitn.ds1.messages;

import java.io.Serializable;

/**
 * Represents a message sent from the user to a node to issue a command.
 */
public class UserInput implements Serializable {

    private int commandId;

    /**
     * Creates a User Input message with the information about the command to
     * issue.
     *
     * @param commandId The id of the command to be issued to the node.
     */
    public UserInput(int commandId) {
        this.commandId = commandId;
    }

    /**
     * Gets the id of the command to be issued to the node.
     *
     * @return An integer containing the id of the command to be issued to the
     * node.
     */
    public int getCommandId() {
        return commandId;
    }
}
