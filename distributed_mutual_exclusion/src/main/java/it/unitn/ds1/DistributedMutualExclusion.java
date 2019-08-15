package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.io.IOException;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.List;


public class DistributedMutualExclusion {
    final static int N_NODES= 10;

    /**
     * Message sent from the main routine to all nodes in order to communicate 
     * the neighbor list and the identity of the protocol starter
     */
    public static class BootstrapMessage implements Serializable {
        List<ActorRef> neighbors;
        boolean isStarter;
        public BootstrapMessage(List<ActorRef> neighbors, boolean isStarter) {
            this.neighbors = neighbors;
            this.isStarter = isStarter;
        }
    }
    
    public static class InitializeMessage implements Serializable {}

    public static class RequestMessage implements Serializable {}

    public static class PrivilegeMessage implements Serializable {}

    public static class RestartMessage implements Serializable {}

    public static class AdviseMessage implements Serializable {}

    public static class RecoveryMessage implements Serializable {
        // a message that emulates a node restart
    }

    public static class Node extends AbstractActor {
        protected int id;                                           // node ID
        protected List<ActorRef> neighbors;                         // list of neighbor nodes
        protected ActorRef holder;                                  // location of the privilege relative to the node itself
        protected LinkedList<ActorRef> requestQ;                    // contains the names of the neighbors that have sent a REQUEST message to the node itself
        protected boolean using;                                    // indicates if the node is executing the critical section
        protected boolean asked;                                    // indicates if the node has sent a REQUEST message to the holder
        protected boolean isRecovering;                             // indicates if the node is in recovery phase
        protected Set<ActorRef> adviseReceived;                     // set of nodes the node received an ADVISE message from

        public Node(int id) {
            super();
            this.id = id;
            
            holder = null;
            requestQ = new LinkedList<ActorRef>();
            using = false;
            asked = false;
            isRecovering = false;
            adviseReceived = new HashSet<>();
        }
        
        public Node(int id, List<ActorRef> neighbors) {
            this(id);
            this.neighbors = neighbors;
        }

        public void setNeighbors(List<ActorRef> neighbors) {
            this.neighbors = neighbors;
        }

        static public Props props(int id, List<ActorRef> neighbors) {
            return Props.create(Node.class, () -> new Node(id,neighbors));
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
                    // TODO: schedule node exits the critical section
                } else {
                    Serializable m = new PrivilegeMessage();
                    holder.tell(m, getSelf());
                }
            }
        }

        void makeRequest() {
            // A node can request the privilege only if it has received the INITIALIZE message
            if (holder == null) return;

            if (holder != getSelf() & !requestQ.isEmpty() & !asked) {
                holder.tell(new RequestMessage(), getSelf());
                asked = true;
            }
        }

        void initialize() {
            for (ActorRef neighbor : neighbors) {
                neighbor.tell(new InitializeMessage(), getSelf());
            }
        }

        // emulate a crash and a recovery in a given time
        void crash(int recoverIn) {
            System.out.println("CRASH!!!");
            // setting a timer to "recover"
            getContext().system().scheduler().scheduleOnce(
                Duration.create(recoverIn, TimeUnit.MILLISECONDS),
                getSelf(),
                new RecoveryMessage(), // message sent to myself
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
                .match(RecoveryMessage.class, this::onRecoveryMessage)
                .build();
        }
        
        public void onBootstrapMessage(BootstrapMessage msg) {
            System.out.println("Received a bootstrap message (Node " + this.id + ") (#Neighbors: " + msg.neighbors.size() + ")");
            this.neighbors = msg.neighbors;
            
            if(msg.isStarter){
                System.out.println("Node " + this.id  + " is the protocol starter");
                
                //TODO: implement starter behaviour
            }
        }

        public void onInitializeMessage(InitializeMessage msg) {
            holder = getSender();
            for (ActorRef neighbor : neighbors) {
                neighbor.tell(new InitializeMessage(), getSelf());
            }
        }

        public void onRequestMessage(RequestMessage msg) {
            requestQ.add(getSender());
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (isRecovering) return;
            assignPrivilege();
            makeRequest();
        }

        public void onPrivilegeMessage(PrivilegeMessage msg) {
            holder = self();
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (isRecovering) return;
            assignPrivilege();
            makeRequest();
        }

        public void onRestartMessage(RestartMessage msg) {
            // TODO: send and ADVISE message informing the recovering node of the state of the relationship with the current node
        }

        public void onAdviseMessage(AdviseMessage msg) {
            adviseReceived.add(getSender());
            // the node is in recovery phase until all ADVISE messages from each neighbor are received
            if (adviseReceived.containsAll(neighbors)) {
                // TODO: determining holder, asked and reconstruct requestQ
                adviseReceived.clear();
                isRecovering = false;
                // After the recovery phase is completed, the node recommence its participation in the algorithm
                assignPrivilege();
                makeRequest();
            }
        }
        
        
        public void onRecoveryMessage(RecoveryMessage msg) {
            // TODO: delay for a period sufficiently long to ensure that all messages sent by node X before it failed have been received
            RestartMessage restartMessage = new RestartMessage();
            for (ActorRef neighbor : neighbors) {
                neighbor.tell(restartMessage, getSelf());
            }
        }
    }
    
    /**
     * Creates an ArrayList (nodes) containing ArrayLists for neighbors
     * @param network 
     */
    public static void createStructure(ArrayList<ArrayList<Integer>> network){
        ArrayList<Integer> n = new ArrayList<>();
        
        n.clear();
        n.add(1);
        n.add(2);
        n.add(3);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(0);
        n.add(4);
        n.add(9);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(0);
        n.add(5);
        n.add(6);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(0);
        n.add(7);
        n.add(8);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(1);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(2);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(2);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(3);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(3);
        network.add((ArrayList<Integer>)n.clone());
        
        n.clear();
        n.add(1);
        network.add((ArrayList<Integer>)n.clone());
        
        /*
        To check correctness:
        
        int i = 0;
        for(ArrayList a : network){
            System.out.print("Node " + i + ": ");
            i++;
            
            for(Object v : a){
                Integer val = (Integer)v;
                System.out.print("[" + val + "] ");
            }
            System.out.println("");
        }
        */
                
    }
    
    /**
     * Defines the nodes that are part of the networks
     * @param args 
     */
    public static void main(String[] args) {
        
        // 1.Create the actor system
        final ActorSystem system = ActorSystem.create("helloakka");
        
        // 2.Instantiate the nodes
        List<ActorRef> nodes = new ArrayList<>();
        for(int i = 0; i < N_NODES; i++){
            nodes.add(system.actorOf(Node.props(i), "node" + i));
        }
        
        // 3.Define the network structure      TODO: Would it be better if we read a csv with the adjacency matrix?
        ArrayList<ArrayList<Integer>> networkStructure = new ArrayList<>();
        createStructure(networkStructure);  //Instantiates the neighbor lists and modify @networkStructure
        
        // 4. Select the starter
        int starter = 0;
        
        // 5.Tell to the nodes their neighbor lists
        for(int nodeId = 0; nodeId < N_NODES; nodeId++){
            // Get the IDs of the neighbors
            ArrayList<Integer> neighborsId = networkStructure.get(nodeId);  //List of IDs
            List<ActorRef> neighbors = new ArrayList<>();                   //List of References
            
            // For all IDs get the reference from the nodes List and add them to the list
            for(int neighborId : neighborsId){
                ActorRef neighborRef = nodes.get(neighborId);
                neighbors.add(neighborRef);
            }
            
            // Check if current node is the selected starter 
            boolean isStarter = false;
            if(nodeId == starter)
                isStarter = true;
            
            // Prepare a message with the neighbor Reference list and start flag
            BootstrapMessage start = new BootstrapMessage(neighbors,isStarter);
            // Send the bootstrap message
            nodes.get(nodeId).tell(start, null);
        }
        
        
        
        
        try {
          System.out.println(">>> Press ENTER to exit <<<");
          System.in.read();
        } 
        catch (IOException ioe) {}
        system.terminate();
    }
    
    
}
