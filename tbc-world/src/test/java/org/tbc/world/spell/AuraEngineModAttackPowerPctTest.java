package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-178 — SPELL_AURA_MOD_ATTACK_POWER_PCT (166).
 * CMaNGOS HandleAuraModAttackPowerPercent → TOTAL_PCT;
 * client field UNIT_FIELD_ATTACK_POWER_MULTIPLIER = (TOTAL_PCT − 1).
 */
class AuraEngineModAttackPowerPctTest {
    /** Synthetic +20% melee AP — Trueshot Aura / Unleashed Rage shape. */
    private static final SpellEngine.SpellInfo AP_PCT = new SpellEngine.SpellInfo(
            999166, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_ATTACK_POWER_PCT, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenModAttackPowerPctShouldRaiseAttackPowerMultiplier() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACK_POWER_PCT));
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER, 0f);

        eng.apply(player, player, AP_PCT);

        assertTrue(player.hasAura(999166));
        assertEquals(0.20f, player.getFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void unapplyWhenModAttackPowerPctShouldRestoreMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER, 0f);
        eng.apply(player, player, AP_PCT);
        eng.auras().unapply(player, AP_PCT);
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER, 0f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999167, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_ATTACK_POWER_PCT,
                0, 0, 0, 0, 0f));
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER), 1e-5f);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, AP_PCT);
        new AuraEngine().unapply(null, AP_PCT);
    }
}
