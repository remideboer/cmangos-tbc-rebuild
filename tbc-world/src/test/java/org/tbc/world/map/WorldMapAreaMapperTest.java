package org.tbc.world.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorldMapArea loc* → image pixels (north-up, origin top-left).
 * CMaNGOS MapCoordinateVsZoneCheck: x2 ≤ worldX ≤ x1, y2 ≤ worldY ≤ y1.
 */
class WorldMapAreaMapperTest {
    /** Spell.dbc-era 2.4.3 Elwynn loc* (contains Goldshire −9465, 62). */
    private static final WorldMapArea ELWYNN = new WorldMapArea(
            AreaTable.ELWYNN_FOREST, 0, "Elwynn", "Elwynn Forest",
            1535.42f, -3766.05f, -7943.22f, -11176.3f);

    @Test
    void toWorldWhenPixelCornersShouldMatchLocTopLeftAndBottomRight() {
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        float[] tl = map.toWorld(0, 0, 100, 50);
        assertEquals(ELWYNN.locTop(), tl[0], 1e-3f);
        assertEquals(ELWYNN.locLeft(), tl[1], 1e-3f);
        float[] br = map.toWorld(100, 50, 100, 50);
        assertEquals(ELWYNN.locBottom(), br[0], 1e-3f);
        assertEquals(ELWYNN.locRight(), br[1], 1e-3f);
    }

    @Test
    void toPixelWhenGoldshireShouldLieInsideElwynnImageAndRoundTrip() {
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        assertTrue(map.contains(-9465f, 62f));
        float[] px = map.toPixel(-9465f, 62f, 200, 100);
        assertTrue(px[0] > 0 && px[0] < 200);
        assertTrue(px[1] > 0 && px[1] < 100);
        float[] world = map.toWorld(px[0], px[1], 200, 100);
        assertEquals(-9465f, world[0], 1e-3f);
        assertEquals(62f, world[1], 1e-3f);
    }

    /**
     * Client map percent: horizontal is world Y (locLeft → locRight), vertical is world X (locTop → locBottom).
     * Goldshire (−9465, 62) is about 28% across Elwynn and 47% down.
     */
    @Test
    void toPixelWhenGoldshireShouldFollowWorldYAcrossAndWorldXDown() {
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        float across = (ELWYNN.locLeft() - 62f) / (ELWYNN.locLeft() - ELWYNN.locRight());
        float down = (ELWYNN.locTop() - (-9465f)) / (ELWYNN.locTop() - ELWYNN.locBottom());
        float[] px = map.toPixel(-9465f, 62f, 200, 100);
        assertEquals(across * 200f, px[0], 0.05f);
        assertEquals(down * 100f, px[1], 0.05f);
    }

    @Test
    void containsWhenOutsideLocBoxShouldBeFalse() {
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        assertFalse(map.contains(0f, 0f));
        assertFalse(map.contains(-9465f, 8000f));
    }

    @Test
    void toWorldWhenZeroImageSizeShouldStayAtTopLeft() {
        WorldMapAreaMapper map = new WorldMapAreaMapper(ELWYNN);
        assertArrayEquals(new float[]{ELWYNN.locTop(), ELWYNN.locLeft()},
                map.toWorld(10, 10, 0, 0), 1e-3f);
    }
}
