package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-068 — SPELL_EFFECT_DISENCHANT (99). Disenchant 13262. loot.md item window. */
class SpellEngineDisenchantTest {
    @Test
    void applyDisenchantWhenItemTargetShouldOpenPickpocketingLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DISENCHANT));
        Player caster = new Player();
        Item sword = new Item(7, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(7, sword);
        caster.setSpellItemTarget(sword);
        SpellEngine.SpellInfo de = new SpellEngine.SpellInfo(
                13262, SpellEngine.EFFECT_DISENCHANT, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), de);
        assertEquals(7, caster.lastDisenchantLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeDisenchantLoot(sword.guid));
        assertEquals(7, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applyDisenchantWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo de = new SpellEngine.SpellInfo(
                13262, SpellEngine.EFFECT_DISENCHANT, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), de);
        assertEquals(0, caster.lastDisenchantLootGuid());
        Item sword = new Item(7, Content.ITEM_WORN_SHORTSWORD);
        eng.apply(new Creature(), new Creature(), de);
        eng.disenchant(null, sword);
        eng.disenchant(caster, null);
        assertEquals(0, caster.lastDisenchantLootGuid());
    }
}
