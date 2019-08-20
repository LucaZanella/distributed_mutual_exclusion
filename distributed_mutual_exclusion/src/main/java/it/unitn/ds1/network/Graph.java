package it.unitn.ds1.network;

import java.util.ArrayList;

public class Graph {

    ArrayList<ArrayList<Integer>> adj;
    int V;

    public Graph(int V) {
        this.V = V;
        adj = new ArrayList<ArrayList<Integer>>(V);
        for (int i = 0; i < V; i++) {
            adj.add(new ArrayList<Integer>());
        }
    }

    public void addEdge(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    public ArrayList<Integer> getAdjacencyList(int u) {
        return adj.get(u);
    }

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
