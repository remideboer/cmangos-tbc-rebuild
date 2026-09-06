package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-079 — SPELL_EFFECT_SUMMON_OBJECT_SLOT1 (104). Freezing Trap 1499 misc 2561.
 * CMaNGOS EffectSummonObject slot 0; player SMSG_TOTEM_CREATED.
 */
class SpellEngineSummonObjectSlot1Test {
    @Test
    void applySummonObjectSlot1WhenPlayerShouldPlaceTrapAndEncodeTotemCreated() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1));
        Player hunter = new Player();
        hunter.guid = 7;
        hunter.relocate(10f, 20f, 30f, 1.5f);
        SpellEngine.SpellInfo trap = new SpellEngine.SpellInfo(
                1499, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1, 0, 0, 0, 0, 0, 0f, 2561);
        eng.apply(hunter, hunter, trap);
        GameObject go = hunter.objectSlot(0);
        assertNotNull(go);
        assertEquals(2561, go.entry);
        assertEquals(10f, go.x, 0.01f);
        assertEquals(20f, go.y, 0.01f);
        assertEquals(30f, go.z, 0.01f);
        assertEquals(go.guid, hunter.lastTotemCreatedGuid());
        assertEquals(0, hunter.lastTotemCreatedSlot());
        WowBuffer b = new WowBuffer(SpellEngine.encodeTotemCreated(hunter.lastTotemCreatedSlot(), go.guid));
        assertEquals(0, b.getU8());
        assertEquals(go.guid, b.getU64());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(Opcodes.SMSG_TOTEM_CREATED, 0x412);
        eng.apply(hunter, hunter, trap);
        assertNotSame(go, hunter.objectSlot(0));
        assertEquals(2561, hunter.objectSlot(0).entry);
    }

    @Test
    void applySummonObjectSlot1WhenMissingEntryOrNonPlayerShouldNoOpOrSkipTotem() {
        SpellEngine eng = new SpellEngine();
        Player hunter = new Player();
        hunter.guid = 3;
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                1499, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(hunter, hunter, none);
        assertNull(hunter.objectSlot(0));
        Creature npc = new Creature();
        npc.guid = 4;
        npc.relocate(1f, 2f, 3f, 0f);
        SpellEngine.SpellInfo trap = new SpellEngine.SpellInfo(
                1499, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1, 0, 0, 0, 0, 0, 0f, 2561);
        eng.apply(npc, npc, trap);
        assertNotNull(npc.objectSlot(0));
        assertEquals(2561, npc.objectSlot(0).entry);
        eng.summonObjectSlot(null, 0, 2561);
        eng.summonObjectSlot(hunter, -1, 2561);
        eng.summonObjectSlot(hunter, 4, 2561);
        eng.summonObjectSlot(hunter, 0, 0);
        assertNull(hunter.objectSlot(0));
    }
}
