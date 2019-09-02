package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import it.unitn.ds1.messages.Bootstrap;
import it.unitn.ds1.messages.UserInput;
import it.unitn.ds1.network.Graph;
import it.unitn.ds1.network.Node;
import it.unitn.ds1.logger.MyLogger;

/**
 * Represents the class where the actor system is created and the user interface
 * is displayed to the user.
 */
public class DistributedMutualExclusion {

    /**
     * The number of nodes in the network.
     */
    public final static int N_NODES = 10;
    /**
     * The identifier of the node chosen as the initial privileged
     */
    public static final int INITIAL_PRIVILEGED_NODE_ID = 0;
    /**
     * The identifier of the command to issue a request to a node.
     */
    public final static int REQUEST_COMMAND = 0;
    /**
     * The identifier of the command to issue a crash to a node.
     */
    public final static int CRASH_COMMAND = 1;
    /**
     * Number of milliseconds the starter has to wait before beginning the
     * initialization of the protocol.
     */
    public final static int BOOTSTRAP_DELAY = 200 * N_NODES;
    /**
     * Number of milliseconds a node is within the critical section.
     */
    public final static int CRITICAL_SECTION_TIME = 15000;
    /**
     * Number of milliseconds between the crash and the recovery.
     */
    public final static int CRASH_TIME = 15000;
    /**
     * The file path containing the commands to be executed.
     */
    public final static String COMMANDS_FILENAME = "commands.txt";

    /**
     * Creates a tree topology of the network. (See image Tree1)
     */
    public static Graph createStructureTree1() {
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
     * Creates a line topology (see image Line)
     */
    public static Graph createStructureLine() {
        // Creating graph with N_NODES vertices
        Graph g = new Graph(N_NODES);

        // Adding edges one by one
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        g.addEdge(5, 6);
        g.addEdge(6, 7);
        g.addEdge(7, 8);
        g.addEdge(8, 9);

        return g;
    }

    /**
     * Creates a (binary) tree topology (see image Tree2)
     */
    public static Graph createStructureTree2() {
        // Creating graph with N_NODES vertices
        Graph g = new Graph(N_NODES);

        // Adding edges one by one
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 3);
        g.addEdge(1, 4);
        g.addEdge(2, 5);
        g.addEdge(2, 6);
        g.addEdge(3, 7);
        g.addEdge(3, 8);
        g.addEdge(4, 9);

        return g;
    }

    /**
     * Creates a unbalanced tree topology (see image Tree3)
     */
    public static Graph createStructureTree3() {
        // Creating graph with N_NODES vertices
        Graph g = new Graph(N_NODES);

        // Adding edges one by one
        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(0, 3);
        g.addEdge(3, 4);
        g.addEdge(3, 5);
        g.addEdge(3, 6);
        g.addEdge(3, 7);
        g.addEdge(3, 8);
        g.addEdge(3, 9);

        return g;
    }

    /**
     * Checks if the identifier of the node is valid.
     *
     * @param nodeId The identifier of the node.
     * @throws IllegalArgumentException If the identifier of the node is not valid.
     */
    public static void checkId(int nodeId) throws IllegalArgumentException {
        if (!(nodeId < N_NODES & nodeId >= 0)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the UserInput message using the information about the command.
     *
     * @param command A string representing the command.
     * @return A UserInput message containing that command.
     * @throws IllegalArgumentException If the command is not valid.
     */
    public static UserInput getCommand(String command) throws IllegalArgumentException {
        UserInput msg = null;
        if (command.equals("r")) {
            msg = new UserInput(REQUEST_COMMAND);
        } else if (command.equals("c")) {
            msg = new UserInput(CRASH_COMMAND);
        } else {
            throw new IllegalArgumentException();
        }

        return msg;
    }

    /**
     * Prints on the console the command issued to a node.
     *
     * @param msg The UserInput message containing the command.
     * @param nodeId The identifier of the node to which the command will be issued.
     */
    public static void printCommand(UserInput msg, int nodeId) {
        if (msg.getCommandId() == REQUEST_COMMAND) {
            System.out.println("REQUEST instruction issued to node " + nodeId);
        } else if (msg.getCommandId() == CRASH_COMMAND) {
            System.out.println("CRASH instruction issued to node " + nodeId);
        }
    }

    /**
     * Displays the user interface.
     *
     * @param nodes List of actors representing the nodes of the network
     * @throws IOException
     */
    public static void userInterface(List<ActorRef> nodes) throws IOException {
        // Handle command line input
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        boolean close = false;
        String out = "Enter:\n"
                + "- 'r' to send a request\n"
                + "- 'c' to crash a node\n"
                + "- 'f' to issue commands from a file\n"
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
                        int nodeId = Integer.parseInt(in.readLine());
                        checkId(nodeId);
                        readValidNumber = true;

                        try {
                            UserInput msg = getCommand(userInput);
                            nodes.get(nodeId).tell(msg, null);
                            printCommand(msg, nodeId);
                        } catch (IllegalArgumentException ex) {
                            // The execution flow must never enter here
                            System.out.println("Unknown command");
                        }
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Incorrect ID number. Please enter an integer value between 0 and " + (N_NODES - 1));
                    }
                }
            } else if (userInput.equals("f")) {
                File file = new File(COMMANDS_FILENAME);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    try {
                        String[] split = line.split("\\s+");
                        String command = split[0];
                        int nodeId = Integer.parseInt(split[1]);
                        checkId(nodeId);

                        try {
                            UserInput msg = getCommand(command);
                            nodes.get(nodeId).tell(msg, null);
                            printCommand(msg, nodeId);
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Unknown command. Skip line");
                        }
                    } catch (IllegalArgumentException ex) {
                        System.out.println("Incorrect ID number. Skip line");
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
     * Creates the actor system representing a computer network of nodes
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

        // Create the actor system
        final ActorSystem system = ActorSystem.create("helloakka");

        // Create nodes
        List<ActorRef> nodes = new ArrayList<>();
        for (int i = 0; i < N_NODES; i++) {
            nodes.add(system.actorOf(Node.props(i), "node" + i));
        }

        // Define the tree topology
        Graph g = createStructureTree1();
        // Graph g = createStructureLine();
        // Graph g = createStructureTree2();
        // Graph g = createStructureTree3();
        g.printAdjacencyList();

        // Send boostrap messages to the nodes to inform them of their neighbors
        for (int nodeId = 0; nodeId < N_NODES; nodeId++) {
            // Get the identifiers of the neighbors
            ArrayList<Integer> neighborsId = g.getAdjacencyList(nodeId);    // List of neighbors ID
            List<ActorRef> neighbors = new ArrayList<>();                   // List of neighbors ActorRef

            for (int neighborId : neighborsId) {
                ActorRef neighbor = nodes.get(neighborId);
                neighbors.add(neighbor);
            }

            // Check if current node is the initial privileged node
            boolean isStarter = false;
            if (nodeId == INITIAL_PRIVILEGED_NODE_ID) {
                isStarter = true;
            }

            // Create a bootstrap message containing the neighbors and a flag used to
            // inform the initial privileged node
            Bootstrap start = new Bootstrap(neighbors, isStarter);
            // Send the bootstrap message
            nodes.get(nodeId).tell(start, null);
        }

        userInterface(nodes);
        system.terminate();
    }
}
