package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-036 — SPELL_EFFECT_DURABILITY_DAMAGE_PCT (115). Corrupt Weapon 23436. */
class SpellEngineDurabilityDamagePctTest {
    @Test
    void applyDurabilityDamagePctWhenEquippedShouldLosePercentOfMax() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT));
        Player p = new Player();
        Item bow = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        bow.bag = 0;
        bow.slot = Player.EQUIPMENT_SLOT_RANGED;
        bow.durability = 100;
        bow.maxDurability = 100;
        p.items.put(1, bow);
        SpellEngine.SpellInfo corrupt = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 100, 100, 0f,
                Player.EQUIPMENT_SLOT_RANGED);
        eng.apply(new Player(), p, corrupt);
        assertEquals(0, bow.durability);
    }

    @Test
    void applyDurabilityDamagePctWhenNonPlayerZeroMaxOrZeroPctShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        SpellEngine.SpellInfo corrupt = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 100, 100, 0f,
                Player.EQUIPMENT_SLOT_RANGED);
        assertEquals(0, eng.apply(new Player(), c, corrupt));
        Player p = new Player();
        Item bow = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        bow.bag = 0;
        bow.slot = Player.EQUIPMENT_SLOT_RANGED;
        bow.durability = 80;
        bow.maxDurability = 0;
        p.items.put(1, bow);
        eng.apply(new Player(), p, corrupt);
        assertEquals(80, bow.durability);
        bow.maxDurability = 100;
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 0, 0, 0f,
                Player.EQUIPMENT_SLOT_RANGED);
        eng.apply(new Player(), p, zero);
        assertEquals(80, bow.durability);
        SpellEngine.SpellInfo invalid = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 100, 100, 0f, -1);
        eng.apply(new Player(), p, invalid);
        assertEquals(80, bow.durability);
        SpellEngine.SpellInfo pastEnd = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 100, 100, 0f,
                Player.INVENTORY_SLOT_BAG_END);
        eng.apply(new Player(), p, pastEnd);
        assertEquals(80, bow.durability);
        SpellEngine.SpellInfo empty = new SpellEngine.SpellInfo(
                23436, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT, 0, 0, 0, 100, 100, 0f,
                Player.EQUIPMENT_SLOT_OFFHAND);
        eng.apply(new Player(), p, empty);
        assertEquals(80, bow.durability);
    }
}
