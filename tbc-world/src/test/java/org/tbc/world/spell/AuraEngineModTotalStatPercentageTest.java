package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-195 — SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE (137).
 * CMaNGOS HandleModTotalPercentStat → TOTAL_PCT on stats + ApplyStatPercentBuffMod (POS/NEG).
 */
class AuraEngineModTotalStatPercentageTest {
    private static final SpellEngine.SpellInfo TOTAL_STR = new SpellEngine.SpellInfo(
            999137, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE, 0, 0, 10, 10, 0f, 0);

    @Test
    void applyAuraWhenTotalStatPctOnPlayerShouldRaiseStatAndBuffColumns() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        player.setInt(UpdateFields.UNIT_FIELD_POSSTAT0, 20);
        player.setInt(UpdateFields.UNIT_FIELD_NEGSTAT0, -10);

        eng.apply(player, player, TOTAL_STR);

        assertTrue(player.hasAura(999137));
        assertEquals(110, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(22, player.getInt(UpdateFields.UNIT_FIELD_POSSTAT0));
        assertEquals(-11, player.getInt(UpdateFields.UNIT_FIELD_NEGSTAT0));
    }

    @Test
    void unapplyWhenTotalStatPctShouldRestoreStatAndBuffColumns() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        player.setInt(UpdateFields.UNIT_FIELD_POSSTAT0, 20);
        eng.apply(player, player, TOTAL_STR);
        eng.auras().unapply(player, TOTAL_STR);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(20, player.getInt(UpdateFields.UNIT_FIELD_POSSTAT0));
    }

    @Test
    void applyWhenMiscAllStatsShouldTouchEveryStat() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        for (int i = 0; i < 5; i++) {
            player.setInt(UpdateFields.UNIT_FIELD_STAT0 + i, 100);
        }
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999138, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE,
                0, 0, 10, 10, 0f, -1));
        for (int i = 0; i < 5; i++) {
            assertEquals(110, player.getInt(UpdateFields.UNIT_FIELD_STAT0 + i));
        }
    }

    @Test
    void applyWhenCreatureShouldScaleStatsWithoutBuffRequirement() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(new Player(), mob, TOTAL_STR);
        assertEquals(110, mob.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenMiscOutOfRangeOrAmountZeroShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999139, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE,
                0, 0, 10, 10, 0f, 5));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999140, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE,
                0, 0, 0, 0, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, TOTAL_STR);
        new AuraEngine().unapply(null, TOTAL_STR);
    }
}
