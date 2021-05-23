package biz.princeps.landlordmap.bluemap.integration;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlordmap.bluemap.LLBlueMap;
import biz.princeps.landlordmap.bluemap.config.Configuration;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.ObjectMarker;
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import org.bukkit.Chunk;

import java.awt.*;
import java.util.Optional;
import java.util.UUID;

public class BlueMapLand {

    private final Configuration config;

    private final String id;
    private final String label;
    private final String world;
    private final Vector2d firstCorner;
    private final Vector2d secondCorner;

    private final UUID owner;
    private final String owners;
    private final String members;

    public BlueMapLand(LLBlueMap plugin, IOwnedLand ownedLand) {
        this.config = plugin.getConfiguration();

        this.id = ownedLand.getName();
        this.label = config.getBlueMapLandLabel().replace("%ID%", id);
        this.world = ownedLand.getWorld().getName();

        final Chunk chunk = ownedLand.getChunk();
        this.firstCorner = new Vector2d(chunk.getX() << 4, chunk.getZ() << 4);
        this.secondCorner = new Vector2d((chunk.getX() << 4) + 16, (chunk.getZ() << 4) + 16);

        this.owner = ownedLand.getOwner();
        this.owners = ownedLand.getOwnersString();
        this.members = ownedLand.getMembersString();
    }

    public String getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public Vector2d getFirstCorner() {
        return firstCorner;
    }

    public Vector2d getSecondCorner() {
        return secondCorner;
    }

    public UUID getOwner() {
        return owner;
    }

    public void process(BlueMapAPI blueMapAPI, MarkerSet markerSet, UpdateReason updateReason) {
        switch (updateReason) {
            case CLAIM:
                add(blueMapAPI, markerSet);
                break;
            case UNCLAIM:
                remove(markerSet);
                break;
            case MANAGE:
                update(blueMapAPI, markerSet);
                break;
            default:
                break;
        }
    }

    public ObjectMarker add(BlueMapAPI blueMapAPI, MarkerSet markerSet) {
        final Optional<BlueMapMap> optionalBlueMapMap = blueMapAPI.getMap(world);
        if (!optionalBlueMapMap.isPresent()) return null;

        final ShapeMarker marker = markerSet.createShapeMarker(
                id,
                optionalBlueMapMap.get(),
                Shape.createRect(firstCorner, secondCorner),
                config.getBlueMapLandHeight());
        marker.setLabel(label);
        marker.setDetail(formatDetail());

        marker.setLineColor(config.getBlueMapLandLineColor());
        marker.setLineWidth(config.getBlueMapLandLineWidth());

        final Color fillColor;
        if (config.isBlueMapLandOwnerBased()) {
            final Color playerColor = new Color((int) owner.getMostSignificantBits());
            fillColor = new Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), config.getBlueMapLandFillAlpha());
        } else {
            final Color configColor = config.getBlueMapLandFillColor();
            fillColor = new Color(configColor.getRed(), configColor.getGreen(), configColor.getBlue(), config.getBlueMapLandFillAlpha());
        }
        marker.setFillColor(fillColor);

        marker.setMinDistance(config.getBlueMapLandMinDistance());
        marker.setMaxDistance(config.getBlueMapLandMaxDistance());

        return marker;
    }

    public void remove(MarkerSet markerSet) {
        markerSet.removeMarker(id);
    }

    public void update(BlueMapAPI blueMapAPI, MarkerSet markerSet) {
        final ObjectMarker marker = (ObjectMarker) markerSet.getMarker(id).orElse(add(blueMapAPI, markerSet));
        if (marker == null) return;

        marker.setDetail(formatDetail());
    }

    private String formatDetail() {
        return config.getBlueMapLandDetail()
                .replace("%NAME%", label)
                .replace("%OWNERS%", owners.replaceAll(", ", "<br>"))
                .replace("%MEMBERS%", members.replaceAll(", ", "<br>"));
    }

}
