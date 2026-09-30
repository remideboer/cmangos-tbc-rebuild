package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.ScriptRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL08-031 — Slain Outrunner 17849 (Dawning Lane) and other Permanent Feign Death corpses:
 * EventAI EVENT_SPAWNED casts spell 29266 so CREATE carries UNIT_FLAG2_FEIGN_DEATH + UNIT_DYNFLAG_DEAD
 * (client corpse animation). ACID: "Cast Permanent Feign Death on Spawn".
 */
class ObjectMgrSlainOutrunnerTest {
    static final int SLAIN_OUTRUNNER = 17849;

    @Test
    void spawnWhenSlainOutrunnerShouldApplyPermanentFeignDeathCorpseFlags() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(SLAIN_OUTRUNNER, 530, -9470f, -189f, 68f, 0f, new ScriptRegistry());
        assertTrue(c.isFeigningDeath(), "FLAGS_2 FEIGN_DEATH must be set before first CREATE");
        assertEquals(Unit.UNIT_DYNFLAG_DEAD,
                c.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & Unit.UNIT_DYNFLAG_DEAD);
        assertEquals(Unit.UNIT_FLAG2_FEIGN_DEATH,
                c.getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & Unit.UNIT_FLAG2_FEIGN_DEATH);
        byte[] create = UpdateBuilder.createUnit(c, false, 0);
        assertTrue(create.length > 0);
    }

    @Test
    void fireEventAiSpawnedWhenNoEventAiShouldNoOp() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.fireEventAiSpawned(null);
        Creature c = new Creature();
        mgr.fireEventAiSpawned(c);
    }
}
