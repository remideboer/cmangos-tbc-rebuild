package org.tbc.world.spell;

import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-087 — SPELL_EFFECT_SUMMON_CHANGE_ITEM (34). Summon Thunderstrike 21180 EffectItemType 17223.
 * CMaNGOS ConvertItem on the cast item.
 */
class SpellEngineSummonChangeItemTest {
    @Test
    void applySummonChangeItemWhenCastItemShouldConvertEntry() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SUMMON_CHANGE_ITEM));
        Player caster = new Player();
        Item old = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, old);
        caster.setSpellItemTarget(old);
        SpellEngine.SpellInfo thunder = new SpellEngine.SpellInfo(
                21180, SpellEngine.EFFECT_SUMMON_CHANGE_ITEM, 0, 0, 0, 0, 0, 0f, 17223);
        eng.apply(caster, caster, thunder);
        assertEquals(17223, old.entry);
        assertEquals(17223, caster.items.get(1).entry);
    }

    @Test
    void applySummonChangeItemWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo thunder = new SpellEngine.SpellInfo(
                21180, SpellEngine.EFFECT_SUMMON_CHANGE_ITEM, 0, 0, 0, 0, 0, 0f, 17223);
        eng.apply(new Creature(), new Creature(), thunder);
        Player caster = new Player();
        Item old = new Item(1, Content.ITEM_WORN_SHORTSWORD);
        caster.items.put(1, old);
        eng.apply(caster, caster, thunder);
        assertEquals(Content.ITEM_WORN_SHORTSWORD, old.entry);
        caster.setSpellItemTarget(old);
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                21180, SpellEngine.EFFECT_SUMMON_CHANGE_ITEM, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(caster, caster, none);
        assertEquals(Content.ITEM_WORN_SHORTSWORD, old.entry);
        eng.summonChangeItem(null, old, 17223);
        eng.summonChangeItem(caster, null, 17223);
        assertEquals(Content.ITEM_WORN_SHORTSWORD, old.entry);
    }
}
