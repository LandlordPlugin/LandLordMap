package biz.princeps.landlordmap.dynmap;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.events.*;
import biz.princeps.landlordmap.dynmap.island.IsleGenerator;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class LandlordListener implements Listener {

    private final LLDynmap plugin;

    public LandlordListener(LLDynmap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClaimEvent(LandPostClaimEvent e) {
        IOwnedLand region = e.getLand();
        plugin.debug("Claim Event called for " + region);

        if (region == null) {
            return;
        }

        World w = e.getLand().getWorld();

        if (!plugin.getIsleGenerator().containsKey(w)) {
            plugin.getIsleGenerator().put(w, new IsleGenerator(plugin, e.getLand().getWorld()));
        }
        plugin.getIsleGenerator().get(w).insert(region);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnclaimEvent(LandUnclaimEvent e) {
        IOwnedLand region = e.getLand();
        plugin.debug("Unclaim Event called for " + region);

        World w = e.getLand().getWorld();
        plugin.getIsleGenerator().get(w).remove(region);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLandClearEvent(LandClearEvent e) {
        IOwnedLand region = e.getLand();
        plugin.debug("Clear Event called for " + region);

        World w = e.getLand().getWorld();
        plugin.getIsleGenerator().get(w).remove(region);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLandClearInactiveEvent(LandClearInactiveEvent e) {
        OfflinePlayer player = e.getClearedPlayer();
        plugin.debug("Clear Inactive Event called for " + player.getName());

        for (IOwnedLand region : plugin.getWorldGuardHandler().getRegions(player.getUniqueId())) {
            plugin.getIsleGenerator().get(region.getWorld()).remove(region);
        }
    }

    @EventHandler
    public void onManage(LandManageEvent e) {
        plugin.debug("Manage Event called for " + e.getLand());

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLandHelper().updateLand(e.getLand());
            }
        };

        if (e.isAsynchronous()) {
            runnable.run();
        } else {
            runnable.runTaskAsynchronously(plugin);
        }
    }

}
