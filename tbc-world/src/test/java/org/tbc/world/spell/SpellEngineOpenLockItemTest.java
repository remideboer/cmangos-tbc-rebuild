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

/** TP-SL26-071 — SPELL_EFFECT_OPEN_LOCK_ITEM (59). Opening 3366. Same EffectOpenLock as 33. */
class SpellEngineOpenLockItemTest {
    @Test
    void applyOpenLockItemWhenItemTargetShouldUnlockAndOpenLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_OPEN_LOCK_ITEM));
        Player caster = new Player();
        Item crate = new Item(10, Content.ITEM_DENTED_CRATE);
        caster.items.put(10, crate);
        caster.setSpellItemTarget(crate);
        SpellEngine.SpellInfo opening = new SpellEngine.SpellInfo(
                3366, SpellEngine.EFFECT_OPEN_LOCK_ITEM, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), opening);
        assertEquals(Content.ITEM_DYNFLAG_UNLOCKED, crate.flags & Content.ITEM_DYNFLAG_UNLOCKED);
        assertEquals(10, caster.lastOpenLockLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeOpenLockLoot(crate.guid));
        assertEquals(10, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applyOpenLockItemWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo opening = new SpellEngine.SpellInfo(
                3366, SpellEngine.EFFECT_OPEN_LOCK_ITEM, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), opening);
        assertEquals(0, caster.lastOpenLockLootGuid());
        Item crate = new Item(10, Content.ITEM_DENTED_CRATE);
        eng.apply(new Creature(), new Creature(), opening);
        assertEquals(0, crate.flags);
        assertEquals(0, caster.lastOpenLockLootGuid());
    }
}
