package it.unitn.ds1.messages;

import java.io.Serializable;

public abstract class Message implements Serializable {

    private Integer senderId;

    public Message(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getSenderId() {
        return senderId;
    }
}
