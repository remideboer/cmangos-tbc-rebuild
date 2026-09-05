package org.tbc.world.world;

import org.tbc.world.entity.Creature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldTickActiveGridTest {
    @Test
    void tickWhenNoPlayerNearbyShouldNotAdvanceFarRandomMotion() {
        World world = World.inMemory();
        Creature c = world.objectMgr.spawnCreature(6, 0, 0, 0, 0, 0, world.scripts);
        world.map(0, 0).add(c);
        c.movementType = 1;
        c.spawnDist = 10f;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.startOocMotion();
        float x = c.x;
        float y = c.y;
        world.tick(1000);
        assertEquals(x, c.x, 1e-4f);
        assertEquals(y, c.y, 1e-4f);
    }
}
