package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-052 — SPELL_EFFECT_NORMALIZED_WEAPON_DMG (121). Sinister Strike 1752 Rank 1. */
class SpellEngineNormalizedWeaponDmgTest {
    @Test
    void applyNormalizedWeaponDmgWhenLivingShouldDealWeaponAverage() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_NORMALIZED_WEAPON_DMG));
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo sinister = new SpellEngine.SpellInfo(
                1752, SpellEngine.EFFECT_NORMALIZED_WEAPON_DMG, 0, 0, 0, 0, 0, 5f);
        int dmg = eng.apply(caster, target, sinister);
        assertEquals(2, dmg);
        assertEquals(78, target.health());
    }

    @Test
    void applyNormalizedWeaponDmgWhenDeadShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo sinister = new SpellEngine.SpellInfo(
                1752, SpellEngine.EFFECT_NORMALIZED_WEAPON_DMG, 0, 0, 0, 0, 0, 5f);
        assertEquals(0, eng.apply(new Player(), dead, sinister));
        assertEquals(0, dead.health());
    }
}
