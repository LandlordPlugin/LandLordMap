package biz.princeps.landlordmap.dynmap;

import biz.princeps.landlord.api.ILLFlag;
import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.IWorldGuardManager;
import com.google.common.collect.Lists;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.codemc.worldguardwrapper.region.IWrappedRegion;
import org.codemc.worldguardwrapper.selection.ICuboidSelection;
import org.codemc.worldguardwrapper.selection.IPolygonalSelection;
import org.codemc.worldguardwrapper.selection.ISelection;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.PolyLineMarker;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LandHelper {

    private final IWorldGuardManager wg;
    private final LLDynmap pl;

    public LandHelper(LLDynmap p, IWorldGuardManager wg) {
        this.wg = wg;
        this.pl = p;
    }

    /**
     * Returns all the adjacent lands, that are owned by the same person as pr.
     * If a chunk is from a different owner, null will be placed inside the array.
     * Also if the neighbour chunk is equals to the ignored parameter, it will be null
     * <p>
     * toReturn[0] = North
     * toReturn[1] = East
     * toReturn[2] = South
     * toReturn[3] = West
     *
     * @param ignored ignores a chunk
     * @param pr      the chunk to get adjacent of same owner from
     * @return an array containing all  the nearby lands
     */
    public IOwnedLand[] getAdjacentOfOwner(IOwnedLand ignored, IOwnedLand pr) {
        IOwnedLand[] adjacentOfOwner = wg.getSurroundingsOwner(pr.getChunk(), pr.getOwner());
        // pl.debug("adjOfOwner="+ Arrays.toString(adjacentOfOwner));
        for (int i = 1; i < adjacentOfOwner.length; i++) {
            if (adjacentOfOwner[i] != null && adjacentOfOwner[i].equals(ignored)) {
                adjacentOfOwner[i] = null;
            }
        }
        // pl.debug("after ignored: adjOfOwner="+ Arrays.toString(adjacentOfOwner));

        return adjacentOfOwner;
    }

    /**
     * Calculates a random rgb color and returns the integer
     *
     * @return a random rgb color as int
     */
    public long randomColor() {
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);
        return new Color(r, g, b).getRGB();
    }

    /**
     * Gets a random color from a config node, that contains a list.
     *
     * @param mode the config node to get the list from.
     * @return a random rgb color as int
     */
    public long randomColorFromSet(String mode) {
        List<String> stringList = pl.getConfig().getStringList("Coloring." + mode + ".set");
        Random randy = new Random();
        int i = randy.nextInt(stringList.size());

        return Long.parseLong(stringList.get(i));
    }

    /**
     * Updates the in-live marker recalculating owner, description, flags, etc.
     *
     * @param pr the protected region to update
     */
    public void updateLand(IOwnedLand pr) {
        AreaMarker areaMarker = pl.getLLMarkers().findAreaMarker(pr.getName());
        if (areaMarker != null) {
            String text = getDescription(pr);
            areaMarker.setDescription(text);
        }
    }

    /**
     * Returns a fully formated description String for a specific land.
     * All the wg flags and placeholders like %owner% will be replaced accordingly.
     *
     * @param pr the protected region to get the string for
     * @return a formatted string
     */
    public String getDescription(IOwnedLand pr) {
        StringBuilder sb = new StringBuilder();
        pl.getConfig().getStringList("LandLord.text").forEach(s -> {
            String replaced = replaceFlags(s, pr);
            sb.append(replaced);
        });
        return sb.toString();
    }

    /**
     * Replaces stuff for a string. Helper method for getDescription.
     *
     * @param s  the string to replace stuff in
     * @param pr the pr
     * @return a formatted string
     */
    private String replaceFlags(String s, IOwnedLand pr) {
        List<ILLFlag> flags = pr.getFlags();
        int inactiveRemainingDaysSync = pl.getApi().getPlayerManager().getInactiveRemainingDaysSync(pr.getOwner());
        for (ILLFlag f : flags) {
            s = s.replace("%name%", pr.getName())
                    .replace("%buyableIn%", String.valueOf(inactiveRemainingDaysSync))
                    .replace("%owner%", pr.getOwnersString())
                    .replace("%owneruuid%", pr.getOwner().toString())
                    .replace("%member%", (pr.getMembersString().isEmpty() ? "-" : pr.getMembersString()))
                    .replace("%" + f.getName() + "%", craftFlagStatus(f))
                    .replace("%greeting%", stripColors(pr.getGreetMessage()))
                    .replace("%farewell%", stripColors(pr.getFarewellMessage()));
        }
        return s;
    }

    private String craftFlagStatus(ILLFlag flag) {
        String deny = pl.getConfig().getString("LandLord.flagStatus.deny", "-");
        String allow = pl.getConfig().getString("LandLord.flagStatus.allow", "+");
        String status = pl.getConfig().getString("LandLord.flagStatus.status", "all: %all% | friends: %friends%");

        if (flag.getAllStatus()) {
            status = status.replace("%all%", allow);
        } else {
            status = status.replace("%all%", deny);
        }
        if (flag.getFriendStatus()) {
            status = status.replace("%friends%", allow);
        } else {
            status = status.replace("%friends%", deny);
        }

        return status;
    }

    /**
     * Remove color codes from a string
     *
     * @param input the string to remove colors from
     * @return a string without colors
     */
    private String stripColors(String input) {
        return ChatColor.stripColor(input).replaceAll("&([a-f]|[0-7]|[k-o]|[r])", "").trim();
    }

    /**
     * Marks a worldguard region in a specific world with coloring from config
     *
     * @param world           the world the pr is in
     * @param protectedRegion the protected region
     */
    public void markWGRegion(World world, IWrappedRegion protectedRegion) {
        ISelection selection = protectedRegion.getSelection();
        ArrayList<Location> points;
        if (selection instanceof IPolygonalSelection) {
            points = Lists.newArrayList(((IPolygonalSelection) selection).getPoints());
        } else {
            // cuboid selection
            Location maximumPoint = ((ICuboidSelection) selection).getMaximumPoint();
            Location minimumPoint = ((ICuboidSelection) selection).getMinimumPoint();

            int xMax = maximumPoint.getBlockX(), zMax = maximumPoint.getBlockZ();
            int xMin = minimumPoint.getBlockX(), zMin = maximumPoint.getBlockZ();

            Location p2 = maximumPoint.clone(), p4 = minimumPoint.clone();
            p2.setX(xMin);
            p4.setX(xMax);
            points = Lists.newArrayList(maximumPoint, p2, minimumPoint, p4);
        }
        double[] x = new double[points.size()];
        double[] z = new double[points.size()];
        double[] y = new double[points.size() + 1];

        StringBuilder sb = new StringBuilder();
        pl.getConfig().getStringList("WorldGuard.text").forEach(s ->
                sb.append(s.replace("%name%", protectedRegion.getId())).append("\n"));

        for (int i = 0; i < points.size(); i++) {
            x[i] = points.get(i).getX();
            z[i] = points.get(i).getZ();
            y[i] = 64;
        }

        if (pl.getWGMarkers().findAreaMarker(protectedRegion.getId()) == null) {
            AreaMarker am = pl.getWGMarkers().createAreaMarker(protectedRegion.getId(), sb.toString(), true,
                    world.getName(), x, z, true);
            if (am != null) {
                am.setFillStyle(pl.getConfig().getDouble("WorldGuard.transparency", 0.6),
                        pl.getConfig().getInt("WorldGuard.land"));
                am.setLineStyle(1, 0, pl.getConfig().getInt("WorldGuard.land"));
            }
        }

        x = Arrays.copyOf(x, x.length + 1);
        z = Arrays.copyOf(z, z.length + 1);
        x[x.length - 1] = points.get(0).getX();
        z[z.length - 1] = points.get(0).getZ();
        y[y.length - 1] = 64;

        if (pl.getWGMarkers().findPolyLineMarker("line:" + protectedRegion.getId()) == null) {
            PolyLineMarker polyLineMarker = pl.getWGMarkers().createPolyLineMarker("line:" + protectedRegion.getId(), "",
                    false, world.getName(), x, y, z, true);
            if (polyLineMarker != null) {
                polyLineMarker.setLineStyle(pl.getConfig().getInt("WorldGuard.linethickness"), 1,
                        pl.getConfig().getInt("WorldGuard.line"));
            }
        }
    }

    public void deleteWGRegion(String id) {
        AreaMarker marker = pl.getWGMarkers().findAreaMarker(id);
        if (marker != null) {
            marker.deleteMarker();
        }
        PolyLineMarker polyMarker = pl.getWGMarkers().findPolyLineMarker("line:" + id);
        if (polyMarker != null) {
            polyMarker.deleteMarker();
        }
    }

}
