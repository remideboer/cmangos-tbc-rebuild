package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
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
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_SKINNING));
        Player caster = new Player();
        caster.learnSkill(Content.SKILL_SKINNING, 1, 75, 1);
        Creature corpse = new Creature();
        corpse.guid = 44;
        corpse.level = 1;
        corpse.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        corpse.setHealth(0);
        corpse.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        SpellEngine.SpellInfo skin = new SpellEngine.SpellInfo(
                8613, SpellEngine.EFFECT_SKINNING, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, corpse, skin);
        assertEquals(0, corpse.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_SKINNABLE);
        assertEquals(44, caster.lastSkinningLootGuid());
        assertEquals(2, caster.skillValue(Content.SKILL_SKINNING));
        WowBuffer b = new WowBuffer(SpellEngine.encodeSkinningLoot(corpse.guid));
        assertEquals(44, b.getU64());
        assertEquals(2, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(Opcodes.SMSG_LOOT_RESPONSE, 0x160);
    }

    @Test
    void applySkinningWhenLevelBandsAndEliteShouldUseReqAndMultiplicator() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        caster.learnSkill(Content.SKILL_SKINNING, 1, 300, 1);
        Creature mid = new Creature();
        mid.guid = 50;
        mid.level = 15;
        mid.setHealth(0);
        mid.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        eng.skinning(caster, mid);
        assertEquals(2, caster.skillValue(Content.SKILL_SKINNING));
        Creature high = new Creature();
        high.guid = 51;
        high.level = 25;
        high.rank = 1;
        high.setHealth(0);
        high.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        eng.skinning(caster, high);
        assertEquals(3, caster.skillValue(Content.SKILL_SKINNING));
    }

    @Test
    void applySkinningWhenNoSkillLearnedShouldSkipGatherGain() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Player caster = new Player();
        Creature corpse = new Creature();
        corpse.guid = 60;
        corpse.level = 1;
        corpse.setHealth(0);
        corpse.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SKINNABLE);
        eng.skinning(caster, corpse);
        assertEquals(0, caster.skillValue(Content.SKILL_SKINNING));
        assertEquals(60, caster.lastSkinningLootGuid());
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
