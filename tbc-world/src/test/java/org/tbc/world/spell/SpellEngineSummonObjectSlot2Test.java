package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-080 — SPELL_EFFECT_SUMMON_OBJECT_SLOT2 (105). Battle Standard 22996 misc 179604.
 * CMaNGOS EffectSummonObject slot 1; player SMSG_TOTEM_CREATED.
 */
class SpellEngineSummonObjectSlot2Test {
    @Test
    void applySummonObjectSlot2WhenPlayerShouldPlaceStandardInSlot1() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_OBJECT_SLOT2));
        Player p = new Player();
        p.guid = 11;
        p.relocate(4f, 5f, 6f, 0f);
        SpellEngine.SpellInfo banner = new SpellEngine.SpellInfo(
                22996, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT2, 0, 0, 0, 0, 0, 0f, 179604);
        eng.apply(p, p, banner);
        GameObject go = p.objectSlot(1);
        assertNotNull(go);
        assertEquals(179604, go.entry);
        assertNull(p.objectSlot(0));
        assertEquals(1, p.lastTotemCreatedSlot());
        assertEquals(go.guid, p.lastTotemCreatedGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeTotemCreated(1, go.guid));
        assertEquals(1, b.getU8());
        assertEquals(go.guid, b.getU64());
        assertEquals(Opcodes.SMSG_TOTEM_CREATED, 0x412);
    }

    @Test
    void applySummonObjectSlot2WhenMissingEntryShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                22996, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT2, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(p, p, none);
        assertNull(p.objectSlot(1));
    }
}
