package biz.princeps.landlordmap.bluemap.listeners;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.events.LandManageEvent;
import biz.princeps.landlord.api.events.LandPostClaimEvent;
import biz.princeps.landlord.api.events.LandUnclaimEvent;
import biz.princeps.landlordmap.bluemap.LLBlueMap;
import biz.princeps.landlordmap.bluemap.config.Configuration;
import biz.princeps.landlordmap.bluemap.integration.BlueMapIntegration;
import biz.princeps.landlordmap.bluemap.integration.UpdateReason;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class LandlordListener implements Listener {

    private final LLBlueMap plugin;
    private final Configuration config;
    private final BlueMapIntegration blueMapIntegration;

    public LandlordListener(LLBlueMap plugin, BlueMapIntegration blueMapIntegration) {
        this.plugin = plugin;
        config = plugin.getConfiguration();
        this.blueMapIntegration = blueMapIntegration;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED){
            return;
        }
        UUID uuid = event.getUniqueId();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Instant lastPlayed = Instant.ofEpochMilli(offlinePlayer.getLastPlayed());

        if (lastPlayed.isBefore(Instant.now().minus(config.getMarkerSetLifetime(), ChronoUnit.DAYS))) {
            for (IOwnedLand ownedLand : plugin.getLandLordAPI().getWGManager().getRegions(uuid)) {
                blueMapIntegration.enqueueLand(ownedLand, UpdateReason.CLAIM);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLandPostClaimEvent(LandPostClaimEvent event) {
        blueMapIntegration.enqueueLand(event.getLand(), UpdateReason.CLAIM);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLandUnclaimEvent(LandUnclaimEvent event) {
        blueMapIntegration.enqueueLand(event.getLand(), UpdateReason.UNCLAIM);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLandManageEvent(LandManageEvent event) {
        if (!"FRIENDS".equals(event.getFlagChanged()))
            return;
        blueMapIntegration.enqueueLand(event.getLand(), UpdateReason.MANAGE);
    }

}
