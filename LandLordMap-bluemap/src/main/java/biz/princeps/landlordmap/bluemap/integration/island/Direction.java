package biz.princeps.landlordmap.bluemap.integration.island;

import com.flowpowered.math.vector.Vector2d;

public enum Direction {

    EAST(1) {
        @Override
        public Vector2d getFirstPoint(int chunkX, int chunkZ) {
            return new Vector2d((chunkX << 4) + 16, chunkZ << 4);
        }

        @Override
        public Vector2d getSecondPoint(int chunkX, int chunkZ) {
            return new Vector2d((chunkX << 4) + 16, (chunkZ << 4) + 16);
        }
    },
    SOUTH(2) {
        @Override
        public Vector2d getFirstPoint(int chunkX, int chunkZ) {
            return new Vector2d((chunkX << 4) + 16, (chunkZ << 4) + 16);
        }

        @Override
        public Vector2d getSecondPoint(int chunkX, int chunkZ) {
            return new Vector2d(chunkX << 4, (chunkZ << 4) + 16);
        }
    },
    WEST(3) {
        @Override
        public Vector2d getFirstPoint(int chunkX, int chunkZ) {
            return new Vector2d(chunkX << 4, (chunkZ << 4) + 16);
        }

        @Override
        public Vector2d getSecondPoint(int chunkX, int chunkZ) {
            return new Vector2d(chunkX << 4, chunkZ << 4);
        }
    },
    NORTH(4) {
        @Override
        public Vector2d getFirstPoint(int chunkX, int chunkZ) {
            return new Vector2d(chunkX << 4, chunkZ << 4);
        }

        @Override
        public Vector2d getSecondPoint(int chunkX, int chunkZ) {
            return new Vector2d((chunkX << 4) + 16, chunkZ << 4);
        }
    };

    private final int index;

    Direction(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public abstract Vector2d getFirstPoint(int chunkX, int chunkZ);

    public abstract Vector2d getSecondPoint(int chunkX, int chunkZ);

}
