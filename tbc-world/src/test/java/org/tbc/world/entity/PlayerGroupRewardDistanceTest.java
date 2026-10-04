package org.tbc.world.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CMaNGOS Player::IsAtGroupRewardDistance — MaxGroupXPDistance 74. */
class PlayerGroupRewardDistanceTest {
    @Test
    void isAtGroupRewardDistanceWhenNullOrOtherMapShouldBeFalse() {
        Player p = new Player();
        p.mapId = 0;
        p.relocate(0, 0, 0, 0);
        assertFalse(p.isAtGroupRewardDistance(null));
        Creature otherMap = new Creature();
        otherMap.mapId = 1;
        otherMap.relocate(0, 0, 0, 0);
        assertFalse(p.isAtGroupRewardDistance(otherMap));
    }

    @Test
    void isAtGroupRewardDistanceWhenInsideOrBeyondSeventyFourYards() {
        Player p = new Player();
        p.mapId = 0;
        p.relocate(0, 0, 0, 0);
        Creature near = new Creature();
        near.mapId = 0;
        near.relocate(74, 0, 0, 0);
        assertTrue(p.isAtGroupRewardDistance(near));
        Creature far = new Creature();
        far.mapId = 0;
        far.relocate(74.1f, 0, 0, 0);
        assertFalse(p.isAtGroupRewardDistance(far));
    }
}
