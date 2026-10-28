package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-259 — power burn (162). */
class AuraEnginePowerBurnTest {
    @Test
    void tickPowerBurnShouldDrainManaAndDealThatMuchDamage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_POWER_BURN_MANA));
        Player caster = alive();
        Player target = alive();
        target.setInt(UpdateFields.UNIT_FIELD_POWER1, 40);
        assertTrue(eng.auras().tickCatalogPeriodic(caster, target, burn(15, Player.POWER_MANA), null));
        assertEquals(25, target.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(85, target.health());
    }

    @Test
    void tickPowerBurnShouldSkipWhenImmuneOrThePowerTypeDoesNotMatch() {
        SpellEngine eng = new SpellEngine();
        Player caster = alive();
        Player immune = alive();
        immune.setInt(UpdateFields.UNIT_FIELD_POWER1, 40);
        immune.setDamageImmunity(1, 1, true);
        eng.auras().tickCatalogPeriodic(caster, immune, burn(15, Player.POWER_MANA), null);
        assertEquals(40, immune.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(100, immune.health());

        Player mismatched = alive();
        mismatched.powerType = Player.POWER_RAGE;
        mismatched.setInt(UpdateFields.UNIT_FIELD_POWER1, 40);
        eng.auras().tickCatalogPeriodic(caster, mismatched, burn(15, Player.POWER_MANA), null);
        assertEquals(40, mismatched.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(100, mismatched.health());
    }

    private static SpellEngine.SpellInfo burn(int amount, int power) {
        return new SpellEngine.SpellInfo(999162, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_POWER_BURN_MANA, 0, 0, amount, amount, 0f, power);
    }

    private static Player alive() {
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        player.setHealth(100);
        player.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        return player;
    }
}
