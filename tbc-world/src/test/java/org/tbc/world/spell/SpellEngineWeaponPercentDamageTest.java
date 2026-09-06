package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-042 — SPELL_EFFECT_WEAPON_PERCENT_DAMAGE (31). Backstab 53 Rank 1 is 150%. */
class SpellEngineWeaponPercentDamageTest {
    @Test
    void applyWeaponPercentDamageWhenLivingShouldDealPercentOfWeapon() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_WEAPON_PERCENT_DAMAGE));
        Player caster = new Player();
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo backstab = new SpellEngine.SpellInfo(
                53, SpellEngine.EFFECT_WEAPON_PERCENT_DAMAGE, 0, 0, 0, 150, 150, 5f);
        int dmg = eng.apply(caster, target, backstab);
        assertEquals(3, dmg);
        assertEquals(77, target.health());
    }

    @Test
    void applyWeaponPercentDamageWhenDeadShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo backstab = new SpellEngine.SpellInfo(
                53, SpellEngine.EFFECT_WEAPON_PERCENT_DAMAGE, 0, 0, 0, 150, 150, 5f);
        assertEquals(0, eng.apply(new Player(), dead, backstab));
        assertEquals(0, dead.health());
    }
}
