package biz.princeps.landlordmap.dynmap.commands;

import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.LLDynmap;
import biz.princeps.lib.command.Arguments;
import biz.princeps.lib.command.MainCommand;
import biz.princeps.lib.command.Properties;
import com.google.common.collect.Sets;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commands extends MainCommand {

    private final LLDynmap pl;
    private final IWorldGuardManager wgm;

    public Commands(LLDynmap pl) {
        super(pl.getConfig().getString("CommandSettings.Main.name"),
                pl.getConfig().getString("CommandSettings.Main.description"),
                pl.getConfig().getString("CommandSettings.Main.usage"),
                Sets.newHashSet(pl.getConfig().getStringList("CommandSettings.Main.permissions")),
                pl.getConfig().getStringList("CommandSettings.Main.aliases").toArray(new String[]{}));
        this.pl = pl;
        this.wgm = pl.getWorldGuardHandler();

        this.addSubcommand(new Import(pl));
        this.addSubcommand(new Color(pl));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        List<String> tabReturn = new ArrayList<>();

        if (args.length == 1) {
            subCommandMap.forEach((n, s) -> {
                if (s.hasPermission(sender)) {
                    tabReturn.add(n);
                }
            });
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("import")) {
                Collections.addAll(tabReturn, "all", "wg", "ll");
            }
        }

        return tabReturn;
    }

    @Override
    public void onCommand(Properties properties, Arguments arguments) {

    }


}
