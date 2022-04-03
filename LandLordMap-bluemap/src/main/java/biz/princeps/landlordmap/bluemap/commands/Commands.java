package biz.princeps.landlordmap.bluemap.commands;

import biz.princeps.landlordmap.bluemap.LLBlueMap;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.MainCommand;
import biz.princeps.lib.command.Properties;
import com.google.common.collect.Sets;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class Commands extends MainCommand {

    private final LLBlueMap plugin;

    public Commands(LLBlueMap plugin) {
        super(plugin.getConfig().getString("CommandSettings.Main.name"),
                plugin.getConfig().getString("CommandSettings.Main.description"),
                plugin.getConfig().getString("CommandSettings.Main.usage"),
                Sets.newHashSet(plugin.getConfig().getStringList("CommandSettings.Main.permissions")),
                plugin.getConfig().getStringList("CommandSettings.Main.aliases").toArray(new String[]{}));
        this.plugin = plugin;

        addSubcommand(new CommandReload(plugin));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            subCommandMap.forEach((name, subCommand) -> {
                if (subCommand.hasPermission(sender)) {
                    completions.add(name);
                }
            });
        }

        completions.sort(null);
        return completions;
    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {
    }

}
