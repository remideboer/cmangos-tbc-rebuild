package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-072 — SPELL_EFFECT_ENCHANT_HELD_ITEM (92). Flametongue Totem Effect 8230 misc 124. */
class SpellEngineEnchantHeldItemTest {
    @Test
    void applyEnchantHeldItemWhenMainhandShouldSetTempEnchant() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_ENCHANT_HELD_ITEM));
        Player target = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        target.items.put(1, sword);
        SpellEngine.SpellInfo flame = new SpellEngine.SpellInfo(
                8230, SpellEngine.EFFECT_ENCHANT_HELD_ITEM, 0, 0, 0, 0, 0, 0f, 124);
        eng.apply(new Creature(), target, flame);
        assertEquals(124, sword.tempEnchant);
    }

    @Test
    void applyEnchantHeldItemWhenNonPlayerMissingOrOtherTempShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo flame = new SpellEngine.SpellInfo(
                8230, SpellEngine.EFFECT_ENCHANT_HELD_ITEM, 0, 0, 0, 0, 0, 0f, 124);
        eng.apply(new Creature(), new Creature(), flame);
        Player target = new Player();
        eng.apply(new Creature(), target, flame);
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        target.items.put(1, sword);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                8230, SpellEngine.EFFECT_ENCHANT_HELD_ITEM, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Creature(), target, zero);
        assertEquals(0, sword.tempEnchant);
        sword.tempEnchant = 1;
        eng.apply(new Creature(), target, flame);
        assertEquals(1, sword.tempEnchant);
        sword.tempEnchant = 124;
        eng.apply(new Creature(), target, flame);
        assertEquals(124, sword.tempEnchant);
        eng.enchantHeldItem(null, 124);
        eng.enchantHeldItem(target, 0);
    }
}
