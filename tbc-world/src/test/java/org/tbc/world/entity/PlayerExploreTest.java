package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL04-023 — Player::CheckAreaExploreAndOutdoor sets PLAYER_EXPLORED_ZONES bits so the client
 * fog-of-war uncovers (AreaTable exploreFlag / map area flag).
 */
class PlayerExploreTest {
    static final int AREA_FLAG = 44; // synthetic exploreFlag (offset 1, bit 12)

    @Test
    void exploreAreaFlagWhenNewShouldSetExploredZonesBit() {
        Player p = new Player();
        p.applyCreateFields();
        int field = p.exploreAreaFlag(AREA_FLAG);
        assertEquals(UpdateFields.PLAYER_EXPLORED_ZONES_1 + AREA_FLAG / 32, field);
        int bit = 1 << (AREA_FLAG % 32);
        assertEquals(bit, p.getInt(field) & bit);
    }

    @Test
    void exploreAreaFlagWhenAlreadyKnownShouldReturnNoChange() {
        Player p = new Player();
        p.applyCreateFields();
        assertTrue(p.exploreAreaFlag(AREA_FLAG) >= 0);
        assertEquals(-1, p.exploreAreaFlag(AREA_FLAG));
    }

    @Test
    void exploreAreaFlagWhenInvalidShouldIgnore() {
        Player p = new Player();
        p.applyCreateFields();
        assertEquals(-1, p.exploreAreaFlag(0));
        assertEquals(-1, p.exploreAreaFlag(0xFFFF));
        assertEquals(-1, p.exploreAreaFlag(128 * 32));
    }
}
