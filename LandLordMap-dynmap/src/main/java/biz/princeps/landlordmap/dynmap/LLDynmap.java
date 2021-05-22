package biz.princeps.landlordmap.dynmap;

import biz.princeps.landlord.api.ILandLord;
import biz.princeps.landlord.api.IWorldGuardManager;
import biz.princeps.landlordmap.dynmap.commands.Commands;
import biz.princeps.landlordmap.dynmap.island.IsleGenerator;
import biz.princeps.landlordmap.dynmap.worldguard.LLDM_Worldguard;
import biz.princeps.lib.PrincepsLib;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerSet;

import java.util.Map;

public class LLDynmap extends JavaPlugin implements Listener {

    private DynmapAPI dynMap;
    private MarkerSet llmarkers;
    private MarkerSet wgmarkers;

    private ILandLord api;
    private IWorldGuardManager worldGuardHandler;

    private FlatFileStorage flatFileStorage;
    private PlayerManager playerManager;
    private LandHelper landHelper;
    private Map<World, IsleGenerator> isleGenerator;

    @Override
    public void onLoad() {
        LLDM_Worldguard.initFlags();
    }

    @Override
    public void onEnable() {
        try {
            this.dynMap = (DynmapAPI) getServer().getPluginManager().getPlugin("dynmap");
        } catch (NoClassDefFoundError ex) {
            getLogger().warning("dynMap is missing! Couldn't start LLDM!!!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        try {
            this.api = (ILandLord) getServer().getPluginManager().getPlugin("Landlord");
        } catch (NoClassDefFoundError ex) {
            getLogger().warning("Landlord missing! Please make sure to install the latest version of Landlord!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        initMarkers();
        flatFileStorage = new FlatFileStorage(this);
        this.playerManager = new PlayerManager(this);
        this.worldGuardHandler = api.getWGManager();

        this.landHelper = new LandHelper(this, worldGuardHandler);
        this.isleGenerator = flatFileStorage.getIsles();

        PrincepsLib.getCommandManager().registerCommand(new Commands(this));

        saveDefaultConfig();
        initListener();
    }

    @Override
    public void onDisable() {
        flatFileStorage.save(isleGenerator);
    }

    private void initListener() {
        this.getServer().getPluginManager().registerEvents(new LandlordListener(this), this);
    }

    /**
     * This method initializes the islegenerator by inserting all the lands into it.
     */
    private void initMarkers() {
        llmarkers = dynMap.getMarkerAPI().createMarkerSet("LandLord", getConfig().getString("LandLord.markerName"),
                null, true);
        if (llmarkers == null) {
            llmarkers = dynMap.getMarkerAPI().getMarkerSet("LandLord");
        }

        if (getConfig().getBoolean("WorldGuard.color", true)) {
            wgmarkers = dynMap.getMarkerAPI().createMarkerSet("WorldGuard", getConfig().getString("WorldGuard" +
                    ".markerName"), null, true);
            if (wgmarkers == null) {
                wgmarkers = dynMap.getMarkerAPI().getMarkerSet("WorldGuard");
            }
        }
    }

    public DynmapAPI getDynMap() {
        return dynMap;
    }

    public MarkerSet getLLMarkers() {
        return llmarkers;
    }

    public MarkerSet getWGMarkers() {
        return wgmarkers;
    }

    public ILandLord getApi() {
        return api;
    }

    public IWorldGuardManager getWorldGuardHandler() {
        return worldGuardHandler;
    }

    public LandHelper getLandHelper() {
        return landHelper;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public Map<World, IsleGenerator> getIsleGenerator() {
        return isleGenerator;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debugging", false))
            this.getLogger().info(message);
    }

    public String getMessage(String path) {
        String s = getConfig().getString(path);
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
