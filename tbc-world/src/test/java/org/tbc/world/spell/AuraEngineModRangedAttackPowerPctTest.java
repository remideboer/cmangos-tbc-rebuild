package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-179 — SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT (167).
 * CMaNGOS HandleAuraModRangedAttackPowerPercent (wand-users skip);
 * UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER = TOTAL_PCT − 1.
 */
class AuraEngineModRangedAttackPowerPctTest {
    private static final SpellEngine.SpellInfo RAP_PCT = new SpellEngine.SpellInfo(
            999168, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenModRangedAttackPowerPctShouldRaiseRangedMultiplier() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT));
        Player player = new Player();
        player.clazz = 3; // hunter — not a wand user
        player.setFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER, 0f);

        eng.apply(player, player, RAP_PCT);

        assertTrue(player.hasAura(999168));
        assertEquals(0.20f, player.getFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void unapplyWhenModRangedAttackPowerPctShouldRestoreMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.clazz = 3;
        player.setFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER, 0f);
        eng.apply(player, player, RAP_PCT);
        eng.auras().unapply(player, RAP_PCT);
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void applyWhenWandUserShouldLeaveRangedMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player mage = new Player();
        mage.clazz = Player.CLASS_MAGE;
        mage.setFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER, 0f);
        eng.apply(mage, mage, RAP_PCT);
        assertEquals(0f, mage.getFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.clazz = 3;
        player.setFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER, 0f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999169, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT,
                0, 0, 0, 0, 0f));
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, RAP_PCT);
        new AuraEngine().unapply(null, RAP_PCT);
    }
}
