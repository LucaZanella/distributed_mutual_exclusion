package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Inbox;
import akka.actor.Cancellable;
import java.io.IOException;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;


public class DistributedMutualExclusion {
    final static int N_NODES= 10;

    public static class InitializeMessage implements Serializable {}

    public static class RequestMessage implements Serializable {}

    public static class PrivilegeMessage implements Serializable {}

    public static class RestartMessage implements Serializable {}

    public static class AdviseMessage implements Serializable {}

    // a message that emulates a node restart
    public static class RecoveryMessage implements Serializable {}

    public static class Node extends AbstractActor {
        protected int id;                                           // node ID
        protected List<ActorRef> neighbors;                         // list of neighbor nodes
        protected ActorRef holder;                                  // location of the privilege relative to the node itself
        protected LinkedList<ActorRef> requestQ;                    // contains the names of the neighbors that have sent a REQUEST message to the node itself
        protected boolean using;                                    // indicates if the node is executing the critical section
        protected boolean asked;                                    // indicates if the node has sent a REQUEST message to the holder
        protected boolean isRecovering;                             // indicates if the node is in recovery phase
        protected Set<ActorRef> adviseReceived;                     // set of nodes the node received an ADVISE message from

        public Node(int id, List<ActorRef> neighbors) {
            super();
            this.id = id;
            // TODO: implement a spanning tree of the computer network
            this.neighbors = neighbors;
            holder = null;
            requestQ = new LinkedList<ActorRef>();
            using = false;
            asked = false;
            isRecovering = false;
            adviseReceived = new HashSet<>();
        }

        static public Props props(int id, List<ActorRef> neighbors) {
            return Props.create(Node.class, () -> new Node(id,neighbors));
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
                .match(PrivilegeMessage.class, this::onPrivilegeMessage)
                .match(RequestMessage.class, this::onRequestMessage)
                .match(InitializeMessage.class, this::onInitializeMessage)
                .match(RecoveryMessage.class, this::onRecoveryMessage)
                .build();
        }

        public void onPrivilegeMessage(PrivilegeMessage msg) {
            holder = self();
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (isRecovering) return;
            assignPrivilege();
            makeRequest();
        }

        public void onRequestMessage(RequestMessage msg) {
            requestQ.add(getSender());
            // procedures assignPrivilege and makeRequest are not called during recovery phase
            if (isRecovering) return;
            assignPrivilege();
            makeRequest();
        }

        public void onInitializeMessage(InitializeMessage msg) {
            holder = getSender();
            for (ActorRef neighbor : neighbors) {
                neighbor.tell(new InitializeMessage(), getSelf());
            }
        }

        public void onRecoveryMessage(RecoveryMessage msg) {
            // TODO: delay for a period sufficiently long to ensure that all messages sent by node X before it failed have been received
            RestartMessage restartMessage = new RestartMessage();
            for (ActorRef neighbor : neighbors) {
                neighbor.tell(restartMessage, getSelf());
            }
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
        
    }
    
    
    public static void main(String[] args) {
        // Create the actor system
        final ActorSystem system = ActorSystem.create("helloakka");

        List<ActorRef> nodes = new ArrayList<>();
        ArrayList neighbors = new ArrayList<>(); //TODO: Now this is empty but we should define it for every node 
        for (int i=0; i<N_NODES; i++) {
            System.out.println("Setting up node " + i);
            nodes.add(system.actorOf(Node.props(i,neighbors), "Node" + i));
        }

        //TODO: Define start procedure (INIT) Where a random node is selected and it starts sending init messages

        try {
          System.out.println(">>> Press ENTER to exit <<<");
          System.in.read();
        } 
        catch (IOException ioe) {}
        system.terminate();
    }
}
