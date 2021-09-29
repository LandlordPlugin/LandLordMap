package biz.princeps.landlordmap.dynmap.island;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.LLDynmap;
import biz.princeps.landlordmap.dynmap.LandHelper;
import org.bukkit.World;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PolyLineMarker;

import java.util.*;

public class Island {

    private final String name;
    private final Set<IOwnedLand> protectedRegions;
    private final Map<String, PolyLineMarker> polyLineIds;
    private final MarkerSet am;
    private final UUID owner;
    private final World world;
    private final LLDynmap pl;

    private long landHex, lineHex, linethickness;
    private final double transparency;

    public Island(LLDynmap pl, World world, String name, UUID owner, Set<IOwnedLand> prs, MarkerSet am) {
        this(pl, world, name, owner, prs, new HashMap<>(), am);
    }

    /**
     * Creates a new island
     *
     * @param pl    reference to the plugin
     * @param world the world the island is in
     * @param name  the name of the island, mostly the first found land
     * @param owner the owner of the island
     * @param prs   a set of all wg regions within the island
     * @param am    a reference to the markerset, so that we can draw lines and areas later
     */
    public Island(LLDynmap pl, World world, String name, UUID owner, Set<IOwnedLand> prs, Map<String, PolyLineMarker> polys, MarkerSet am) {
        this.name = name;
        this.pl = pl;
        this.protectedRegions = prs;
        this.polyLineIds = polys;
        this.am = am;
        this.world = world;
        this.owner = owner;
        this.transparency = pl.getConfig().getDouble("Coloring.transparency", 0.6);

        // Calculates the color depending on config options.
        switch (pl.getConfig().getString("Coloring.mode").toUpperCase()) {
            case "STATIC":
                this.landHex = pl.getConfig().getLong("Coloring.static.land");
                this.lineHex = pl.getConfig().getLong("Coloring.static.line");
                this.linethickness = pl.getConfig().getLong("Coloring.static.linethickness");
                break;

            case "RANDOMPERPLAYER":
                if (!pl.getPlayerManager().hasEntry(owner)) {
                    long fill = pl.getLandHelper().randomColor();
                    pl.getPlayerManager().add(owner, fill, fill);
                }
                this.landHex = pl.getPlayerManager().getLandColor(owner);
                this.lineHex = pl.getPlayerManager().getLineColor(owner);
                this.linethickness = pl.getConfig().getLong("Coloring.randomperplayer.linethickness");
                break;

            case "RANDOMPERISLAND":
                this.landHex = pl.getLandHelper().randomColor();
                this.lineHex = landHex;
                this.linethickness = pl.getConfig().getLong("Coloring.randomperisland.linethickness");
                break;

            case "RANDOMFROMSETPERPLAYER":
                if (!pl.getPlayerManager().hasEntry(owner)) {
                    // get random fill from config set
                    long fill = pl.getLandHelper().randomColorFromSet(
                            pl.getConfig().getString("Coloring.mode").toLowerCase());
                    pl.getPlayerManager().add(owner, fill, fill);
                }

                this.landHex = pl.getPlayerManager().getLandColor(owner);
                this.lineHex = pl.getPlayerManager().getLineColor(owner);
                this.linethickness = pl.getConfig().getLong("Coloring.randomfromsetperplayer.linethickness");
                break;

            case "RANDOMFROMSETPERISLAND":
                this.landHex = pl.getLandHelper().randomColorFromSet(pl.getConfig().getString("Coloring.mode")
                        .toLowerCase());
                this.lineHex = landHex;
                this.linethickness = pl.getConfig().getLong("Coloring.randomperisland.linethickness");
                break;
        }
    }

    /**
     * @return the name of the island
     */
    public String getName() {
        return name;
    }

    /**
     * @return a set, that contains all the regions within the  island
     */
    public Set<IOwnedLand> getProtectedRegions() {
        return protectedRegions;
    }

    /**
     * Add one poly line to the island
     *
     * @param pl the polyline, that should be added
     */
    public void addPolyLine(PolyLineMarker pl) {
        if (pl != null) {
            this.polyLineIds.put(pl.getMarkerID(), pl);
        }
    }

    /**
     * Add multiple polylines to the island
     *
     * @param collection a collection of polylines, that will be added
     */
    public void addPolyLines(Collection<PolyLineMarker> collection) {
        collection.forEach(this::addPolyLine);
    }

    /**
     * @return a collection contaiining all the polylines
     */
    public Collection<PolyLineMarker> getPolyLines() {
        return polyLineIds.values();
    }

    /**
     * returns if there is a line between two corner nodes. Those two corner nodes are basically the vector
     *
     * @param one first node
     * @param two second node
     * @return if there is a line between one and two
     */
    public boolean containsLineBetween(Node one, Node two) {
        return getLineBetween(one, two) != null;
    }

    /**
     * Removes the polylinemarker from the island
     *
     * @param polyLineMarker polyline that should be removed
     */
    public void removePolyLine(PolyLineMarker polyLineMarker) {
        this.polyLineIds.remove(polyLineMarker.getMarkerID());
        polyLineMarker.deleteMarker();
    }

    /**
     * gets the poly line between to corner nodes. May return null
     *
     * @param one first node
     * @param two second node
     * @return the line between one and two
     */
    public PolyLineMarker getLineBetween(Node one, Node two) {
        PolyLineMarker p1 = polyLineIds.get(one.toString() + ":" + two.toString());
        PolyLineMarker p2 = polyLineIds.get(two.toString() + ":" + one.toString());
        if (p1 != null)
            return p1;
        return p2;
    }

    /**
     * Mark a land while respecting the color of the island
     *
     * @param pr the land to mark
     */
    public void markLand(IOwnedLand pr) {
        String landname = pr.getName();

        if (this.am.findAreaMarker(landname) != null) {
            return;
        }

        if (!pl.getApi().getWGManager().isLLRegion(landname)) {
            return;
        }

        int x = pl.getWorldGuardHandler().getX(landname) * 16;
        int z = pl.getWorldGuardHandler().getZ(landname) * 16;
        double[] x1 = {x + 16, x, x, x + 16};
        double[] z1 = {z + 16, z + 16, z, z};
        AreaMarker am = this.am.createAreaMarker(landname, pl.getLandHelper().getDescription(pr), true,
                world.getName(), x1, z1, true);
        if (am != null) {
            am.setFillStyle(transparency, (int) landHex);
            am.setLineStyle(1, 0, (int) landHex);
        }

    }


    /**
     * Marks a poly line in this island
     *
     * @param id the id of the line (unique)
     * @param x  all the x coordinates
     * @param z  all the z coordinates
     */
    public void markLine(String id, double[] x, double[] z) {

        if (this.am.findPolyLineMarker(id) != null) {
            return;
        }

        PolyLineMarker polyLineMarker = am.createPolyLineMarker(id, "", false, world.getName(),
                x, new double[]{64, 64}, z, true);
        if (polyLineMarker != null) {
            polyLineMarker.setLineStyle((int) linethickness, 1, (int) lineHex);
            addPolyLine(polyLineMarker);
        }
    }

    public long getLandHex() {
        return landHex;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setColor(long hex) {
        this.landHex = hex;
        this.lineHex = hex;
        for (IOwnedLand land : protectedRegions) {
            AreaMarker areaMarker = this.am.findAreaMarker(land.getName());
            if (areaMarker != null) {
                areaMarker.setFillStyle(transparency, (int) landHex);
                areaMarker.setLineStyle(1, 0, (int) landHex);
            }
        }

        for (PolyLineMarker value : polyLineIds.values()) {
            if (value != null) {
                value.setLineStyle((int) linethickness, 1, (int) lineHex);
            }
        }
    }

    @Override
    public String toString() {
        ArrayList<String> isles = new ArrayList<>();
        for (IOwnedLand protectedRegion : this.protectedRegions) {
            isles.add(protectedRegion.getName());
        }
        return "{ ISLAND name=" + name + ", isles=" + isles + "}";
    }

    /**
     * Helper method (currently unused). Checks if a protected region is withing an island
     *
     * @param pr the land to check
     * @return if the land is within the island
     */
    private boolean isWithinIsland(IWrappedRegion pr) {
        int xouter = pl.getWorldGuardHandler().getX(pr.getId());
        int zouter = pl.getWorldGuardHandler().getZ(pr.getId());
        return isWithinIsland(xouter, zouter);
    }

    /**
     * Checks if two coordinates x and z live within an island.
     *
     * @param x x coord
     * @param z z coord
     * @return if (x|z) are withing the island
     */
    private boolean isWithinIsland(int x, int z) {
        boolean hasXUp = false, hasXDown = false, hasZUp = false, hasZDown = false;
        for (IOwnedLand inner : this.getProtectedRegions()) {
            int xinner = pl.getWorldGuardHandler().getX(inner.getName());
            int zinner = pl.getWorldGuardHandler().getZ(inner.getName());
            if (x >= xinner && zinner == z) {
                hasXDown = true;
            }
            if (x <= xinner && zinner == z) {
                hasXUp = true;
            }
            if (z >= zinner && xinner == x) {
                hasZDown = true;
            }
            if (z <= zinner && xinner == x) {
                hasZUp = true;
            }
        }
        return hasXDown && hasXUp && hasZDown && hasZUp;
    }

    /**
     * This outline method works very easy by placing a shitton of polylines.
     * For each land I check the surroundings. Is a adjacent chunk null, I mark this edge with a poly line.
     * This approach also handles inner islands without doing any more work.
     *
     * @param pr      the land, that should be outlined
     * @param ignored this land will be treated as if its not existing
     */
    public void outline(IOwnedLand pr, IOwnedLand ignored) {
        IWorldGuardManager wg = pl.getWorldGuardHandler();
        LandHelper helper = pl.getLandHelper();

        //pl.debug(pr.getName() + ": Starting to outline");
        IOwnedLand[] adjacentOfOwner;
        if (ignored == null) {
            adjacentOfOwner = wg.getSurroundingsOwner(pr, pr.getOwner());
        } else {
            //pl.debug("before getAdjOfOwner: ignored=" + ignored + " toOutline=" + pr);
            adjacentOfOwner = helper.getAdjacentOfOwner(ignored, pr);
        }
        pl.debug(pr.getName() + ": adj " + Arrays.toString(adjacentOfOwner));


        // x+1, z
        Set<Node> nodes = getNodes(pr);
        pl.debug(pr.getName() + ": nodes " + nodes);


        Node n1 = new Node(Integer.MIN_VALUE, Integer.MIN_VALUE), n2 = null;
        for (Node node : nodes) {
            if (node.x > n1.x) {
                n1 = node;
            } else if (node.x == n1.x) {
                n2 = node;
            }
        }
        pl.debug("x+1 z: " + n1.toString() + ":" + n2.toString());
        if (adjacentOfOwner[1] == null) {
            pl.debug("\tinserting");
            markLine(n1.toString() + ":" + n2.toString(), new double[]{n1.x, n2.x}, new double[]{n1.z, n2.z});
        } else {
            // Remove edge
            if (containsLineBetween(n1, n2)) {
                pl.debug("\tremoving");
                PolyLineMarker lineBetween = getLineBetween(n1, n2);
                removePolyLine(lineBetween);
            }
        }

        // x, z+1
        nodes = getNodes(pr);

        n1 = new Node(Integer.MIN_VALUE, Integer.MIN_VALUE);
        n2 = null;
        for (Node node : nodes) {
            if (node.z > n1.z) {
                n1 = node;
            } else if (node.z == n1.z) {
                n2 = node;
            }
        }
        pl.debug("x z+1: " + n1.toString() + ":" + n2.toString());
        if (adjacentOfOwner[2] == null) {
            pl.debug("\tinserting");
            markLine(n1.toString() + ":" + n2.toString(), new double[]{n1.x, n2.x}, new double[]{n1.z, n2.z});
        } else {
            // Remove edge
            if (containsLineBetween(n1, n2)) {
                pl.debug("\tremoving");
                PolyLineMarker lineBetween = getLineBetween(n1, n2);
                removePolyLine(lineBetween);
            }
        }


        // x-1, z
        nodes = getNodes(pr);

        n1 = new Node(Integer.MAX_VALUE, Integer.MAX_VALUE);
        n2 = null;
        for (Node node : nodes) {
            if (node.x < n1.x) {
                n1 = node;
            } else if (node.x == n1.x) {
                n2 = node;
            }
        }
        pl.debug("x-1 z: " + n1.toString() + ":" + n2.toString());
        if (adjacentOfOwner[3] == null) {
            pl.debug("\tinserting");
            markLine(n1.toString() + ":" + n2.toString(), new double[]{n1.x, n2.x}, new double[]{n1.z, n2.z});
        } else {
            // Remove edge
            if (containsLineBetween(n1, n2)) {
                pl.debug("\tremoving");
                PolyLineMarker lineBetween = getLineBetween(n1, n2);
                removePolyLine(lineBetween);
            }
        }

        // x, z-1
        nodes = getNodes(pr);

        n1 = new Node(Integer.MAX_VALUE, Integer.MAX_VALUE);
        n2 = null;
        for (Node node : nodes) {
            if (node.z < n1.z) {
                n1 = node;
            } else if (node.z == n1.z) {
                n2 = node;
            }
        }
        pl.debug("x z-1: " + n1.toString() + ":" + n2.toString());
        if (adjacentOfOwner[4] == null) {
            pl.debug("\tinserting");
            markLine(n1.toString() + ":" + n2.toString(), new double[]{n1.x, n2.x}, new double[]{n1.z, n2.z});
        } else {
            // Remove edge
            if (containsLineBetween(n1, n2)) {
                pl.debug("\tremoving");
                PolyLineMarker lineBetween = getLineBetween(n1, n2);
                removePolyLine(lineBetween);
            }
        }
    }

    public void outline(IOwnedLand pr) {
        outline(pr, null);
    }

    public void removeVisual() {
        getPolyLines().forEach(GenericMarker::deleteMarker);
        getProtectedRegions().forEach(reg -> {
            AreaMarker areaMarker = am.findAreaMarker(reg.getName());
            if (areaMarker != null)
                areaMarker.deleteMarker();
        });
    }


    // helper functions

    /**
     * Calculates the nodes of a simple land.
     *
     * @param r the land
     * @return a set of nodes
     */
    private Set<Node> getNodes(IOwnedLand r) {
        Set<Node> set = new HashSet<>();

        int x = pl.getWorldGuardHandler().getX(r.getName()) * 16;
        int z = pl.getWorldGuardHandler().getZ(r.getName()) * 16;

        set.add(new Node(x, z));
        set.add(new Node(x + 16, z));
        set.add(new Node(x, z + 16));
        set.add(new Node(x + 16, z + 16));
        return set;
    }
}
