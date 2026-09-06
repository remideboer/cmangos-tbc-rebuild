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

/** TP-SL26-070 — SPELL_EFFECT_OPEN_LOCK (33). Opening 3365. Item.h ITEM_DYNFLAG_UNLOCKED. */
class SpellEngineOpenLockTest {
    @Test
    void applyOpenLockWhenItemTargetShouldUnlockAndOpenLoot() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_OPEN_LOCK));
        Player caster = new Player();
        Item crate = new Item(9, Content.ITEM_DENTED_CRATE);
        caster.items.put(9, crate);
        caster.setSpellItemTarget(crate);
        SpellEngine.SpellInfo opening = new SpellEngine.SpellInfo(
                3365, SpellEngine.EFFECT_OPEN_LOCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), opening);
        assertEquals(Content.ITEM_DYNFLAG_UNLOCKED, crate.flags & Content.ITEM_DYNFLAG_UNLOCKED);
        assertEquals(9, caster.lastOpenLockLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeOpenLockLoot(crate.guid));
        assertEquals(9, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applyOpenLockWhenMissingItemOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        SpellEngine.SpellInfo opening = new SpellEngine.SpellInfo(
                3365, SpellEngine.EFFECT_OPEN_LOCK, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, new Creature(), opening);
        assertEquals(0, caster.lastOpenLockLootGuid());
        Item crate = new Item(9, Content.ITEM_DENTED_CRATE);
        eng.apply(new Creature(), new Creature(), opening);
        eng.openLock(null, crate);
        eng.openLock(caster, null);
        assertEquals(0, crate.flags);
        assertEquals(0, caster.lastOpenLockLootGuid());
    }
}
