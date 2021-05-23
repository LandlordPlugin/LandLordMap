package biz.princeps.landlordmap.bluemap.commands;

import biz.princeps.landlordmap.bluemap.LLBlueMap;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.Properties;
import biz.princeps.lib.command.SubCommand;
import com.google.common.collect.Sets;
import de.bluecolored.bluemap.api.BlueMapAPI;

import java.util.Optional;

public class CommandReload extends SubCommand {

    private final LLBlueMap plugin;

    public CommandReload(LLBlueMap plugin) {
        super(plugin.getConfig().getString("CommandSettings.reload.name"),
                plugin.getConfig().getString("CommandSettings.reload.usage"),
                Sets.newHashSet(plugin.getConfig().getStringList("CommandSettings.reload.permissions")),
                Sets.newHashSet(plugin.getConfig().getStringList("CommandSettings.reload.aliases")));
        this.plugin = plugin;
    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {
        plugin.reloadConfig();
        plugin.getConfiguration().load();

        final Optional<BlueMapAPI> optionalBlueMapAPI = BlueMapAPI.getInstance();
        if (optionalBlueMapAPI.isPresent()) {
            plugin.getBlueMapIntegration().hookBlueMap(optionalBlueMapAPI.get());
            properties.sendMessage("§aConfig successfully reloaded!");
        } else {
            properties.sendMessage("§eConfig partially reloaded!");
        }
    }

}