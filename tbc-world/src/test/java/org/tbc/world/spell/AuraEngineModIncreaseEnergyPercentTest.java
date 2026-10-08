package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-183 — SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT (132).
 * CMaNGOS HandleAuraModIncreaseEnergyPercent → TOTAL_PCT on UNIT_MOD_POWER_START + misc power.
 */
class AuraEngineModIncreaseEnergyPercentTest {
    /** Synthetic +20% max energy (misc POWER_ENERGY). */
    private static final SpellEngine.SpellInfo ENERGY_PCT = new SpellEngine.SpellInfo(
            999132, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT, 0, 0, 20, 20, 0f, Player.POWER_ENERGY);

    @Test
    void applyAuraWhenModIncreaseEnergyPercentShouldRaiseMaxEnergy() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT));
        Player rogue = new Player();
        rogue.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        rogue.setInt(UpdateFields.UNIT_FIELD_POWER4, 50);

        eng.apply(rogue, rogue, ENERGY_PCT);

        assertTrue(rogue.hasAura(999132));
        assertEquals(120, rogue.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(50, rogue.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void unapplyWhenModIncreaseEnergyPercentShouldRestoreMaxAndClampCurrent() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        rogue.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        rogue.setInt(UpdateFields.UNIT_FIELD_POWER4, 100);
        eng.apply(rogue, rogue, ENERGY_PCT);
        rogue.setInt(UpdateFields.UNIT_FIELD_POWER4, 120);
        eng.auras().unapply(rogue, ENERGY_PCT);
        assertEquals(100, rogue.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(100, rogue.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void applyWhenMiscOutOfRangeShouldLeaveMaxPower() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        eng.apply(p, p, new SpellEngine.SpellInfo(
                999131, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT,
                0, 0, 20, 20, 0f, 99));
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveMaxPower() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        eng.apply(p, p, new SpellEngine.SpellInfo(
                999130, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT,
                0, 0, 0, 0, 0f, Player.POWER_ENERGY));
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, ENERGY_PCT);
        new AuraEngine().unapply(null, ENERGY_PCT);
    }
}
