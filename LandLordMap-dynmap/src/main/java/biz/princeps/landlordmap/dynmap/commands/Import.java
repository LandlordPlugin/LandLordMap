package biz.princeps.landlordmap.dynmap.commands;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.LLDynmap;
import biz.princeps.landlordmap.dynmap.island.IsleGenerator;
import biz.princeps.landlordmap.dynmap.worldguard.LLDM_Worldguard;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.Properties;
import biz.princeps.lib.command.SubCommand;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Import extends SubCommand {

    private final LLDynmap pl;
    private final IWorldGuardManager wgm;

    private final boolean defaultVal;
    private final boolean showflag;

    public Import(LLDynmap pl) {
        super(pl.getConfig().getString("CommandSettings.import.name"),
                pl.getConfig().getString("CommandSettings.import.usage"),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.import.permissions")),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.import.aliases")));
        this.pl = pl;
        wgm = pl.getWorldGuardHandler();
        defaultVal = pl.getConfig().getBoolean("WorldGuard.color.default");
        showflag = pl.getConfig().getBoolean("WorldGuard.color.show_flag");

    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {
        //lldm import <option>
        if (arguments.size() == 0 || (arguments.size() == 1 && "all".equalsIgnoreCase(arguments.get()[0]))) {
            // assume all
            properties.getCommandSender().sendMessage(ChatColor.GREEN + "Starting to import all regions into dynmap!");
            run(() -> {
                importRegions();
                importWorldguard();
            }, properties);
        }

        if (arguments.size() == 1) {
            switch (arguments.get(0)) {
                case "wg":
                    properties.getCommandSender().sendMessage(ChatColor.GREEN + "Starting to import worldguard regions into dynmap!");
                    run(this::importWorldguard, properties);
                    break;
                case "ll":
                    properties.getCommandSender().sendMessage(ChatColor.GREEN + "Starting to import landlord regions into dynmap!");
                    run(this::importRegions, properties);
                    break;
            }
        }
    }

    private void run(Runnable r, Properties properties) {
        new BukkitRunnable() {
            @Override
            public void run() {
                r.run();
                properties.getCommandSender().sendMessage(ChatColor.GREEN + "Finished importing!");
            }
        }.runTaskAsynchronously(pl);
    }

    private void importRegions() {
        Set<IOwnedLand> regions = wgm.getRegions();
        regions.forEach(ol -> {
            World world = ol.getWorld();
            if (!pl.getIsleGenerator().containsKey(world)) {
                pl.getIsleGenerator().put(world, new IsleGenerator(pl, world));
            }
            pl.getIsleGenerator().get(world).insert(ol);
        });
    }

    private void importWorldguard() {
        Map<IWrappedRegion, World> regions = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            WorldGuardWrapper instance = WorldGuardWrapper.getInstance();
            for (IWrappedRegion rg : instance.getRegions(world).values()) {
                regions.put(rg, world);
            }
        }

        if (pl.getConfig().getBoolean("WorldGuard.color", true)) {
            for (IWrappedRegion rg : regions.keySet()) {
                Optional<Boolean> flag = rg.getFlag(LLDM_Worldguard.LLDM_COLOR_FLAG);
                if ((flag.isPresent() && showflag) || (!flag.isPresent() && defaultVal)) {
                    pl.getLandHelper().markWGRegion(regions.get(rg), rg);
                } else {
                    pl.getLandHelper().deleteWGRegion(rg.getId());
                }
            }
        } else {
            for (IWrappedRegion region : regions.keySet()) {
                pl.getLandHelper().deleteWGRegion(region.getId());
            }
        }
    }
}
