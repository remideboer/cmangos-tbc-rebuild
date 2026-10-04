package org.tbc.world.map;

/** Height-sampled region image for quest-editor minimap (no client BLP). */
public final class RegionMinimap {
    public record Raster(int width, int height, int[] argb) {
        public boolean empty() {
            return width <= 0 || height <= 0 || argb == null || argb.length == 0;
        }
    }

    private RegionMinimap() {}

    public static int gridX(float worldX) {
        return (int) (32 - worldX / Terrain.SIZE_OF_GRIDS);
    }

    public static int gridY(float worldY) {
        return (int) (32 - worldY / Terrain.SIZE_OF_GRIDS);
    }

    public static Raster render(WorldMapArea area, Terrain.Height height, int width, int heightPx) {
        int w = Math.max(1, width);
        int h = Math.max(1, heightPx);
        int[] argb = new int[w * h];
        if (area == null) {
            return new Raster(w, h, argb);
        }
        WorldMapAreaMapper mapper = new WorldMapAreaMapper(area);
        Terrain.Height src = height == null ? Terrain.NONE : height;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        float[] zs = new float[w * h];
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float[] world = mapper.toWorld(px + 0.5f, py + 0.5f, w, h);
                float z = src.at(area.mapId(), world[0], world[1], Terrain.INVALID);
                zs[py * w + px] = z;
                if (Float.isFinite(z) && z > Terrain.INVALID + 1) {
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        float span = maxZ - minZ;
        if (!(span > 0.001f)) {
            span = 1f;
        }
        for (int i = 0; i < zs.length; i++) {
            float z = zs[i];
            if (!Float.isFinite(z) || z <= Terrain.INVALID + 1) {
                argb[i] = 0xFF202428;
            } else {
                int g = 40 + (int) ((z - minZ) / span * 180);
                argb[i] = 0xFF000000 | (g << 8) | (g / 2);
            }
        }
        return new Raster(w, h, argb);
    }
}
