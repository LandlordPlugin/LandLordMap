package biz.princeps.landlordmap.dynmap.commands;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.LLDynmap;
import biz.princeps.landlordmap.dynmap.island.Island;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.Properties;
import biz.princeps.lib.command.SubCommand;
import com.google.common.collect.Sets;
import org.bukkit.entity.Player;

public class Color extends SubCommand {

    private LLDynmap pl;
    private IWorldGuardManager wgm;

    public Color(LLDynmap pl) {
        super(pl.getConfig().getString("CommandSettings.color.name"),
                pl.getConfig().getString("CommandSettings.color.usage"),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.color.permissions")),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.color.aliases")));
        this.pl = pl;
        this.wgm = pl.getWorldGuardHandler();
    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {
        if (properties.isConsole()) {
            return;
        }

        if (arguments.size() != 1) {
            properties.sendUsage();
            return;
        }

        long hex = 0xffffff;
        try {
            hex = Long.parseLong(arguments.get(0), 16);
        } catch (Exception ex) {
            properties.sendMessage(pl.getMessage("CommandSettings.color.messages.wrongformat"));
            return;
        }
        Player p = properties.getPlayer();
        IOwnedLand land = wgm.getRegion(p.getLocation());
        if (land == null) {
            properties.sendMessage(pl.getMessage("CommandSettings.color.messages.noland"));
            return;
        }

        Island isle = pl.getIsleGenerator().get(p.getWorld()).getIsle(land.getName());
        isle.setColor(hex);
        properties.sendMessage(pl.getMessage("CommandSettings.color.messages.success")
                .replace("%color%", String.valueOf(hex)));
    }
}
