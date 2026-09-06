package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-021 — SPELL_EFFECT_WEAPON_DAMAGE_NOSCHOOL (17). Physical line, no magic miss. */
class SpellEngineWeaponDamageNoschoolTest {
    @Test
    void applyWeaponDamageNoschoolWhenLivingShouldDealPhysicalDamage() {
        SpellEngine eng = SpellEngine.alwaysHit();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_WEAPON_DAMAGE_NOSCHOOL));
        Creature target = new Creature();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo ambush = new SpellEngine.SpellInfo(
                8676, SpellEngine.EFFECT_WEAPON_DAMAGE_NOSCHOOL, 0, 0, 0, 10, 10, 5f);
        int dmg = eng.apply(new Player(), target, ambush);
        assertEquals(10, dmg);
        assertEquals(70, target.health());
    }

    @Test
    void applyWeaponDamageNoschoolWhenDeadShouldNotHealOrRaise() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Creature dead = new Creature();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo ambush = new SpellEngine.SpellInfo(
                8676, SpellEngine.EFFECT_WEAPON_DAMAGE_NOSCHOOL, 0, 0, 0, 10, 10, 5f);
        eng.apply(new Player(), dead, ambush);
        assertEquals(0, dead.health());
    }
}
