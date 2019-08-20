package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.BufferedReader;
import java.io.IOException;

import it.unitn.ds1.messages.BootstrapMessage;
import it.unitn.ds1.messages.UserInput;
import it.unitn.ds1.network.Graph;
import it.unitn.ds1.network.Node;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import it.unitn.ds1.logger.MyLogger;

public class DistributedMutualExclusion {

    public final static int N_NODES = 10;
    public final static int REQUEST_COMMAND = 0;
    public final static int CRASH_COMMAND = 1;
    public final static int BOOTSTRAP_DELAY = 200 * N_NODES;
    public final static int CRITICAL_SECTION_TIME = 5000;
    public final static int CRASH_TIME = 5000;

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

    public static void menu(List<ActorRef> nodes) throws IOException {
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

                        UserInput msg = null;
                        if (userInput.equals("r")) {
                            System.out.println("REQUEST instruction issued to node " + nodeID);
                            msg = new UserInput(REQUEST_COMMAND);
                        } else if (userInput.equals("c")) {
                            System.out.println("CRASH instruction issued to node " + nodeID);
                            msg = new UserInput(CRASH_COMMAND);
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

        menu(nodes);
        system.terminate();
    }
}
