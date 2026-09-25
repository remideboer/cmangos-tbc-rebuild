package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-130 — SPELL_AURA_MOD_INCREASE_ENERGY (35). Vigor 14983 (+10 energy, misc POWER_ENERGY 3).
 * CMaNGOS HandleAuraModIncreaseEnergy → HandleStatModifier(UNIT_MOD_POWER_START + power).
 */
class AuraEngineModIncreaseEnergyTest {
    private static final SpellEngine.SpellInfo VIGOR = new SpellEngine.SpellInfo(
            SpellEngine.VIGOR, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY,
            0, 0, 10, 10, 0f, Player.POWER_ENERGY);

    @Test
    void applyWhenVigorShouldRaiseMaxEnergy() {
        AuraEngine auras = new AuraEngine();
        assertTrue(auras.knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY));
        Player rogue = new Player();
        rogue.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        rogue.setInt(UpdateFields.UNIT_FIELD_POWER4, 50);
        auras.apply(rogue, VIGOR);
        assertEquals(110, rogue.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(50, rogue.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void unapplyWhenVigorShouldRestoreMaxEnergyAndClampCurrent() {
        AuraEngine auras = new AuraEngine();
        Player rogue = new Player();
        rogue.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        auras.apply(rogue, VIGOR);
        rogue.setInt(UpdateFields.UNIT_FIELD_POWER4, 110);
        auras.unapply(rogue, VIGOR);
        assertEquals(100, rogue.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(100, rogue.getInt(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void applyWhenMiscOutOfRangeOrZeroAmountShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        auras.apply(null, VIGOR);
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        SpellEngine.SpellInfo bad = new SpellEngine.SpellInfo(
                999013, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY,
                0, 0, 5, 5, 0f, 99);
        auras.apply(p, bad);
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                999014, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_ENERGY,
                0, 0, 0, 0, 0f, Player.POWER_ENERGY);
        auras.apply(p, zero);
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
    }

    @Test
    void unapplyAuraWhenCatalogedShouldRestoreMaxEnergy() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        rogue.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, 100);
        eng.apply(new Player(), rogue, VIGOR);
        eng.unapplyAura(rogue, SpellEngine.VIGOR);
        assertEquals(100, rogue.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
    }
}
