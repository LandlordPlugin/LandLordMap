package biz.princeps.landlordmap.bluemap;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlordmap.bluemap.commands.Commands;
import biz.princeps.landlordmap.bluemap.config.Configuration;
import biz.princeps.landlordmap.bluemap.integration.BlueMapIntegration;
import biz.princeps.landlordmap.bluemap.listeners.LandlordListener;
import biz.princeps.lib.PrincepsLib;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LLBlueMap extends JavaPlugin {

    private ILandLord landLordAPI;
    private Configuration configuration;
    private BlueMapIntegration blueMapIntegration;

    @Override
    public void onEnable() {
        getLogger().info( "Loading configuration...");
        saveDefaultConfig();
        configuration = new Configuration(this);

        blueMapIntegration = new BlueMapIntegration(this);

        PluginManager pluginManager = getServer().getPluginManager();
        getLogger().info("Loading listeners...");
        pluginManager.registerEvents(new LandlordListener(this, blueMapIntegration), this);

        getLogger().info( "Loading commands...");
        PrincepsLib.getCommandManager().registerCommand(new Commands(this));

        getLogger().info( "Loading APIs...");
        landLordAPI = (ILandLord) pluginManager.getPlugin("Landlord");

        BlueMapAPI.onEnable(blueMapAPI -> {
            getLogger().info( "BlueMap integration is initializing...");
            blueMapIntegration.hookBlueMap(blueMapAPI);
            getLogger().info( "BlueMap integration has been successfully enabled/reloaded!");
        });
    }

    @Override
    public void onDisable() {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> blueMapIntegration.unhookBlueMap(blueMapAPI));
        getLogger().info("Thank you :)");
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public BlueMapIntegration getBlueMapIntegration() {
        return blueMapIntegration;
    }

    public ILandLord getLandLordAPI() {
        return landLordAPI;
    }

}
