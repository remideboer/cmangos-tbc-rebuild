package org.tbc.world.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionMinimapTest {
    private static final WorldMapArea ELWYNN = new WorldMapArea(
            AreaTable.ELWYNN_FOREST, 0, "Elwynn", "Elwynn Forest",
            1535.42f, -3766.05f, -7943.22f, -11176.3f);

    @Test
    void gridWhenGoldshireShouldMatchTerrainTileIndex() {
        assertEquals((int) (32 - (-9465f) / Terrain.SIZE_OF_GRIDS), RegionMinimap.gridX(-9465f));
        assertEquals((int) (32 - 62f / Terrain.SIZE_OF_GRIDS), RegionMinimap.gridY(62f));
        assertEquals(49, RegionMinimap.gridX(-9465f));
        assertEquals(31, RegionMinimap.gridY(62f));
    }

    @Test
    void renderWhenStubHeightShouldPlaceGoldshirePixelInsideRaster() {
        Terrain.Height height = (mapId, x, y, hint) -> mapId == 0 ? 56f : Terrain.INVALID;
        RegionMinimap.Raster raster = RegionMinimap.render(ELWYNN, height, 80, 40);
        assertEquals(80, raster.width());
        assertEquals(40, raster.height());
        assertEquals(80 * 40, raster.argb().length);
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        float[] px = map.toPixel(-9465f, 62f, raster.width(), raster.height());
        int ix = Math.min(raster.width() - 1, Math.max(0, (int) px[0]));
        int iy = Math.min(raster.height() - 1, Math.max(0, (int) px[1]));
        assertTrue(ix >= 0 && iy >= 0);
        int color = raster.argb()[iy * raster.width() + ix];
        assertTrue((color & 0xFF000000) != 0);
    }

    @Test
    void renderWhenNoHeightShouldStillHaveMapperSizedImage() {
        RegionMinimap.Raster raster = RegionMinimap.render(ELWYNN, Terrain.NONE, 16, 16);
        assertEquals(16, raster.width());
        assertEquals(16 * 16, raster.argb().length);
    }
}
