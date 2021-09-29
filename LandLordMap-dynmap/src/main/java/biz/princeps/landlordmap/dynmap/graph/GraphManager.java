package biz.princeps.landlordmap.dynmap.graph;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlordmap.dynmap.LLDynmap;

import java.util.*;

public class GraphManager {

    private Graph graph;
    private List<IOwnedLand> vertices;
    private LLDynmap pl;

    public GraphManager(LLDynmap pl, Set<IOwnedLand> lands) {
        this.vertices = new ArrayList<>(lands);
        this.pl = pl;
        this.graph = new Graph(vertices.size());

        for (int i = 0; i < vertices.size(); i++) {
            IOwnedLand[] adjacent = pl.getWorldGuardHandler().getSurroundings(vertices.get(i));
            for (IOwnedLand pr : adjacent) {
                if (pr != null && vertices.indexOf(pr) > -1) {
                    graph.addEdge(i, vertices.indexOf(pr));
                    pl.debug("Added edge " + i + "|" + vertices.indexOf(pr));
                }
            }
        }

        pl.debug(vertices.toString());
        pl.debug(graph.toString());
    }

    /**
     * This method calculates all connected components, if you remove pr.
     * Using a queue internally and BFS floodfill
     *
     * @param pr the "start" node, also called bridge node
     * @return a map, where the key is the startregion (neighbor to pr) and all the regions within that connected comp.
     */
    public Map<IOwnedLand, Set<IOwnedLand>> findSections(IOwnedLand pr) {
        Map<IOwnedLand, Set<IOwnedLand>> sections = new HashMap<>();

        LinkedList<IOwnedLand> queue = new LinkedList<>();

        // first step, get neighbors of pr
        IOwnedLand[] adjacentOfOwner = pl.getWorldGuardHandler().getSurroundingsOwner(pr, pr.getOwner());
        for (IOwnedLand protectedRegion : adjacentOfOwner) {
            if (protectedRegion != null && this.vertices.contains(protectedRegion)) {
                queue.add(protectedRegion);
            }
        }

        while (!queue.isEmpty()) {
            IOwnedLand pop = queue.pop();

            Set<IOwnedLand> protectedRegionSet = this.breadthFirstSearch(pr, pop);
            sections.put(pop, protectedRegionSet);

            queue.removeAll(protectedRegionSet);
        }

        return sections;
    }

    /**
     * This method checks, if pr is part of a bridge.
     * A bridge connects two strong maximum connected components. Removing the bridge result into two subgraphs.
     * This method finds the first node for all those subgraphs.
     *
     * @param pr the source node
     * @return a set of first nodes for each subgraph
     */
    public Set<IOwnedLand> findSectionStarts(IOwnedLand pr) {
        int i = vertices.indexOf(pr);
        List<Graph.Bridge> bridge = graph.bridge();

        pl.debug("Bridges before removing: " + bridge);

        bridge.removeIf(b -> b.one != i && b.two != i);

        pl.debug("Bridges left: " + bridge);

        // Now convert the integer back to corresponding lands
        Set<IOwnedLand> toReturn = new HashSet<>();
        for (Graph.Bridge bridge1 : bridge) {
            if (bridge1.one == i)
                toReturn.add(vertices.get(bridge1.two));
            if (bridge1.two == i)
                toReturn.add(vertices.get(bridge1.one));
        }

        pl.debug("Startpoints of new islands: " + toReturn);

        return toReturn;
    }

    /**
     * This is a simple floodfill algorithm (bfs). This way I'm calculating the remaining connected component
     *
     * @param ignored this node is ignored by the algorithm
     * @param source  the start node
     * @return a set of all lands the algorithm reached from source
     */
    public Set<IOwnedLand> breadthFirstSearch(IOwnedLand ignored, IOwnedLand source) {
        Set<IOwnedLand> protectedRegionSet = new HashSet<>();

        int ignore = vertices.indexOf(ignored);
        int s = vertices.indexOf(source);

        boolean[] visited = new boolean[vertices.size()];

        LinkedList<Integer> queue = new LinkedList<>();
        visited[s] = true;
        queue.add(s);

        while (queue.size() != 0) {
            s = queue.poll();
            protectedRegionSet.add(vertices.get(s));
            for (Integer n : graph.adj(s)) {
                if (n == ignore)
                    continue;
                if (!visited[n]) {
                    visited[n] = true;
                    queue.add(n);
                }
            }
        }
        return protectedRegionSet;
    }

}

