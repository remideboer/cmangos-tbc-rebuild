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
}
