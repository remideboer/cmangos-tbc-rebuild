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
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                float[] world = mapper.toWorld(px + 0.5f, py + 0.5f, w, h);
                float z = src.at(area.mapId(), world[0], world[1], Terrain.INVALID);
                argb[py * w + px] = Float.isFinite(z) && z > Terrain.INVALID + 1
                        ? colorForHeight(z) : 0xFF202428;
            }
        }
        return new Raster(w, h, argb);
    }

    /**
     * Noggit horizon ramp: below 0 is water, then grass, dirt, rock, snow.
     * Uses world Z from ADT, the same yards Noggit reads from WDL.
     */
    public static int colorForHeight(float height) {
        if (height < 0f) {
            int blue = 255 + (int) Math.max(height / 2.0, -255.0);
            return 0xFF000000 | (Math.max(0, Math.min(255, blue)));
        }
        if (height >= 1600f) {
            return 0xFFFFFFFF;
        }
        float[][] bands = {
                {0f, 600f, 20, 149, 7, 137, 84, 21},
                {600f, 1200f, 137, 84, 21, 96, 96, 96},
                {1200f, 1600f, 96, 96, 96, 255, 255, 255}
        };
        float start = 0f;
        float stop = 600f;
        int r0 = 20, g0 = 149, b0 = 7, r1 = 137, g1 = 84, b1 = 21;
        for (float[] band : bands) {
            if (height >= band[0] && height < band[1]) {
                start = band[0];
                stop = band[1];
                r0 = (int) band[2];
                g0 = (int) band[3];
                b0 = (int) band[4];
                r1 = (int) band[5];
                g1 = (int) band[6];
                b1 = (int) band[7];
                break;
            }
        }
        float t = (height - start) / (stop - start);
        int r = (int) (r1 * t + r0 * (1f - t));
        int g = (int) (g1 * t + g0 * (1f - t));
        int b = (int) (b1 * t + b0 * (1f - t));
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
