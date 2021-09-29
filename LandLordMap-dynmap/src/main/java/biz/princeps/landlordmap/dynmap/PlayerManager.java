package biz.princeps.landlordmap.dynmap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Simple and boring Playermanager.
 * I dont bother to use a fat database or anything like that, since a flatfile is more then enough for this purpose.
 */
public class PlayerManager {

    private final LLDynmap pl;
    private final FileConfiguration data;
    private final File file;

    /**
     * Instantiates a new playermanager. Creates data.yml if its not existing already.
     * Also loads the config file to data.
     *
     * @param pl reference to the plugin
     */
    public PlayerManager(LLDynmap pl) {
        this.pl = pl;
        file = new File(pl.getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                if (!pl.getDataFolder().exists()) {
                    pl.getDataFolder().mkdir();
                }
                file.createNewFile();
            } catch (IOException e) {
                pl.getLogger().warning("Could not create data.yml!");
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Add or replace a new entry. Each player has land and line as attribute
     * Also saves the change directly to the disk
     *
     * @param id   the uuid of the player
     * @param land the corresponding land hex as integer
     * @param line the corresponding line hex as integer
     */
    public void add(UUID id, long land, long line) {
        data.set(id.toString() + ".land", land);
        data.set(id.toString() + ".line", line);
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the land color for a specific player
     *
     * @param id the player
     * @return the land color
     */
    public long getLandColor(UUID id) {
        return data.getLong(id.toString() + ".land", 0x81d864);
    }

    /**
     * Gets the line color for a specific player
     *
     * @param id the player
     * @return the line color
     */
    public long getLineColor(UUID id) {
        return data.getLong(id.toString() + ".line", 0x4daf2c);
    }

    /**
     * Check if a player has an entry
     *
     * @param id the player
     * @return a boolean
     */
    public boolean hasEntry(UUID id) {
        return data.getConfigurationSection(id.toString()) != null;
    }

}
