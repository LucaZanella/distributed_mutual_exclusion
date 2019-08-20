package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.io.BufferedReader;
import java.io.IOException;

import scala.concurrent.duration.Duration;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import it.unitn.ds1.logger.MyLogger;

public class DistributedMutualExclusion {

    final static int N_NODES = 10;
    final static int BOOTSTRAP_DELAY = 200 * N_NODES;
    final static int CRITICAL_SECTION_TIME = 5000;
    final static int CRASH_TIME = 5000;

    final static int REQUEST_COMMAND = 0;
    final static int CRASH_COMMAND = 1;

    // use the classname for the logger, this way you can refactor
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static class Message implements Serializable {

        Integer senderId;

        public Message(Integer senderId) {
            this.senderId = senderId;
        }
    }

    /**
     * Message sent from the main routine to all nodes in order to communicate
     * the neighbor list and the identity of the protocol starter
     */
    public static class BootstrapMessage extends Message implements Serializable {

        List<ActorRef> neighbors;
        boolean isStarter;

        public BootstrapMessage(List<ActorRef> neighbors, boolean isStarter) {
            //Bootstrap message is sent only by the main routine and not from 
            //specific nodes
            super(null);
            this.neighbors = neighbors;
            this.isStarter = isStarter;
        }
    }

    public static class InitializeMessage extends Message implements Serializable {

        public InitializeMessage(Integer senderId) {
            super(senderId);
        }
    }

    public static class RequestMessage extends Message implements Serializable {

        public RequestMessage(Integer senderId) {
            super(senderId);
        }
    }

    public static class PrivilegeMessage extends Message implements Serializable {

        public PrivilegeMessage(Integer senderId) {
            super(senderId);
        }
    }

    public static class RestartMessage extends Message implements Serializable {

        public RestartMessage(Integer senderId) {
            super(senderId);
        }
    }

    public static class AdviseMessage extends Message implements Serializable {

        boolean isXHolder;
        boolean isXInRequestQ;
        boolean askedY;

        public AdviseMessage(boolean isXHolder, boolean isXInRequestQ, boolean askedY, Integer senderId) {
            super(senderId);
            this.isXHolder = isXHolder;
            this.isXInRequestQ = isXInRequestQ;
            this.askedY = askedY;
        }
    }

    public static class Recovery extends Message implements Serializable {

        public Recovery(Integer senderId) {
            super(senderId);
        }
    }

    public static class UserInputMessage implements Serializable {

        protected int commandId;

        public UserInputMessage(int commandId) {
            this.commandId = commandId;
        }
    }

    public static class ExitCriticalSection implements Serializable {
    }

    public static class Node extends AbstractActor {

        protected int id;                                                       // node ID
        protected List<ActorRef> neighbors = null;                              // list of neighbor nodes
        protected ActorRef holder = null;                                       // location of the privilege relative to the node itself
        protected LinkedList<ActorRef> requestQ = new LinkedList<ActorRef>();   // contains the names of the neighbors that have sent a REQUEST message to the node itself
        protected Boolean using = false;                                        // indicates if the node is executing the critical section
        protected Boolean asked = false;                                        // indicates if the node has sent a REQUEST message to the holder
        protected Boolean isRecovering = false;                                 // indicates if the node is in recovery phase
        protected HashMap<ActorRef,AdviseMessage> adviseMessages = new HashMap<>();

        public Node(int id) {
            super();
            this.id = id;
        }

        static public Props props(int id) {
            return Props.create(Node.class, () -> new Node(id));
        }

        void assignPrivilege() {
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

        void makeRequest() {
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

        void initialize() {
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
        void crash(int recoverIn) {
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
                    new Recovery(this.id), // message sent to myself
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
                    .match(UserInputMessage.class, this::onUserInputMessage)
                    .match(ExitCriticalSection.class, this::onExitCriticalSection)
                    .build();
        }

        public void onBootstrapMessage(BootstrapMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("BOOTSTRAP message received by node " + id + ". Node " + id + " has: " + msg.neighbors.size() + " neighbors");

            this.neighbors = msg.neighbors;

            if (msg.isStarter) {
                LOGGER.info("STARTER of the protocol is node " + id);

                initialize();
            }
        }

        public void onInitializeMessage(InitializeMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("INITIALIZE message received by node " + this.id + " from node " + msg.senderId);

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

        public void onRequestMessage(RequestMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("REQUEST message received by node " + id + " from node " + msg.senderId);

            requestQ.add(getSender());
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (!isRecovering) {
                assignPrivilege();
                makeRequest();
            }
        }

        public void onPrivilegeMessage(PrivilegeMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("PRIVILEGE message received by node " + id + " from node " + msg.senderId);

            holder = self();
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (!isRecovering) {
                assignPrivilege();
                makeRequest();
            }
        }

        public void onRestartMessage(RestartMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("RESTART message received by node " + id + " from node " + msg.senderId);

            // DONE: send and ADVISE message informing the recovering node of the state of the relationship with the current node
            
            boolean isXInRequestQ = this.requestQ.contains(getSender());
            boolean isXHolder = this.holder == getSender();

            getSender().tell(new AdviseMessage(isXHolder, isXInRequestQ, asked, id), getSelf());
        }

        public void onAdviseMessage(AdviseMessage msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("ADVISE message received by node " + id + " from node " + msg.senderId);

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
                    
                    if (!currentMsg.isXHolder) {
                        // It means that THIS node is not privileged
                        this.holder = neighbor;
                        holderId = currentMsg.senderId;
                        if (currentMsg.isXInRequestQ)
                            this.asked = true;
                    } else {
                        // 2. Reconstruct Request Queue
                        if (currentMsg.askedY) {
                            requestQ.add(neighbor);
                            requestQIds += currentMsg.senderId + ", ";
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

        public void onUserInputMessage(UserInputMessage msg) {
            LOGGER.setLevel(Level.INFO);

            switch (msg.commandId) {
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

        public void onExitCriticalSection(ExitCriticalSection msg) {
            LOGGER.setLevel(Level.INFO);
            LOGGER.info("Node " + this.id + " EXIT critical section...");

            this.using = false;
            assignPrivilege();
            makeRequest();
        }

        public void onRecovery(Recovery msg) {
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

    public static class Graph {

        ArrayList<ArrayList<Integer>> adj;
        int V;

        Graph(int V) {
            this.V = V;
            adj = new ArrayList<ArrayList<Integer>>(V);
            for (int i = 0; i < V; i++) {
                adj.add(new ArrayList<Integer>());
            }
        }

        void addEdge(int u, int v) {
            adj.get(u).add(v);
            adj.get(v).add(u);
        }

        ArrayList<Integer> getAdjacencyList(int u) {
            return adj.get(u);
        }

        void printAdjacencyList() {
            for (int i = 0; i < adj.size(); i++) {
                System.out.print("Adjacency list of " + i + ": [");
                for (int j = 0; j < adj.get(i).size(); j++) {
                    System.out.print(adj.get(i).get(j));
                    if (j != adj.get(i).size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");
            }
        }
    }

    /**
     * Creates an ArrayList (nodes) containing ArrayLists for neighbors
     */
    public static Graph createStructure() {
        // Creating graph with N_NODES vertices
        Graph g = new Graph(N_NODES);

        // Adding edges one by one
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(0, 3);
        g.addEdge(1, 4);
        g.addEdge(1, 9);
        g.addEdge(2, 5);
        g.addEdge(2, 6);
        g.addEdge(3, 7);
        g.addEdge(3, 8);

        return g;
    }

    /**
     * Defines the nodes that are part of the networks
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {

        try {
            MyLogger.setup();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problems with creating the log files");
        }

        // 1.Create the actor system
        final ActorSystem system = ActorSystem.create("helloakka");

        // 2.Instantiate the nodes
        List<ActorRef> nodes = new ArrayList<>();
        for (int i = 0; i < N_NODES; i++) {
            nodes.add(system.actorOf(Node.props(i), "node" + i));
        }

        // 3.Define the network structure      TODO: Would it be better if we read a csv with the adjacency matrix?
        Graph g = createStructure();  // Instantiates the neighbor lists and modify @networkStructure
        g.printAdjacencyList();

        // 4.Select the starter
        int starter = 0;

        // 5.Tell to the nodes their neighbor lists
        for (int nodeId = 0; nodeId < N_NODES; nodeId++) {
            // Get the IDs of the neighbors
            ArrayList<Integer> neighborsId = g.getAdjacencyList(nodeId);    // List of neighbors ID
            List<ActorRef> neighbors = new ArrayList<>();                   // List of neighbors

            for (int neighborId : neighborsId) {
                ActorRef neighbor = nodes.get(neighborId);
                neighbors.add(neighbor);
            }

            // Check if current node is the selected starter
            boolean isStarter = false;
            if (nodeId == starter) {
                isStarter = true;
            }

            // Prepare a message with the neighbor Reference list and start flag
            BootstrapMessage start = new BootstrapMessage(neighbors, isStarter);
            // Send the bootstrap message
            nodes.get(nodeId).tell(start, null);
        }

        // 6.Handle command line input
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        boolean close = false;
        String out = "Enter:\n"
                + "- 'r' to send a request\n"
                + "- 'c' to crash a node\n"
                + "- 'q' to quit\n"
                + "Your choice:";

        do {
            System.out.println(out);

            String userInput = in.readLine();
            if (userInput.equals("q")) {
                close = true;
            } else if (userInput.equals("r") | userInput.equals("c")) {
                System.out.println("Enter the node's ID:");

                boolean readValidNumber = false;
                while (!readValidNumber) {
                    try {
                        int nodeID = Integer.parseInt(in.readLine());
                        if (!(nodeID < N_NODES & nodeID >= 0)) {
                            throw new IllegalArgumentException();
                        }
                        readValidNumber = true;

                        UserInputMessage msg = null;
                        if (userInput.equals("r")) {
                            System.out.println("REQUEST instruction issued to node " + nodeID);
                            msg = new UserInputMessage(REQUEST_COMMAND);
                        } else if (userInput.equals("c")) {
                            System.out.println("CRASH instruction issued to node " + nodeID);
                            msg = new UserInputMessage(CRASH_COMMAND);
                        }
                        nodes.get(nodeID).tell(msg, null);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Incorrect ID number. Please enter an integer value between 0 and " + (N_NODES - 1));
                    }
                }
            } else {
                System.out.println("Unknown command. Please try again");
            }
        } while (!close);
        System.out.println("Closing application...");
        in.close();
        system.terminate();
    }
}
