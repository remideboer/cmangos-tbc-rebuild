package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-065 — SPELL_EFFECT_SKINNING (95). Skinning 8613. loot.md clientLootType 2. */
class SpellEngineSkinningTest {
    @Test
    void applySkinningWhenCreatureTargetShouldOpenPickpocketingLootAndClearSkinnable() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SKINNING));
        Player caster = new Player();
        Creature corpse = new Creature();
        corpse.guid = 44;
        corpse.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        corpse.setHealth(0);
        corpse.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        SpellEngine.SpellInfo skin = new SpellEngine.SpellInfo(
                8613, SpellEngine.EFFECT_SKINNING, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, corpse, skin);
        assertEquals(0, corpse.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SKINNABLE);
        assertEquals(44, caster.lastSkinningLootGuid());
        WowBuffer b = new WowBuffer(SpellEngine.encodeSkinningLoot(corpse.guid));
        assertEquals(44, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applySkinningWhenNonCreatureOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        Player other = new Player();
        other.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        SpellEngine.SpellInfo skin = new SpellEngine.SpellInfo(
                8613, SpellEngine.EFFECT_SKINNING, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, other, skin);
        assertEquals(Unit.UNIT_FLAG_SKINNABLE, other.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SKINNABLE);
        assertEquals(0, caster.lastSkinningLootGuid());
        Creature corpse = new Creature();
        corpse.guid = 8;
        corpse.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        eng.apply(new Creature(), corpse, skin);
        assertEquals(Unit.UNIT_FLAG_SKINNABLE, corpse.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SKINNABLE);
        eng.skinning(null, corpse);
        eng.skinning(caster, null);
        assertEquals(0, caster.lastSkinningLootGuid());
        assertEquals(Unit.UNIT_FLAG_SKINNABLE, corpse.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SKINNABLE);
    }
}
