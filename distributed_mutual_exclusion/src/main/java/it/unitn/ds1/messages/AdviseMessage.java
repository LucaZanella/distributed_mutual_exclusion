package it.unitn.ds1.messages;

public class AdviseMessage extends Message {

    private boolean isXHolder;
    private boolean isXInRequestQ;
    private boolean askedY;

    public AdviseMessage(Integer senderId, boolean isXHolder, boolean isXInRequestQ, boolean askedY) {
        super(senderId);
        this.isXHolder = isXHolder;
        this.isXInRequestQ = isXInRequestQ;
        this.askedY = askedY;
    }

    public boolean isXHolder() {
        return isXHolder;
    }

    public boolean isXInRequestQ() {
        return isXInRequestQ;
    }

    public boolean isAskedY() {
        return askedY;
    }
}
