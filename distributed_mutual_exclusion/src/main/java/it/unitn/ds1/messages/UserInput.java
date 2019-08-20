package it.unitn.ds1.messages;

import java.io.Serializable;

public class UserInput implements Serializable {

    private int commandId;

    public UserInput(int commandId) {
        this.commandId = commandId;
    }

    public int getCommandId() {
        return commandId;
    }
}
