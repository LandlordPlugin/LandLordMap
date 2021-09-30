package biz.princeps.landlordmap.bluemap.integration.island;

import biz.princeps.landlord.api.IOwnedLand;
import biz.princeps.landlordmap.bluemap.LLBlueMap;
import com.flowpowered.math.vector.Vector2d;
import org.bukkit.Chunk;

import java.util.*;

public class BlueMapIsland {

    private final LLBlueMap plugin;

    private final UUID owner;
    private final Map<IOwnedLand, Set<Direction>> lands;

    public BlueMapIsland(LLBlueMap plugin, IOwnedLand ownedLand) {
        this.plugin = plugin;

        this.owner = ownedLand.getOwner();
        this.lands = new HashMap<>();

        recursiveSearch(ownedLand);
    }

    public UUID getOwner() {
        return owner;
    }

    public Map<IOwnedLand, Set<Direction>> getLands() {
        return lands;
    }

    private void recursiveSearch(IOwnedLand center) {
        if (center == null || lands.containsKey(center))
            return;

        final IOwnedLand[] adjacents = plugin.getLandLordAPI().getWGManager().getSurroundingsOwner(center, owner);
        final Set<Direction> directions = new HashSet<>();

        for (Direction direction : Direction.values()) {
            if (adjacents[direction.getIndex()] == null) {
                directions.add(direction);
            }
        }
        lands.put(center, directions);

        for (Direction direction : Direction.values()) {
            recursiveSearch(adjacents[direction.getIndex()]);
        }
    }

    public Vector2d[] getBorderPoints() {
        final Set<Vector2d> points = new LinkedHashSet<>();

        for (Map.Entry<IOwnedLand, Set<Direction>> entry : lands.entrySet()) {
            final Set<Direction> directions = entry.getValue();

            if (directions.isEmpty())
                continue;
            final IOwnedLand iOwnedLand = entry.getKey();
            final Chunk chunk = iOwnedLand.getChunk();
            final int chunkX = chunk.getX();
            final int chunkZ = chunk.getZ();

            for (Direction direction : directions) {
                points.add(direction.getFirstPoint(chunkX, chunkZ));
                points.add(direction.getSecondPoint(chunkX, chunkZ));
            }
        }

        return points.toArray(new Vector2d[0]);
    }

}
