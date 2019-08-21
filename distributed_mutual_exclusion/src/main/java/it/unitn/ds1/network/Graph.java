package it.unitn.ds1.network;

import java.util.ArrayList;

/**
 * Represents the computer network.
 * This class is used only at the beginning to create the tree topology.
 */
public class Graph {

    /**
     * List that describes the set of neighbors of each node in the network.
     */
    ArrayList<ArrayList<Integer>> adj;
    /**
     * Number of nodes in the network.
     */
    int V;

    /**
     * Creates a Graph with the information about the number of nodes in the network.
     * @param V The number of nodes in the network.
     */
    public Graph(int V) {
        this.V = V;
        adj = new ArrayList<ArrayList<Integer>>(V);
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<Integer>());
        }
    }

    /**
     * Adds a neighbor v to a node u and vice versa.
     * @param u The id of a node in the network.
     * @param v The id of a node in the network.
     */
    public void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    /**
     * Gets the neighbors of a node in the network.
     * @param u The id of the node we want to get the neighbors.
     * @return A list containing the id of the neighbors in the network.
     */
    public ArrayList<Integer> getAdjacencyList(int u) {
        return adj.get(u);
    }

    /**
     * Prints the id of the neighbors of each node in the network.
     */
    public void printAdjacencyList() {
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
