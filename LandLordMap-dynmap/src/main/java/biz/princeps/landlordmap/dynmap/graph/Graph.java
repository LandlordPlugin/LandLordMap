package biz.princeps.landlordmap.dynmap.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class Graph {
    private final int V;

    private final LinkedList<Integer>[] adj;
    private int time = 0;
    private static final int NIL = -1;
    private List<Bridge> bridges;

    Graph(int v) {
        V = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; ++i)
            adj[i] = new LinkedList<>();
    }

    public void addEdge(int v, int w) {
        if (!adj[v].contains(w))
            adj[v].add(w);
        if (!adj[w].contains(v))
            adj[w].add(v);
    }

    public LinkedList<Integer> adj(int i) {
        return adj[i];
    }

    @Override
    public String toString() {
        return "Graph{" +
                "V=" + V +
                ", adj=" + Arrays.toString(adj) +
                '}';
    }

    private void bridgeUtil(int u, boolean[] visited, int[] disc, int[] low, int[] parent) {
        visited[u] = true;
        disc[u] = low[u] = ++time;

        for (int v : adj[u]) {
            if (!visited[v]) {
                parent[v] = u;
                bridgeUtil(v, visited, disc, low, parent);
                low[u] = Math.min(low[u], low[v]);
                if (low[v] > disc[u]) {
                    bridges.add(new Bridge(u, v));
                }
            } else if (v != parent[u]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }
    }

    public List<Bridge> bridge() {
        boolean[] visited = new boolean[V];
        int[] disc = new int[V];
        int[] low = new int[V];
        int[] parent = new int[V];
        bridges = new ArrayList<>();

        for (int i = 0; i < V; i++) {
            parent[i] = NIL;
            visited[i] = false;
        }
        for (int i = 0; i < V; i++) {
            if (!visited[i]) {
                bridgeUtil(i, visited, disc, low, parent);
            }
        }

        return bridges;
    }

    static class Bridge {
        final int one;
        final int two;

        Bridge(int u, int v) {
            one = u;
            two = v;
        }

        @Override
        public String toString() {
            return "Bridge{" +
                    "from=" + one +
                    ", to=" + two +
                    '}';
        }
    }
}
