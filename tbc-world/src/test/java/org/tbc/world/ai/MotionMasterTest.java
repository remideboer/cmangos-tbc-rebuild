package org.tbc.world.ai;

import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.WorldSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void chaseWhenLaunchingShouldFaceVictimGuid() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        byte[] spline = c.motion.update(c, 50);
        assertNotNull(spline);
        WowBuffer pkt = new WowBuffer(spline);
        pkt.getPackedGuid();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getU32();
        assertEquals(TaxiHandler.MONSTER_MOVE_FACING_TARGET, pkt.getU8());
        assertEquals(p.guid, pkt.getU64());
        pkt.getU32();
        pkt.getU32();
        assertEquals(1, pkt.getU32());
    }

    @Test
    void chaseWhenVictimSidestepsShouldSendNewSpline() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        assertNotNull(c.motion.update(c, 50));
        p.relocate(1, 0, 0, 0);
        assertNotNull(c.motion.update(c, MotionMaster.CHASE_REACTION_MS));
    }

    @Test
    void chaseWhenVictimMovesBeforeReactionMsShouldKeepCurrentSpline() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        assertNotNull(c.motion.update(c, 50));
        p.relocate(1, 0, 0, 0);
        assertEquals(null, c.motion.update(c, 50));
    }

    @Test
    void chaseWhenVictimNudgesShouldRepathWithinReactionMs() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        assertNotNull(c.motion.update(c, 50));
        p.relocate(0.1f, 0, 0, 0);
        assertNotNull(c.motion.update(c, MotionMaster.CHASE_REACTION_MS));
    }

    @Test
    void chaseWhenRepathShouldUseNewSplineId() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        byte[] first = c.motion.update(c, 50);
        p.relocate(1, 0, 0, 0);
        byte[] second = c.motion.update(c, MotionMaster.CHASE_REACTION_MS);
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(splineId(first), splineId(second));
    }

    @Test
    void chaseWhenInMeleeVictimCirclesShouldKeepFacingOnWire() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(5, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        assertNotNull(c.motion.update(c, 50));
        p.relocate(0, 5, 0, 0);
        byte[] face = c.motion.update(c, MotionMaster.CHASE_REACTION_MS);
        assertNotNull(face);
        WowBuffer pkt = new WowBuffer(face);
        pkt.getPackedGuid();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getU32();
        assertEquals(TaxiHandler.MONSTER_MOVE_FACING_TARGET, pkt.getU8());
        assertEquals(p.guid, pkt.getU64());
        assertEquals((float) Math.atan2(5, -5), c.o, 0.05f);
    }

    @Test
    void chaseWhenArrivingShouldStopAtMeleeAndFaceVictim() {
        Creature c = new Creature();
        c.guid = 2;
        c.relocate(20, 0, 0, 0);
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        c.motion.moveChase(p);
        c.motion.update(c, 3000);
        assertEquals(Combat.meleeRange(c), c.distance2d(p), 0.05f);
        assertEquals((float) Math.PI, c.o, 0.05f);
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
        WowBuffer pkt = new WowBuffer(spline);
        pkt.getPackedGuid();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getU32();
        pkt.getU8();
        pkt.getU32();
        pkt.getU32();
        assertEquals(1, pkt.getU32());
        byte[] again = c.motion.update(c, 50);
        assertEquals(null, again);
        assertTrue(c.x > UpdateBuilder.WALK);
    }

    @Test
    void wanderWhenGroundLookupShouldUseTerrainZ() {
        Creature c = new Creature();
        c.guid = 2;
        c.mapId = 0;
        c.relocate(0, 0, 12, 0);
        c.spawnX = 0;
        c.spawnY = 0;
        c.spawnZ = 12;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.motion.moveRandom(10f);
        byte[] spline = c.motion.update(c, 1000, (map, x, y, hint) -> 40f);
        assertNotNull(spline);
        WowBuffer pkt = new WowBuffer(spline);
        pkt.getPackedGuid();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getU32();
        pkt.getU8();
        pkt.getU32();
        pkt.getU32();
        pkt.getU32();
        pkt.getFloat();
        pkt.getFloat();
        assertEquals(40f, pkt.getFloat(), 0.01f);
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

    @Test
    void homeWhenAwayShouldEmitSplineAndAdvanceTowardSpawn() {
        Creature c = new Creature();
        c.guid = 2;
        c.spawnX = 0;
        c.spawnY = 0;
        c.spawnZ = 0;
        c.relocate(20, 0, 0, 0);
        c.motion.moveHome();
        byte[] spline = c.motion.update(c, 50);
        assertNotNull(spline);
        assertEquals(MotionMaster.HOME, c.motion.type());
        c.motion.update(c, 1000);
        assertTrue(c.x < 20f);
    }

    private static int splineId(byte[] spline) {
        WowBuffer pkt = new WowBuffer(spline);
        pkt.getPackedGuid();
        pkt.getFloat();
        pkt.getFloat();
        pkt.getFloat();
        return pkt.getU32();
    }
}
