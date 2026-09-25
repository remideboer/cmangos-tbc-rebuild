package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-125 — SPELL_AURA_MOD_STAT (29). Arcane Intellect 1459 (+10 intellect, misc STAT_INTELLECT 3).
 * CMaNGOS HandleAuraModStat → HandleStatModifier + ApplyStatBuffMod (player POSSTAT).
 */
class AuraEngineModStatTest {
    /** Spell.dbc 1459 effect0: APPLY_AURA, aura 29, misc 3 (intellect), EffectBasePoints+1 = 10. */
    private static final SpellEngine.SpellInfo ARCANE_INTELLECT = new SpellEngine.SpellInfo(
            1459, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
            0, 0, 10, 10, 0f, 3);

    @Test
    void applyWhenArcaneIntellectShouldRaiseIntellectAndPosStat() {
        AuraEngine auras = new AuraEngine();
        assertTrue(auras.knownAura(AuraEngine.SPELL_AURA_MOD_STAT));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_STAT3, 20);
        p.setInt(UpdateFields.UNIT_FIELD_POSSTAT3, 0);
        auras.apply(p, ARCANE_INTELLECT);
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(10, p.getInt(UpdateFields.UNIT_FIELD_POSSTAT3));
    }

    @Test
    void unapplyWhenArcaneIntellectShouldRestoreIntellect() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_STAT3, 20);
        auras.apply(p, ARCANE_INTELLECT);
        auras.unapply(p, ARCANE_INTELLECT);
        assertEquals(20, p.getInt(UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_POSSTAT3));
    }

    @Test
    void applyWhenMiscOutOfRangeShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_STAT0, 15);
        SpellEngine.SpellInfo bad = new SpellEngine.SpellInfo(
                999001, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 5, 5, 0f, 99);
        auras.apply(p, bad);
        assertEquals(15, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenAllStatsMiscNegativeShouldRaiseEveryStat() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        for (int i = 0; i < AuraEngine.MAX_STATS; i++) {
            p.setInt(UpdateFields.UNIT_FIELD_STAT0 + i, 10);
        }
        SpellEngine.SpellInfo all = new SpellEngine.SpellInfo(
                999002, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 2, 2, 0f, -1);
        auras.apply(p, all);
        for (int i = 0; i < AuraEngine.MAX_STATS; i++) {
            assertEquals(12, p.getInt(UpdateFields.UNIT_FIELD_STAT0 + i));
        }
    }

    @Test
    void applyWhenTargetMissingOrZeroAmountShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        auras.apply(null, ARCANE_INTELLECT);
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_STAT3, 20);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                999003, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 0, 0, 0f, 3);
        auras.apply(p, zero);
        assertEquals(20, p.getInt(UpdateFields.UNIT_FIELD_STAT3));
    }

    @Test
    void unapplyWhenNegativeAmountShouldRestoreNegStatColumn() {
        AuraEngine auras = new AuraEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_STAT0, 20);
        p.setInt(UpdateFields.UNIT_FIELD_NEGSTAT0, 0);
        SpellEngine.SpellInfo curse = new SpellEngine.SpellInfo(
                999004, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, -5, -5, 0f, 0);
        auras.apply(p, curse);
        assertEquals(15, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(-5, p.getInt(UpdateFields.UNIT_FIELD_NEGSTAT0));
        auras.unapply(p, curse);
        assertEquals(20, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_NEGSTAT0));
    }
}
