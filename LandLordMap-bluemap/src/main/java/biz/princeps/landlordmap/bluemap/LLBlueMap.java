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

    ILandLord landLordAPI;
    Configuration configuration;
    BlueMapIntegration blueMapIntegration;

    @Override
    public void onEnable() {
        logToConsole(Level.INFO, "Loading configuration...");
        saveDefaultConfig();
        configuration = new Configuration(this);

        blueMapIntegration = new BlueMapIntegration(this);

        final PluginManager pluginManager = Bukkit.getPluginManager();
        logToConsole(Level.INFO, "Loading listeners...");
        pluginManager.registerEvents(new LandlordListener(this, blueMapIntegration), this);

        logToConsole(Level.INFO, "Loading commands...");
        PrincepsLib.getCommandManager().registerCommand(new Commands(this));

        logToConsole(Level.INFO, "Loading APIs...");
        landLordAPI = (ILandLord) pluginManager.getPlugin("Landlord");

        BlueMapAPI.onEnable(blueMapAPI -> {
            logToConsole(Level.INFO, "BlueMap integration is initializing...");
            blueMapIntegration.hookBlueMap(blueMapAPI);
            logToConsole(Level.INFO, "BlueMap integration has been successfully enabled/reloaded!");
        });
    }

    @Override
    public void onDisable() {
        BlueMapAPI.getInstance().ifPresent(blueMapAPI -> blueMapIntegration.unhookBlueMap(blueMapAPI));
        logToConsole(Level.INFO, "Thank you :)");
    }

    public void logToConsole(Level level, String message) {
        Bukkit.getLogger().log(level, "[" + getName() + "] " + message);
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
