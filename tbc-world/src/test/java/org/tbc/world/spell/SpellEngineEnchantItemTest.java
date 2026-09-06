package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-093 — SPELL_EFFECT_ENCHANT_ITEM (53). Sharpen Blade 2605 misc 1.
 * CMaNGOS EffectEnchantItemPerm: PERM_ENCHANTMENT_SLOT on the cast item.
 */
class SpellEngineEnchantItemTest {
    @Test
    void applyEnchantItemWhenCastItemShouldSetPermEnchant() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENCHANT_ITEM));
        Player caster = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, sword);
        caster.setSpellItemTarget(sword);
        SpellEngine.SpellInfo sharpen = new SpellEngine.SpellInfo(
                2605, SpellEngine.EFFECT_ENCHANT_ITEM, 0, 0, 0, 0, 0, 0f, 1);
        eng.apply(caster, caster, sharpen);
        assertEquals(1, sword.enchant);
    }

    @Test
    void applyEnchantItemWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo sharpen = new SpellEngine.SpellInfo(
                2605, SpellEngine.EFFECT_ENCHANT_ITEM, 0, 0, 0, 0, 0, 0f, 1);
        eng.apply(new Creature(), new Creature(), sharpen);
        Player caster = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, sword);
        eng.apply(caster, caster, sharpen);
        assertEquals(0, sword.enchant);
        caster.setSpellItemTarget(sword);
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                2605, SpellEngine.EFFECT_ENCHANT_ITEM, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(caster, caster, none);
        assertEquals(0, sword.enchant);
        eng.enchantItem(null, sword, 1);
        eng.enchantItem(caster, null, 1);
        eng.enchantItem(caster, sword, 0);
        assertEquals(0, sword.enchant);
        eng.enchantItem(caster, sword, 1);
        assertEquals(1, sword.enchant);
        eng.enchantItem(caster, sword, 15);
        assertEquals(15, sword.enchant);
    }
}
