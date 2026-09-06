package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-095 — SPELL_EFFECT_ENCHANT_ITEM_TEMPORARY (54). Deadly Poison 2823 misc 7.
 * CMaNGOS EffectEnchantItemTmp: TEMP_ENCHANTMENT_SLOT on the cast item.
 */
class SpellEngineEnchantItemTemporaryTest {
    @Test
    void applyEnchantItemTemporaryWhenCastItemShouldSetTempEnchant() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY));
        Player caster = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, sword);
        caster.setSpellItemTarget(sword);
        SpellEngine.SpellInfo poison = new SpellEngine.SpellInfo(
                2823, SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY, 0, 0, 0, 0, 0, 0f, 7);
        eng.apply(caster, caster, poison);
        assertEquals(7, sword.tempEnchant);
    }

    @Test
    void applyEnchantItemTemporaryWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo poison = new SpellEngine.SpellInfo(
                2823, SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY, 0, 0, 0, 0, 0, 0f, 7);
        eng.apply(new Creature(), new Creature(), poison);
        Player caster = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, sword);
        eng.apply(caster, caster, poison);
        assertEquals(0, sword.tempEnchant);
        caster.setSpellItemTarget(sword);
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                2823, SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(caster, caster, none);
        assertEquals(0, sword.tempEnchant);
        eng.enchantItemTemporary(null, sword, 7);
        eng.enchantItemTemporary(caster, null, 7);
        eng.enchantItemTemporary(caster, sword, 0);
        assertEquals(0, sword.tempEnchant);
        eng.enchantItemTemporary(caster, sword, 7);
        assertEquals(7, sword.tempEnchant);
        eng.enchantItemTemporary(caster, sword, 8);
        assertEquals(8, sword.tempEnchant);
    }
}
