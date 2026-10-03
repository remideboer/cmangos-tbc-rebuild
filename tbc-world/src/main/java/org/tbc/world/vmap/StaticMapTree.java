package org.tbc.world.vmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** MapTree.cpp StaticMapTree: {map}.vmtree BIH of model instances, tiles loaded on demand. */
final class StaticMapTree {
    private final int mapId;
    private final Path basePath;
    private final Function<String, WorldModel> models;
    private final Bih tree;
    private final boolean tiled;
    private final ModelInstance[] values;
    private final Set<Integer> loadedTiles = new HashSet<>();

    private StaticMapTree(int mapId, Path basePath, Function<String, WorldModel> models, Bih tree, boolean tiled) {
        this.mapId = mapId;
        this.basePath = basePath;
        this.models = models;
        this.tree = tree;
        this.tiled = tiled;
        this.values = new ModelInstance[tree.primCount()];
    }

    static String treeFileName(int mapId) {
        return String.format("%03d.vmtree", mapId);
    }

    /** MapTree.cpp getTileFileName: tileY precedes tileX. */
    static String tileFileName(int mapId, int tileX, int tileY) {
        return String.format("%03d_%02d_%02d.vmtile", mapId, tileY, tileX);
    }

    /** InitMap; null when the file is missing, has a wrong magic or is truncated. */
    static StaticMapTree open(int mapId, Path basePath, Function<String, WorldModel> models) {
        Path file = basePath.resolve(treeFileName(mapId));
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            VMapReader r = VMapReader.open(file);
            if (!r.chunk(VMapManager.MAGIC)) {
                return null;
            }
            boolean tiled = r.u8() != 0;
            if (!r.chunk("NODE")) {
                return null;
            }
            Bih tree = Bih.read(r);
            if (!r.chunk("GOBJ")) {
                return null;
            }
            StaticMapTree map = new StaticMapTree(mapId, basePath, models, tree, tiled);
            if (!tiled) {
                map.readSpawns(r, Integer.MAX_VALUE);
            }
            return map;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** LoadMapTile once per tile; a missing tile file is remembered as empty. */
    synchronized void ensureTile(int tileX, int tileY) {
        if (!tiled || !loadedTiles.add((tileX << 8) | tileY)) {
            return;
        }
        Path file = basePath.resolve(tileFileName(mapId, tileX, tileY));
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            VMapReader r = VMapReader.open(file);
            if (r.chunk(VMapManager.MAGIC)) {
                readSpawns(r, r.u32());
            }
        } catch (IOException | RuntimeException e) {
            return;
        }
    }

    private void readSpawns(VMapReader r, int max) {
        for (int i = 0; i < max; i++) {
            ModelInstance.Spawn spawn = ModelInstance.Spawn.read(r);
            if (spawn == null || !r.hasRemaining()) {
                return;
            }
            int ref = r.u32();
            if (ref < 0 || ref >= values.length || values[ref] != null) {
                continue;
            }
            values[ref] = new ModelInstance(spawn, models.apply(spawn.name()));
        }
    }

    /** StaticMapTree::getHeight; +inf when nothing is hit. Position is in vmap internal coordinates. */
    synchronized float getHeight(float x, float y, float z, float maxSearchDist) {
        float dirZ = maxSearchDist >= 0f ? -1f : 1f;
        float[] maxDist = {Math.abs(maxSearchDist)};
        boolean[] hit = {false};
        tree.intersectRay(Ray.of(x, y, z, 0f, 0f, dirZ), (ray, entry, d) -> {
            ModelInstance instance = values[entry];
            if (instance != null && instance.intersectRay(ray, d)) {
                hit[0] = true;
                return true;
            }
            return false;
        }, maxDist);
        if (!hit[0]) {
            return Float.POSITIVE_INFINITY;
        }
        return maxSearchDist >= 0f ? z - maxDist[0] : z + maxDist[0];
    }
}
