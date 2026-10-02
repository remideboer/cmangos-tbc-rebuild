package org.tbc.world.map;

import org.tbc.world.session.DeathHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveyardManagerTest {
    @Test
    void closestWhenNorthshireShouldReturnElwynnAllianceLoc() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -8949.95f, -132.493f, 83.5312f, GraveyardManager.ALLIANCE, 0);
        assertEquals(DeathHandler.GY_ELWYNN_X, loc.x(), 0.01f);
        assertEquals(DeathHandler.GY_ELWYNN_Y, loc.y(), 0.01f);
    }

    @Test
    void closestWhenColdridgeShouldNotReturnElwynn() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -6240f, 331f, 383f, GraveyardManager.ALLIANCE, 0);
        assertEquals(-6220f, loc.x(), 0.01f);
        assertTrue(Math.abs(loc.x() - DeathHandler.GY_ELWYNN_X) > 100);
    }

    /**
     * CMaNGOS GetClosestGraveYard: area AREALINK, then zone AREALINK. Goldshire
     * (area 87) has no own row; Elwynn Forest zone 12 links world_safe_locs 106.
     */
    @Test
    void closestWhenGoldshireAreaHasNoLinkShouldUseElwynnZoneSpiritHealer() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -9465f, 16f, 57f, GraveyardManager.ALLIANCE, 87, 12);
        assertEquals(106, loc.id());
        assertEquals(-9339.46f, loc.x(), 0.05f);
        assertEquals(171.408f, loc.y(), 0.05f);
    }

    /**
     * TP-SL17-019 — Sunstrider Isle (area 3431) has no AREALINK; Eversong zone 3430 links
     * world_safe_locs 912 (Sunstrider Isle GY), not Horde default / void.
     */
    @Test
    void closestWhenSunstriderAreaShouldUseEversongSunstriderIsleGy() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(530, 10349.6f, -6357.29f, 33.4f,
                GraveyardManager.HORDE, AreaTable.SUNSTRIDER_ISLE, AreaTable.EVERSONG_WOODS);
        assertEquals(912, loc.id());
        assertEquals(530, loc.map());
        assertEquals(10458.5f, loc.x(), 0.5f);
        assertEquals(-6364.61f, loc.y(), 0.5f);
    }

    /**
     * TP-SL17-020 — Shadowglen (area 188) / Teldrassil zone 141 → world_safe_locs 93 Aldrassil,
     * not Alliance continent default (Elwynn) / mid-air void.
     */
    @Test
    void closestWhenShadowglenShouldUseAldrassilSpiritHealer() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(1, 10311.3f, 831.463f, 1326.41f,
                GraveyardManager.ALLIANCE, AreaTable.SHADOWGLEN, AreaTable.TELDRASSIL);
        assertEquals(93, loc.id());
        assertEquals(1, loc.map());
        assertEquals(10384.8f, loc.x(), 0.5f);
        assertEquals(811.531f, loc.y(), 0.5f);
        assertEquals(1317.54f, loc.z(), 0.5f);
    }
}
