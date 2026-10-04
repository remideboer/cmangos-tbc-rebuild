package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreaMapper;
import org.tbc.world.map.WorldMapAreas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapBlpTest {
    @Test
    void cropWhenChildInsideParentShouldKeepTheChildRectangle() {
        int[] argb = new int[40 * 20];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = 0xFF112233;
        }
        RegionMinimap.Raster full = new RegionMinimap.Raster(40, 20, argb);
        WorldMapArea parent = new WorldMapArea(1, 0, "Parent", "Parent", 100f, 0f, 100f, 0f);
        WorldMapArea child = new WorldMapArea(2, 0, "Child", "Child", 60f, 40f, 60f, 40f);
        RegionMinimap.Raster crop = WorldMapBlp.crop(full, parent, child);
        assertTrue(crop.width() > 0 && crop.width() < full.width());
        assertTrue(crop.height() > 0 && crop.height() < full.height());
        assertEquals(0xFF112233, crop.argb()[0]);
    }

    @Test
    void loadWhenNoClientArchiveShouldReturnNullAndKeepSunstriderSelectable() {
        assertEquals(null, WorldMapBlp.load(null, WorldMapAreas.SUNSTRIDER));
        assertEquals("Sunstrider Isle", WorldMapAreas.SUNSTRIDER.displayName());
    }

    @Test
    void sunstriderViewWhenOverlayHasAlphaShouldPaintIsleOnTheEversongCrop() {
        int[] sheet = new int[1024 * 768];
        java.util.Arrays.fill(sheet, 0xFF224466);
        RegionMinimap.Raster parent = new RegionMinimap.Raster(1024, 768, sheet);
        int[] over = new int[8 * 8];
        over[0] = 0xFFFF0000;
        over[1] = 0x00000000;
        RegionMinimap.Raster overlay = new RegionMinimap.Raster(8, 8, over);
        RegionMinimap.Raster view = WorldMapBlp.sunstriderView(parent, overlay);
        assertEquals(WorldMapBlp.CROP_X1 - WorldMapBlp.CROP_X0, view.width());
        assertEquals(WorldMapBlp.CROP_Y1 - WorldMapBlp.CROP_Y0, view.height());
        int ox = WorldMapBlp.SUNSTRIDER_OX - WorldMapBlp.CROP_X0;
        int oy = WorldMapBlp.SUNSTRIDER_OY - WorldMapBlp.CROP_Y0;
        assertEquals(0xFFFF0000, view.argb()[oy * view.width() + ox]);
        assertEquals(0xFF224466, view.argb()[oy * view.width() + ox + 1]);
        WorldMapAreaMapper eversong = new WorldMapAreaMapper(WorldMapAreas.EVERSONG);
        float[] tl = eversong.toWorld(WorldMapBlp.CROP_X0, WorldMapBlp.CROP_Y0, 1024, 768);
        float[] br = eversong.toWorld(WorldMapBlp.CROP_X1, WorldMapBlp.CROP_Y1, 1024, 768);
        WorldMapArea isle = WorldMapAreas.SUNSTRIDER;
        assertEquals(tl[0], isle.locTop(), 0.05f);
        assertEquals(tl[1], isle.locLeft(), 0.05f);
        assertEquals(br[0], isle.locBottom(), 0.05f);
        assertEquals(br[1], isle.locRight(), 0.05f);
    }
}
