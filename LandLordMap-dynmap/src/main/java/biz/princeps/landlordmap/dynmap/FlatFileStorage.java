package biz.princeps.landlordmap.dynmap;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlordmap.dynmap.island.Island;
import biz.princeps.landlordmap.dynmap.island.IsleGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dynmap.markers.PolyLineMarker;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FlatFileStorage {

    private final LLDynmap pl;

    private File customConfigFile;
    private FileConfiguration customConfig;

    public FlatFileStorage(LLDynmap pl) {
        this.pl = pl;
        init();
    }

    public void init() {
        customConfigFile = new File(pl.getDataFolder(), "storage.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            try {
                customConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            pl.getLogger().log(Level.SEVERE, "coult not load config", e);
        }

    }

    /*
    world:
      islename:
        owner: asdasdasd
        lands:
     */
    public Map<World, IsleGenerator> getIsles() {
        Map<World, IsleGenerator> worldsIsles = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            ConfigurationSection section = customConfig.getConfigurationSection(world.getName());
            if (section == null || section.getKeys(false).isEmpty()) {
                continue;
            }
            IsleGenerator gen = new IsleGenerator(pl, world);
            worldsIsles.put(world, gen);

            Map<String, Island> isles = new HashMap<>();
            Set<String> keys = section.getKeys(false);
            for (String islenames : keys) {
                List<String> ol = (List<String>) section.get(islenames + ".lands");
                UUID uuid = UUID.fromString((String) section.get(islenames + ".owner"));
                List<String> lines = (List<String>) section.get(islenames + ".lines");

                Set<IOwnedLand> lands = new HashSet<>();
                for (String land : ol) {
                    IOwnedLand region = pl.getWorldGuardHandler().getRegion(land);
                    if (region != null) {
                        lands.add(region);
                    }
                }

                Map<String, PolyLineMarker> polys = new HashMap<>();
                for (String line : lines) {
                    PolyLineMarker polyLineMarker = pl.getLLMarkers().findPolyLineMarker(line);
                    if (polyLineMarker != null) {
                        polys.put(line, polyLineMarker);
                    }
                }

                isles.put(islenames, new Island(pl, world, islenames, uuid, lands, polys, pl.getLLMarkers()));
            }

            gen.setIsles(isles);
        }

        return worldsIsles;
    }


    public void save(Map<World, IsleGenerator> isles) {
        Map<String, Object> configValues = customConfig.getValues(false);
        for (Map.Entry<String, Object> entry : configValues.entrySet()) {
            customConfig.set(entry.getKey(), null);
        }
        isles.forEach((w, i) -> {
            ConfigurationSection section = customConfig.createSection(w.getName());

            i.getIsles().forEach((name, isle) -> {
                List<String> list = new ArrayList<>();
                for (IOwnedLand ownedLand : isle.getProtectedRegions()) {
                    list.add(ownedLand.getName());
                }

                List<String> polylines = new ArrayList<>();
                for (PolyLineMarker polyLine : isle.getPolyLines()) {
                    polylines.add(polyLine.getMarkerID());
                }

                section.set(name + ".owner", isle.getOwner().toString());
                section.set(name + ".lands", list);
                section.set(name + ".lines", polylines);
            });
        });
        save();
    }


    public synchronized void save() {
        try {
            customConfig.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
