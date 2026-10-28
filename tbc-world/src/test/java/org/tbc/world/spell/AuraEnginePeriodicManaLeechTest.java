package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-258 — periodic mana leech (64). */
class AuraEnginePeriodicManaLeechTest {
    @Test
    void tickManaLeechShouldMovePowerWhenThePowerTypeMatches() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_MANA_LEECH));
        Player caster = mana(100, 10);
        Player target = mana(100, 40);
        assertTrue(eng.auras().tickCatalogPeriodic(caster, target, leech(15, Player.POWER_MANA), null));
        assertEquals(25, target.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(25, caster.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tickManaLeechShouldSkipWhenThePowerTypeDoesNotMatch() {
        SpellEngine eng = new SpellEngine();
        Player caster = mana(100, 10);
        Player target = mana(100, 40);
        target.powerType = Player.POWER_RAGE;
        eng.auras().tickCatalogPeriodic(caster, target, leech(15, Player.POWER_MANA), null);
        assertEquals(40, target.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(10, caster.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tickManaLeechShouldDrainWithoutGrantingWhenTheCasterHasNoPool() {
        SpellEngine eng = new SpellEngine();
        Player caster = mana(0, 0);
        Player target = mana(100, 40);
        eng.auras().tickCatalogPeriodic(caster, target, leech(15, Player.POWER_MANA), null);
        assertEquals(25, target.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(0, caster.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tickManaLeechShouldSkipWhenDeadOrImmune() {
        SpellEngine eng = new SpellEngine();
        Player caster = mana(100, 10);
        caster.setHealth(0);
        Player target = mana(100, 40);
        eng.auras().tickCatalogPeriodic(caster, target, leech(15, Player.POWER_MANA), null);
        assertEquals(40, target.getInt(UpdateFields.UNIT_FIELD_POWER1));

        Player living = mana(100, 10);
        Player immune = mana(100, 40);
        immune.setDamageImmunity(1, 1, true);
        eng.auras().tickCatalogPeriodic(living, immune, leech(15, Player.POWER_MANA), null);
        assertEquals(40, immune.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(10, living.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    private static SpellEngine.SpellInfo leech(int amount, int power) {
        return new SpellEngine.SpellInfo(999064, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PERIODIC_MANA_LEECH, 0, 0, amount, amount, 0f, power);
    }

    private static Player mana(int max, int current) {
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(100);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, max);
        player.setInt(UpdateFields.UNIT_FIELD_POWER1, current);
        return player;
    }
}
