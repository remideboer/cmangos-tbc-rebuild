package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-032 — SPELL_EFFECT_DURABILITY_DAMAGE (111). Melt Weapon 21388. */
class SpellEngineDurabilityDamageTest {
    @Test
    void applyDurabilityDamageWhenEquippedSlotShouldSubtractPoints() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DURABILITY_DAMAGE));
        Player p = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        sword.durability = 20;
        p.items.put(1, sword);
        SpellEngine.SpellInfo melt = new SpellEngine.SpellInfo(
                21388, SpellEngine.EFFECT_DURABILITY_DAMAGE, 0, 0, 0, 10, 10, 0f,
                Player.EQUIPMENT_SLOT_MAINHAND);
        eng.apply(new Player(), p, melt);
        assertEquals(10, sword.durability);
    }

    @Test
    void applyDurabilityDamageWhenNonPlayerInvalidSlotOrMissingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        SpellEngine.SpellInfo melt = new SpellEngine.SpellInfo(
                21388, SpellEngine.EFFECT_DURABILITY_DAMAGE, 0, 0, 0, 10, 10, 0f,
                Player.EQUIPMENT_SLOT_MAINHAND);
        assertEquals(0, eng.apply(new Player(), c, melt));
        Player p = new Player();
        Item sword = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        sword.bag = 0;
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        sword.durability = 20;
        p.items.put(1, sword);
        SpellEngine.SpellInfo bagSlot = new SpellEngine.SpellInfo(
                21388, SpellEngine.EFFECT_DURABILITY_DAMAGE, 0, 0, 0, 10, 10, 0f,
                Player.INVENTORY_SLOT_BAG_END);
        eng.apply(new Player(), p, bagSlot);
        assertEquals(20, sword.durability);
        SpellEngine.SpellInfo empty = new SpellEngine.SpellInfo(
                21388, SpellEngine.EFFECT_DURABILITY_DAMAGE, 0, 0, 0, 10, 10, 0f,
                Player.EQUIPMENT_SLOT_OFFHAND);
        eng.apply(new Player(), p, empty);
        assertEquals(20, sword.durability);
        SpellEngine.SpellInfo overkill = new SpellEngine.SpellInfo(
                21388, SpellEngine.EFFECT_DURABILITY_DAMAGE, 0, 0, 0, 50, 50, 0f,
                Player.EQUIPMENT_SLOT_MAINHAND);
        eng.apply(new Player(), p, overkill);
        assertEquals(0, sword.durability);
    }
}
