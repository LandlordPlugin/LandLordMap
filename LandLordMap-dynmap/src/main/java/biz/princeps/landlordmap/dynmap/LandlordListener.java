package biz.princeps.landlordmap.dynmap;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.events.LandManageEvent;
import biz.princeps.landlord.api.events.LandPostClaimEvent;
import biz.princeps.landlord.api.events.LandUnclaimEvent;
import biz.princeps.landlordmap.dynmap.island.IsleGenerator;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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

    @EventHandler
    public void onUnclaimEvent(LandUnclaimEvent e) {
        IOwnedLand region = e.getLand();
        plugin.debug("Unclaim Event called for " + region);

        World w = e.getLand().getWorld();
        plugin.getIsleGenerator().get(w).remove(region);
    }

    @EventHandler
    public void onManage(LandManageEvent e) {
        plugin.debug("Manage Event called for " + e.getLand());

        plugin.getLandHelper().updateLand(e.getLand());
    }

}
