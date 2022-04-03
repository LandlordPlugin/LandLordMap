package biz.princeps.landlordmap.bluemap.integration;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlordmap.bluemap.LLBlueMap;
import biz.princeps.landlordmap.bluemap.config.Configuration;
import biz.princeps.landlordmap.bluemap.utils.Constants;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BlueMapIntegration {

    private final LLBlueMap plugin;
    private final Configuration config;

    private final Map<IOwnedLand, UpdateReason> queue;
    private BukkitTask updateTask;

    public BlueMapIntegration(LLBlueMap plugin) {
        this.plugin = plugin;
        config = plugin.getConfiguration();

        queue = new ConcurrentHashMap<>();
    }

    private MarkerSet buildMarkerSet(MarkerAPI markerAPI) {
        return markerAPI.createMarkerSet(Constants.MARKER_SET_ID);
    }

    public void hookBlueMap(BlueMapAPI blueMapAPI) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, bukkitTask -> {
            try {
                MarkerAPI markerAPI = blueMapAPI.getMarkerAPI();
                plugin.getLogger().info("Loading Landlord markers...");

                MarkerSet markerSet = buildMarkerSet(markerAPI);
                int size = markerSet.getMarkers().size();

                plugin.getLogger().info("Updating Landlord markers...");
                markerSet.setLabel(config.getMarkerSetLabel());
                markerSet.setDefaultHidden(config.isMarkerSetDefaultHidden());
                markerSet.setToggleable(config.isMarkerSetToggleable());

                if (size == 0) {
                    plugin.getLogger().warning("Landlord markers not found!");
                    importLands(blueMapAPI, markerAPI, markerSet);
                } else {
                    plugin.getLogger().info(size + " Landlord markers found!");
                    markerAPI.save();
                }
                plugin.getLogger().info("Loading update task...");
                initUpdateTask();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not retrieve marker api", e);
            }
        });
    }

    public void unhookBlueMap(BlueMapAPI blueMapAPI) {
        try {
            updateTask.cancel();
            MarkerAPI markerAPI = blueMapAPI.getMarkerAPI();

            Set<IOwnedLand> ownedLands = plugin.getLandLordAPI().getWGManager().getRegions();
            plugin.getLogger().warning("Checking " + ownedLands.size() + " lands and processing " + queue.size() + " remaining updates, this could take a while...");

            for (IOwnedLand ownedLand : plugin.getLandLordAPI().getWGManager().getRegions()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownedLand.getOwner());
                Instant lastPlayed = Instant.ofEpochMilli(offlinePlayer.getLastPlayed());

                if (lastPlayed.isBefore(Instant.now().minus(config.getMarkerSetLifetime(), ChronoUnit.DAYS))) {
                    enqueueLand(ownedLand, UpdateReason.UNCLAIM);
                }
            }
            processQueue(blueMapAPI, markerAPI, buildMarkerSet(markerAPI), Integer.MAX_VALUE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not retrieve marker api", e);
        }
    }

    public void initUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
                BlueMapAPI.getInstance().ifPresent(blueMapAPI -> {
                    try {
                        MarkerAPI markerAPI = blueMapAPI.getMarkerAPI();
                        processQueue(blueMapAPI, markerAPI, buildMarkerSet(markerAPI), config.getMaxProcessedPerUpdate());
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Could not retrieve marker api", e);
                    }
                }), config.getUpdateTaskFrequency(), config.getUpdateTaskFrequency());
    }

    private void importLands(BlueMapAPI blueMapAPI, MarkerAPI markerAPI, MarkerSet markerSet) {
        Set<IOwnedLand> ownedLands = plugin.getLandLordAPI().getWGManager().getRegions();
        plugin.getLogger().warning("Importing " + ownedLands.size() + " lands, this could take a while...");

//        final BlueMapIsland blueMapIsland = new BlueMapIsland(plugin, plugin.getLandLordAPI().getWGManager().getRegion("world_1429_-2113"));
//
//        final ShapeMarker marker = markerSet.createShapeMarker(
//                "test",
//                blueMapAPI.getMap("world").get(),
//                new Shape(blueMapIsland.getBorderPoints()),
//                config.getBlueMapLandHeight());
//        marker.setLabel("test");
//        blueMapIsland.getLands().forEach((ownedLand, directions) -> {
//            if (!directions.isEmpty()) {
//                final Chunk chunk = ownedLand.getChunk();
//
//                final ShapeMarker smarker = markerSet.createShapeMarker(
//                ownedLand.getName(),
//                blueMapAPI.getMap("world").get(),
//                Shape.createRect(new Vector2d(chunk.getX() << 4, chunk.getZ() << 4), new Vector2d((chunk.getX() << 4) + 16, (chunk.getZ() << 4) + 16)),
//                config.getBlueMapLandHeight());
//                smarker.setLabel(ownedLand.getName());
//                smarker.setDetail(directions.toString());
//            }
//        });
//        for (Vector2d borderPoint : blueMapIsland.getBorderPoints()) {
//            final POIMarker m = markerSet.createPOIMarker(
//                    borderPoint.toString(),
//                    blueMapAPI.getMap("world").get(),
//                    borderPoint.getX(), 63, borderPoint.getY());
//            m.setLabel(borderPoint.toString());
//        }
//
//        try {
//            markerAPI.save();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        for (IOwnedLand ownedLand : plugin.getLandLordAPI().getWGManager().getRegions()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownedLand.getOwner());
            Instant lastPlayed = Instant.ofEpochMilli(offlinePlayer.getLastPlayed());

            if (lastPlayed.isAfter(Instant.now().minus(config.getMarkerSetLifetime(), ChronoUnit.DAYS))) {
                enqueueLand(ownedLand, UpdateReason.CLAIM);
            }
        }

        processQueue(blueMapAPI, markerAPI, markerSet, Integer.MAX_VALUE);
    }

    public void enqueueLand(IOwnedLand ownedLand, UpdateReason updateReason) {
        if (ownedLand == null || ownedLand.getOwner() == null) return;

        queue.compute(ownedLand, (queuedOwnedLand, queuedUpdateReason) -> {
            if (queuedUpdateReason == null) return updateReason;

            switch (queuedUpdateReason) {
                case CLAIM:
                case UNCLAIM:
                    if (updateReason == UpdateReason.MANAGE) return queuedUpdateReason;
                default:
                    return updateReason;
            }
        });
    }

    private void processQueue(BlueMapAPI blueMapAPI, MarkerAPI markerAPI, MarkerSet markerSet, int limit) {
        int iterations = 0;

        for (Iterator<Map.Entry<IOwnedLand, UpdateReason>> iterator = queue.entrySet().iterator(); iterator.hasNext() && iterations < limit; ) {
            Map.Entry<IOwnedLand, UpdateReason> entry = iterator.next();

            new BlueMapLand(plugin, entry.getKey()).process(blueMapAPI, markerSet, entry.getValue());

            iterator.remove();
            iterations++;
        }

        if (iterations == 0) return;

        try {
            markerAPI.save();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save to marker api", e);
        }
    }

}
