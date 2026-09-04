package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.WorldSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
