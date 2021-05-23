package biz.princeps.landlordmap.bluemap.config;

import biz.princeps.landlordmap.bluemap.LLBlueMap;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;

public class Configuration {

    private final LLBlueMap plugin;

    private long updateTaskFrequency;
    private int maxProcessedPerUpdate;

    private String markerSetLabel;
    private boolean markerSetDefaultHidden;
    private boolean markerSetToggleable;
    private int markerSetLifetime;

    private String blueMapLandLabel;
    private Color blueMapLandLineColor;
    private int blueMapLandLineWidth;
    private Color blueMapLandFillColor;
    private int blueMapLandFillAlpha;
    private boolean blueMapLandOwnerBased;
    private int blueMapLandHeight;
    private int blueMapLandMinDistance;
    private int blueMapLandMaxDistance;
    private String blueMapLandDetail;

    public Configuration(LLBlueMap plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        final FileConfiguration config = plugin.getConfig();

        updateTaskFrequency = config.getLong("update-task-frequency");
        maxProcessedPerUpdate = config.getInt("max-processed-per-update");

        markerSetLabel = config.getString("markerset.label");
        markerSetDefaultHidden = config.getBoolean("markerset.default-hidden");
        markerSetToggleable = config.getBoolean("markerset.toggleable");
        markerSetLifetime = config.getInt("markerset.max-lifetime");

        blueMapLandLabel = config.getString("bluemapland.label");
        blueMapLandLineColor = Color.decode(config.getString("bluemapland.line-color"));
        blueMapLandLineWidth = config.getInt("bluemapland.line-width");
        blueMapLandFillColor = Color.decode(config.getString("bluemapland.fill-color"));
        blueMapLandFillAlpha = (int) (config.getDouble("bluemapland.fill-alpha") * 255);
        blueMapLandOwnerBased = config.getBoolean("bluemapland.owner-based");
        blueMapLandHeight = config.getInt("bluemapland.height");
        blueMapLandMinDistance = config.getInt("bluemapland.min-distance");
        blueMapLandMaxDistance = config.getInt("bluemapland.max-distance");
        blueMapLandDetail = config.getString("bluemapland.detail");
    }

    public long getUpdateTaskFrequency() {
        return updateTaskFrequency;
    }

    public int getMaxProcessedPerUpdate() {
        return maxProcessedPerUpdate;
    }

    public int getMarkerSetLifetime() {
        return markerSetLifetime;
    }

    public String getMarkerSetLabel() {
        return markerSetLabel;
    }

    public boolean isMarkerSetDefaultHidden() {
        return markerSetDefaultHidden;
    }

    public boolean isMarkerSetToggleable() {
        return markerSetToggleable;
    }

    public String getBlueMapLandLabel() {
        return blueMapLandLabel;
    }

    public Color getBlueMapLandLineColor() {
        return blueMapLandLineColor;
    }

    public int getBlueMapLandLineWidth() {
        return blueMapLandLineWidth;
    }

    public Color getBlueMapLandFillColor() {
        return blueMapLandFillColor;
    }

    public int getBlueMapLandFillAlpha() {
        return blueMapLandFillAlpha;
    }

    public boolean isBlueMapLandOwnerBased() {
        return blueMapLandOwnerBased;
    }

    public int getBlueMapLandHeight() {
        return blueMapLandHeight;
    }

    public int getBlueMapLandMinDistance() {
        return blueMapLandMinDistance;
    }

    public int getBlueMapLandMaxDistance() {
        return blueMapLandMaxDistance;
    }

    public String getBlueMapLandDetail() {
        return blueMapLandDetail;
    }

}
