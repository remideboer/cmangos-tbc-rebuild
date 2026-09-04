package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.WorldSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionMasterTest {
    @Test
    void chaseWhenOutOfMeleeShouldAdvanceTowardVictim() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        c.motion.update(c, 1000);
        assertEquals(20f - UpdateBuilder.RUN, c.x, 0.01f);
        assertTrue(c.distance2d(p) > WorldSession.MELEE_RANGE);
    }

    @Test
    void wanderWhenSpawnDistShouldMoveWithinRadius() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(0, 0, 0, 0);
        c.spawnX = 0;
        c.spawnY = 0;
        c.spawnZ = 0;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.motion.moveRandom(10f);
        byte[] spline = c.motion.update(c, 1000);
        assertNotNull(spline);
        assertEquals(UpdateBuilder.WALK, c.x, 0.01f);
        assertEquals(0f, c.y, 0.01f);
        assertTrue(c.spawnDistance2d(c.x, c.y) <= 10f + 0.01f);
    }

    @Test
    void startOocMotionWhenRandomTypeShouldResumeAfterChase() {
        Creature c = new Creature();
        c.movementType = 1;
        c.spawnDist = 8f;
        c.startOocMotion();
        assertEquals(MotionMaster.RANDOM, c.motion.type());
        Player p = new Player();
        p.guid = 1;
        c.motion.moveChase(p);
        assertEquals(MotionMaster.CHASE, c.motion.type());
        c.startOocMotion();
        assertEquals(MotionMaster.RANDOM, c.motion.type());
    }
}
