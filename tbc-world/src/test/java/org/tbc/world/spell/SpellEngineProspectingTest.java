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

/** TP-SL26-069 — SPELL_EFFECT_PROSPECTING (127). Prospecting 31252. loot.md item window. */
class SpellEngineProspectingTest {
    @Test
    void applyProspectingWhenItemTargetShouldOpenPickpocketingLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PROSPECTING));
        Player caster = new Player();
        Item ore = new Item(8, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(8, ore);
        caster.setSpellItemTarget(ore);
        SpellEngine.SpellInfo prospect = new SpellEngine.SpellInfo(
                31252, SpellEngine.EFFECT_PROSPECTING, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), prospect);
        assertEquals(8, caster.lastProspectingLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeProspectingLoot(ore.guid));
        assertEquals(8, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applyProspectingWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo prospect = new SpellEngine.SpellInfo(
                31252, SpellEngine.EFFECT_PROSPECTING, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), prospect);
        assertEquals(0, caster.lastProspectingLootGuid());
        Item ore = new Item(8, Content.ITEM_WORN_SHORTSWORD);
        eng.apply(new Creature(), new Creature(), prospect);
        eng.prospecting(null, ore);
        eng.prospecting(caster, null);
        assertEquals(0, caster.lastProspectingLootGuid());
    }
}
