package it.unitn.ds1.messages;

/**
 * Represents a message that describes the relationship between the sender Y and
 * the receiver X as Y sees it. It is sent by node Y in reply to a RESTART
 * message from node X.
 */
public class AdviseMessage extends Message {

    private boolean isXHolder;
    private boolean isXInRequestQ;
    private boolean askedY;

    /**
     * Creates an Advise Message with the information about the relationship
     * between X and Y.
     *
     * @param senderId The id of the node that sends this message.
     * @param isXHolder True if X is the holder of Y is X, false otherwise.
     * @param isXInRequestQ True if X is an element of the requestQ of Y, false
     * otherwise.
     * @param askedY True if Y has sent a REQUEST message to its holder, false
     * otherwise.
     */
    public AdviseMessage(Integer senderId, boolean isXHolder, boolean isXInRequestQ, boolean askedY) {
        super(senderId);
        this.isXHolder = isXHolder;
        this.isXInRequestQ = isXInRequestQ;
        this.askedY = askedY;
    }

    /**
     * Gets a boolean that describes if X is the holder of Y.
     *
     * @return True if X is the holder of Y, false otherwise.
     */
    public boolean isXHolder() {
        return isXHolder;
    }

    /**
     * Gets a boolean that describes if X is an element of the requestQ of Y.
     *
     * @return True if X belongs to the requestQ of Y, false otherwise.
     */
    public boolean isXInRequestQ() {
        return isXInRequestQ;
    }

    /**
     * Gets a boolean that describes if Y has sent a REQUEST message to its
     * holder
     *
     * @return True if Y has sent a REQUEST message to its holder, false
     * otherwise.
     */
    public boolean isAskedY() {
        return askedY;
    }
}
