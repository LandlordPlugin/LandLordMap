package biz.princeps.landlordmap.dynmap.island;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.LLDynmap;
import biz.princeps.landlordmap.dynmap.graph.GraphManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class purpose is, to handle islands and the outlining.
 */
public class IsleGenerator {

    private final MarkerSet am;
    private final LLDynmap pl;
    private final IWorldGuardManager wg;

    // The world to operate on
    private final World world;
    // maps island name to the island for nice O(1)
    private Map<String, Island> isles;

    // I'm taking a queue approach. Every action will be added into a queue and be polled later.
    private final Queue<Runnable> queue = new LinkedList<>();
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final Lock lock = new ReentrantLock();

    /**
     * Initialize a new Island generator for a specific world
     *
     * @param plugin reference to the main class
     * @param world  the world to operate on
     */
    public IsleGenerator(LLDynmap plugin, World world) {
        isles = new HashMap<>();
        am = plugin.getLLMarkers();
        pl = plugin;
        this.world = world;
        wg = pl.getWorldGuardHandler();

        // Starting the queue thread
        new BukkitRunnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    Runnable poll = queue.poll();
                    if (poll != null) {
                        //pl.debug("Executing " + poll);
                        exec.submit(poll);
                        //pl.debug("Finished " + poll);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }.runTaskTimerAsynchronously(pl, 0, 2);
    }

    /**
     * insert helper method to handle threading correctly
     *
     * @param ol the pr that should be inserted
     */
    public void insert(IOwnedLand ol) {
        if (ol == null) {
            return;
        }

        // Even if the land already exists, it should be updated. This case covers "advertise claim" for example,
        // where owner is replaced.
        if (getIsle(ol.getName()) != null) {
            pl.getLandHelper().updateLand(ol);
            return;
        }

//        this.insertLocal(ol);
        lock.lock();
        try {
            pl.debug("Added " + ol + " to the queue");
            queue.add(() -> this.insertLocal(ol));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called when a new land is claimed. The new claimed land will be inserted into the correct island.
     * Islands may be merged.
     *
     * @param pr the new land, that should be inserted
     */
    private void insertLocal(IOwnedLand pr) {
        pl.debug(pr.getName() + ": Inserting " + pr);
        // Lets check the isles of the surrounding lands
        IOwnedLand[] adjacent = wg.getSurroundingsOwner(pr, pr.getOwner());
        pl.debug(pr.getName() + ": Adjacent: " + Arrays.toString(adjacent));
        String[] isles = new String[4];

        // This way we can find out, if there is an existing island close by
        for (int i = 1; i < adjacent.length; i++) {
            if (adjacent[i] == null) {
                continue;
            }
            // pl.debug(pr.getName() + ": " + i + adjacent[i]);

            Island is = getIsle(adjacent[i].getName());

            if (is == null) {
                isles[i - 1] = null;
            } else {
                isles[i - 1] = is.getName();
            }
            // isles[i] = (is == null ? null : is.getName());

        }
        pl.debug(pr.getName() + ": Existing islands: " + Arrays.toString(isles));

        // 3 cases:
        // 1. no neighbors: so we insert a new isle
        // 2. one neighbor or multiple neighbors with the same isleID: insert into that isle
        // 3. multiple different isles: merge those isles
        Set<String> lands = new HashSet<>(Arrays.asList(isles));
        // Remove null since the above method might have added a null land into the isles list
        lands.remove(null);

        pl.debug(pr.getName() + ": Lands: " + lands);
        Set<IOwnedLand> landList = new HashSet<>();
        switch (lands.size()) {
            case 0:
                // case 1
                pl.debug("Case 1");
                landList.add(pr);

                // Insert a new island and update the marker id
                Island island = new Island(pl, world, pr.getName(), pr.getOwner(), landList, am);
                this.isles.put(pr.getName(), island);

                island.markLand(pr);
                island.outline(pr);

                pl.debug(pr.getName() + ": Isles after case 1");
                printIsles();
                break;

            case 1:
                // case 2
                pl.debug("Case 2");
                // just grab the first (and only) element
                String s = lands.iterator().next();

                // insert the land into the existing island
                island = this.isles.get(s);
                island.getProtectedRegions().add(pr);

                for (PolyLineMarker polyLine : island.getPolyLines()) {
                    if (polyLine == null)
                        continue;
                    polyLine.deleteMarker();
                }

                island.markLand(pr);
                for (IOwnedLand protectedRegion : island.getProtectedRegions()) {
                    island.outline(protectedRegion);
                }

                pl.debug(pr.getName() + ": Isles after case 2");
                printIsles();
                break;

            case 2:
            case 3:
            case 4:
                pl.debug("Case 3");
                // Merge all together into a new one
                // This list will contain all the lands of the new merged huge island
                landList.add(pr);

                Island newIsland = new Island(pl, world, pr.getName(), pr.getOwner(), landList, am);
                this.isles.put(pr.getName(), newIsland);

                // Iterate over the found islands, add them to the list and remove their outlining.
                for (String l : lands) {
                    pl.debug(pr.getName() + ": Case3 merge: " + l);
                    Island isle = this.isles.get(l);
                    landList.addAll(isle.getProtectedRegions());

                    // delete all existing lines of the old land
                    isle.getPolyLines().stream().filter(Objects::nonNull).forEach(GenericMarker::deleteMarker);

                    // in case we are in any island mode, we most likely need to recolor the new island. So remove
                    // old colors
                    if (pl.getConfig().getString("Coloring.mode").toUpperCase().contains("ISLAND")) {
                        for (IOwnedLand protectedRegion : isle.getProtectedRegions()) {
                            AreaMarker areaMarker = am.findAreaMarker(protectedRegion.getName());
                            if (areaMarker != null) {
                                areaMarker.deleteMarker();
                            }
                        }
                    }

                    // Remove island
                    this.isles.remove(l);
                }

                newIsland.getProtectedRegions().forEach(p -> {
                    newIsland.markLand(p);
                    newIsland.outline(p);
                });

                pl.debug(pr.getName() + ": Isles after case 3");
                printIsles();
                break;
        }
    }


    /**
     * Remove helper method to handle threading correctly
     *
     * @param pr the pr that should be removed
     */
    public void remove(IOwnedLand pr) {
        lock.lock();
        try {
            queue.add(() -> removeLocal(pr));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove a land. Calculates all the connected components and inserts them as new islands
     *
     * @param pr the land that should be removed
     */
    private void removeLocal(IOwnedLand pr) {
        // need the old algorithm
        pl.debug("Unclaiming " + pr);

        /*
        1. I find all the connected components using BSF
        2. remove the old island
        3. insert a new island for each found connected component
        */
        Island oldIsland = getIsle(pr.getName());
        pl.debug("Island before removal: " + oldIsland.getProtectedRegions());

        // first handle visuals, then graphs
        am.findAreaMarker(pr.getName()).deleteMarker();

        GraphManager gm = new GraphManager(pl, oldIsland.getProtectedRegions());
        Map<IOwnedLand, Set<IOwnedLand>> connectedComponents = gm.findSections(pr);

        pl.debug("Connected Components: " + connectedComponents);

        oldIsland.removeVisual();
        isles.remove(oldIsland.getName());

        connectedComponents.forEach((l, set) -> {
            Island island = new Island(pl, world, l.getName(), l.getOwner(), set, am);
            isles.put(island.getName(), island);
            for (IOwnedLand p : set) {
                island.markLand(p);
                island.outline(p, pr);
            }
        });


        pl.debug("Islands after removal:");
        printIsles();
    }

    /**
     * This is not a normal node, but node is a container for a chunk.
     * So basically said, n contains x and z chunk coordinate.
     *
     * @param n the node to get the chunk from
     * @return the ownedland corresponding to the coordinates n is carrying
     */
    private IOwnedLand getLandFromChunkNode(Node n) {
        return pl.getWorldGuardHandler().getRegion(world.getChunkAt(n.x, n.z));
    }

    /**
     * I abuse my nodes here as chunk coordinate container.
     * Calculates all the chunk coordinates around a corner node.
     *
     * @param n the corner node to get the surrounding lands
     * @return an array of chunk coord node container
     */
    private Node[] getChunkPointsAroundNode(Node n) {
        Node[] nodes = new Node[4];

        Chunk chunk0 = new Location(world, n.x + 1, 64, n.z + 1).getChunk();
        nodes[0] = new Node(chunk0.getX(), chunk0.getZ());

        Chunk chunk1 = new Location(world, n.x + 1, 64, n.z - 1).getChunk();
        nodes[1] = new Node(chunk1.getX(), chunk1.getZ());

        Chunk chunk2 = new Location(world, n.x - 1, 64, n.z - 1).getChunk();
        nodes[2] = new Node(chunk2.getX(), chunk2.getZ());

        Chunk chunk3 = new Location(world, n.x - 1, 64, n.z + 1).getChunk();
        nodes[3] = new Node(chunk3.getX(), chunk3.getZ());
        return nodes;
    }

    /**
     * get the wg lands around a certain node.
     *
     * @param n the node to look around
     * @return a list of all the wg lands around a certain node.
     */
    private List<IOwnedLand> getLandsAroundNode(Node n) {
        List<IOwnedLand> pr = new ArrayList<>();

        Chunk[] chunks = new Chunk[4];
        chunks[0] = new Location(world, n.x + 1, 64, n.z + 1).getChunk();
        chunks[1] = new Location(world, n.x + 1, 64, n.z - 1).getChunk();
        chunks[2] = new Location(world, n.x - 1, 64, n.z - 1).getChunk();
        chunks[3] = new Location(world, n.x - 1, 64, n.z + 1).getChunk();

        for (Chunk chunk : chunks) {
            IOwnedLand land = pl.getWorldGuardHandler().getRegion(chunk);
            if (land != null) {
                pr.add(land);
            }
        }

        return pr;
    }

    /**
     * FUCK YOU java. Why do i have to bother with this kind of micromanagement?
     * Converts a list to an array.
     *
     * @param list the magical list
     * @return a nice double array
     */
    private double[] toArray(List<Integer> list) {
        double[] toReturn = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            toReturn[i] = list.get(i);
        }
        return toReturn;
    }

    /**
     * Helper method for debugging purposes.
     * Prints all islands.
     */
    private void printIsles() {
        if (isles.isEmpty()) pl.debug("EMPTY");
        isles.forEach((a, b) -> pl.debug(a + ":\t" + b.getProtectedRegions() + "\n"));
    }

    /**
     * Gets corresponding island of a land
     *
     * @param name the name of the land to get the isle for
     * @return the isle of the pr
     */
    public Island getIsle(String name) {
        for (Map.Entry<String, Island> entry : isles.entrySet()) {
            pl.debug("At island: " + entry.getKey());
            for (IOwnedLand protectedRegion : entry.getValue().getProtectedRegions()) {
                pl.debug(protectedRegion + "\t");
                if (protectedRegion.getName().equals(name)) {
                    pl.debug("found island: " + name + " entry is " + entry.getValue());
                    return entry.getValue();
                }
            }
            pl.debug("\n");
        }
        return null;
    }

    public World getWorld() {
        return world;
    }

    public Map<String, Island> getIsles() {
        return isles;
    }

    public void setIsles(Map<String, Island> isles) {
        this.isles = isles;
    }
}
