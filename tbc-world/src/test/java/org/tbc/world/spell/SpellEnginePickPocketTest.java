package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-066 — SPELL_EFFECT_PICKPOCKET (71). Pick Pocket 921. loot.md clientLootType 2. */
class SpellEnginePickPocketTest {
    @Test
    void applyPickPocketWhenCreatureTargetShouldOpenPickpocketingLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PICKPOCKET));
        Player caster = new Player();
        Creature victim = new Creature();
        victim.guid = 55;
        SpellEngine.SpellInfo pick = new SpellEngine.SpellInfo(
                921, SpellEngine.EFFECT_PICKPOCKET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, victim, pick);
        assertEquals(55, caster.lastPickpocketLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodePickpocketLoot(victim.guid));
        assertEquals(55, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applyPickPocketWhenNonCreatureOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        Player other = new Player();
        other.guid = 9;
        SpellEngine.SpellInfo pick = new SpellEngine.SpellInfo(
                921, SpellEngine.EFFECT_PICKPOCKET, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, other, pick);
        assertEquals(0, caster.lastPickpocketLootGuid());
        Creature victim = new Creature();
        victim.guid = 8;
        eng.apply(new Creature(), victim, pick);
        eng.pickPocket(null, victim);
        eng.pickPocket(caster, null);
        assertEquals(0, caster.lastPickpocketLootGuid());
    }
}
