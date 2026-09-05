package org.tbc.world.world;

import org.tbc.world.entity.Creature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRespawnTest {
    @Test
    void tickWhenCorpseRespawnDueShouldRestoreHealth() {
        World world = World.inMemory();
        Creature c = world.objectMgr.spawnCreature(6, 0, 0, 0, 0, 0, world.scripts);
        world.map(0, 0).add(c);
        c.setHealth(0);
        c.respawnDelayMs = 1;
        c.respawnAtMs = world.nowMs();
        world.tick(50);
        assertTrue(c.alive());
        assertEquals(c.maxHealth(), c.health());
    }
}
