package it.unitn.ds1.network;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.messages.*;
import scala.concurrent.duration.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static it.unitn.ds1.DistributedMutualExclusion.CRITICAL_SECTION_TIME;
import static it.unitn.ds1.DistributedMutualExclusion.BOOTSTRAP_DELAY;
import static it.unitn.ds1.DistributedMutualExclusion.CRASH_TIME;
import static it.unitn.ds1.DistributedMutualExclusion.REQUEST_COMMAND;
import static it.unitn.ds1.DistributedMutualExclusion.CRASH_COMMAND;

public class Node extends AbstractActor {

    private int id;                                                       // node ID
    private List<ActorRef> neighbors = null;                              // list of neighbor nodes
    private ActorRef holder = null;                                       // location of the privilege relative to the node itself
    private LinkedList<ActorRef> requestQ = new LinkedList<ActorRef>();   // contains the names of the neighbors that have sent a REQUEST message to the node itself
    private Boolean using = false;                                        // indicates if the node is executing the critical section
    private Boolean asked = false;                                        // indicates if the node has sent a REQUEST message to the holder
    private Boolean isRecovering = false;                                 // indicates if the node is in recovery phase
    private HashMap<ActorRef, AdviseMessage> adviseMessages = new HashMap<>();
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public Node(int id) {
        super();
        this.id = id;
    }

    static public Props props(int id) {
        return Props.create(Node.class, () -> new Node(id));
    }

    private void assignPrivilege() {
        if (holder.equals(getSelf()) & !using & !requestQ.isEmpty()) {
            holder = requestQ.remove();
            asked = false;
            if (holder.equals(getSelf())) {
                using = true;

                LOGGER.setLevel(Level.INFO);
                LOGGER.info("Node " + this.id + " ENTER critical section...");

                getContext().system().scheduler().scheduleOnce(Duration.create(CRITICAL_SECTION_TIME, TimeUnit.MILLISECONDS),
                        getSelf(),
                        new ExitCriticalSection(),
                        getContext().system().dispatcher(), getSelf()
                );

            } else {
                PrivilegeMessage m = new PrivilegeMessage(this.id);
                holder.tell(m, getSelf());
            }
        }
    }

    private void makeRequest() {
        LOGGER.setLevel(Level.INFO);
        // A node can request the privilege only if it has received the INITIALIZE message
        if (holder == null) {
            LOGGER.severe("Node " + id + " is trying to request the PRIVILEGE but has not received the INITIALIZE message");
            // TODO: Should the code return here? Otherwise the execution will go on
        }

        if (holder != getSelf() & !requestQ.isEmpty() & !asked) {
            holder.tell(new RequestMessage(this.id), getSelf());
            asked = true;
        }
    }

    private void initialize() {
        // We only schedule an init message to the starter itself to account
        //for setup time
        getContext().system().scheduler().scheduleOnce(
                Duration.create(BOOTSTRAP_DELAY, TimeUnit.MILLISECONDS),
                getSelf(),
                new InitializeMessage(this.id),
                getContext().system().dispatcher(), getSelf()
        );
    }

    // emulate a crash and a recovery in a given time
    private void crash(int recoverIn) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Node " + id + " CRASHED");
        // setting a timer to "recover"

        //TODO: See what variables we lose oltre a queste
        this.holder = null;
        this.using = null;
        this.asked = null;
        this.requestQ.clear();

        getContext().system().scheduler().scheduleOnce(Duration.create(recoverIn, TimeUnit.MILLISECONDS),
                getSelf(),
                new Recovery(), // message sent to myself
                getContext().system().dispatcher(), getSelf()
        );
    }

    @java.lang.Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(BootstrapMessage.class, this::onBootstrapMessage)
                .match(InitializeMessage.class, this::onInitializeMessage)
                .match(RequestMessage.class, this::onRequestMessage)
                .match(PrivilegeMessage.class, this::onPrivilegeMessage)
                .match(RestartMessage.class, this::onRestartMessage)
                .match(AdviseMessage.class, this::onAdviseMessage)
                .match(Recovery.class, this::onRecovery)
                .match(UserInput.class, this::onUserInput)
                .match(ExitCriticalSection.class, this::onExitCriticalSection)
                .build();
    }

    private void onBootstrapMessage(BootstrapMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("BOOTSTRAP message received by node " + id + ". Node " + id + " has: " + msg.getNeighbors().size() + " neighbors");

        this.neighbors = msg.getNeighbors();

        if (msg.isStarter()) {
            LOGGER.info("STARTER of the protocol is node " + id);

            initialize();
        }
    }

    private void onInitializeMessage(InitializeMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("INITIALIZE message received by node " + this.id + " from node " + msg.getSenderId());

        ActorRef sender = getSender();
        if (sender == null) {
            // TODO: Can we remove this block of code?
            LOGGER.severe("The sender of the INITIALIZE message received by node" + this.id + " is NULL");
        }
        holder = getSender();
        for (ActorRef neighbor : neighbors) {
            if (neighbor != holder) {
                neighbor.tell(new InitializeMessage(this.id), getSelf());
            }
        }

    }

    private void onRequestMessage(RequestMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("REQUEST message received by node " + id + " from node " + msg.getSenderId());

        requestQ.add(getSender());
        // procedures assignPrivilege and makeRequest are not called during recovery phase
        if (!isRecovering) {
            assignPrivilege();
            makeRequest();
        }
    }

    private void onPrivilegeMessage(PrivilegeMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("PRIVILEGE message received by node " + id + " from node " + msg.getSenderId());

        holder = self();
        // procedures assignPrivilege and makeRequest are not called during recovery phase
        if (!isRecovering) {
            assignPrivilege();
            makeRequest();
        }
    }

    private void onRestartMessage(RestartMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("RESTART message received by node " + id + " from node " + msg.getSenderId());

        // DONE: send and ADVISE message informing the recovering node of the state of the relationship with the current node

        boolean isXInRequestQ = this.requestQ.contains(getSender());
        boolean isXHolder = this.holder == getSender();

        getSender().tell(new AdviseMessage(id, isXHolder, isXInRequestQ, asked), getSelf());
    }

    private void onAdviseMessage(AdviseMessage msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("ADVISE message received by node " + id + " from node " + msg.getSenderId());

        adviseMessages.put(getSender(), msg);

        // the node is in recovery phase until all ADVISE messages from each neighbor are received
        if (adviseMessages.keySet().containsAll(neighbors)) {
            String requestQIds = "[";
            Integer holderId;
            // All advise messages have been received
            // Recovery procedure

            // 1.determining holder, ASKED and USING
            this.using = false;
            this.holder = getSelf();
            holderId = id;
            asked = false;
            for (ActorRef neighbor : adviseMessages.keySet()) {
                AdviseMessage currentMsg = adviseMessages.get(neighbor);

                if (!currentMsg.isXHolder()) {
                    // It means that THIS node is not privileged
                    this.holder = neighbor;
                    holderId = currentMsg.getSenderId();
                    if (currentMsg.isXInRequestQ())
                        this.asked = true;
                } else {
                    // 2. Reconstruct Request Queue
                    if (currentMsg.isAskedY()) {
                        requestQ.add(neighbor);
                        requestQIds += currentMsg.getSenderId() + ", ";
                    }
                }
            }
            if (!requestQ.isEmpty()) {
                requestQIds = requestQIds.substring(0, requestQIds.length() - 2);
            }
            requestQIds += "]";

            adviseMessages.clear();
            isRecovering = false;

            LOGGER.info("Node " + id + " has completed RECOVERY. " +
                    "Holder: " + holderId + ", " +
                    "Asked: " + asked + ", " +
                    "RequestQ: " + requestQIds + ", " +
                    "Using: " + using);

            // After the recovery phase is completed, the node recommence its participation in the algorithm
            assignPrivilege();
            makeRequest();
        }
    }

    private void onUserInput(UserInput msg) {
        LOGGER.setLevel(Level.INFO);

        switch (msg.getCommandId()) {
            case REQUEST_COMMAND:
                LOGGER.info("REQUEST command received by node " + id + " from user");
                requestQ.add(self());
                assignPrivilege();
                makeRequest();
                break;
            case CRASH_COMMAND:
                LOGGER.info("CRASH command received by node " + id + " from user");
                crash(CRASH_TIME);
                break;
        }
    }

    private void onExitCriticalSection(ExitCriticalSection msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Node " + this.id + " EXIT critical section...");

        this.using = false;
        assignPrivilege();
        makeRequest();
    }

    private void onRecovery(Recovery msg) {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Node " + this.id + " starts RECOVERY");

        /* We assume that the crash lasts long enough to guarantee that all
         * messages sent before crashing are received by all nodes.
         * TODO: check if we need to wait for messages to be received
         */
        this.isRecovering = true;
        RestartMessage restartMessage = new RestartMessage(this.id);
        for (ActorRef neighbor : neighbors) {
            neighbor.tell(restartMessage, getSelf());
        }
    }
}
