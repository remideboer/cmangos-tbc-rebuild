package org.tbc.world.vmap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.tbc.world.map.Terrain;

/**
 * Pure-Java VMAP_7.0 height reader (VMapManager2::getHeight). Map trees, tiles and .vmo models load
 * lazily from {@code <DataDir>/vmaps}; any missing or malformed file yields {@link Terrain#INVALID}.
 */
public final class VMapManager {
    static final String MAGIC = "VMAP_7.0";
    private static final float MID = 0.5f * 64.0f * 533.33333333f;

    private final Path dir;
    private final ConcurrentHashMap<Integer, Optional<StaticMapTree>> trees = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Optional<WorldModel>> models = new ConcurrentHashMap<>();

    public static VMapManager fromDataDir(Path dataDir) {
        return new VMapManager(dataDir == null ? null : dataDir.resolve("vmaps"));
    }

    private VMapManager(Path dir) {
        this.dir = dir != null && Files.isDirectory(dir) ? dir : null;
    }

    /** VMapManager2::convertPositionToInternalRep. */
    static float[] convertPosition(float x, float y, float z) {
        return new float[]{MID - x, MID - y, z};
    }

    /** Ground height below (maxSearchDist >= 0) or above (< 0) z; {@link Terrain#INVALID} on miss. */
    public float getHeight(int mapId, float x, float y, float z, float maxSearchDist) {
        if (dir == null) {
            return Terrain.INVALID;
        }
        int tileX = (int) (32 - x / Terrain.SIZE_OF_GRIDS);
        int tileY = (int) (32 - y / Terrain.SIZE_OF_GRIDS);
        if (tileX < 0 || tileX > 63 || tileY < 0 || tileY > 63) {
            return Terrain.INVALID;
        }
        StaticMapTree tree = trees.computeIfAbsent(mapId,
                id -> Optional.ofNullable(StaticMapTree.open(id, dir, this::model))).orElse(null);
        if (tree == null) {
            return Terrain.INVALID;
        }
        tree.ensureTile(tileX, tileY);
        float[] pos = convertPosition(x, y, z);
        float height = tree.getHeight(pos[0], pos[1], pos[2], maxSearchDist);
        return height < Float.POSITIVE_INFINITY ? height : Terrain.INVALID;
    }

    private WorldModel model(String name) {
        return models.computeIfAbsent(name, n -> Optional.ofNullable(WorldModel.read(dir.resolve(n + ".vmo"))))
                .orElse(null);
    }
}
