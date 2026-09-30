package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-162 — SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL (71). Holy Power 5923:
 * EffectBasePoints 0 + 1 = +1% holy crit; EffectMiscValue school mask bit 1 (HOLY).
 * CMaNGOS HandleModSpellCritChanceShool → UpdateSpellCritChance(school).
 */
class AuraEngineModSpellCritChanceSchoolTest {
    /** Holymask bit 1 → SPELL_SCHOOL_HOLY. */
    private static final SpellEngine.SpellInfo HOLY_POWER = new SpellEngine.SpellInfo(
            SpellEngine.HOLY_POWER, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL, 0, 0, 1, 1, 0f, 2);

    @Test
    void applyAuraWhenModSpellCritChanceSchoolOnPlayerShouldRaiseMatchingSchoolOnly() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL));
        Player player = new Player();
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i, 5f);
        }

        eng.apply(player, player, HOLY_POWER);

        assertTrue(player.hasAura(SpellEngine.HOLY_POWER));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1));
        assertEquals(6f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 2));
    }

    @Test
    void unapplyWhenModSpellCritChanceSchoolShouldRestoreMatchingSchool() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1, 5f);
        eng.apply(player, player, HOLY_POWER);
        eng.unapplyAura(player, SpellEngine.HOLY_POWER);
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveSpellCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999071, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL,
                0, 0, 0, 0, 0f, 2));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1));
    }

    @Test
    void applyWhenCreatureShouldRaiseMatchingSchoolCritChance() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, HOLY_POWER);
        assertTrue(mob.hasAura(SpellEngine.HOLY_POWER));
        assertEquals(0f, mob.spellCritChance(0));
        assertEquals(1f, mob.spellCritChance(1));
    }

    @Test
    void applyWhenMaskZeroShouldLeaveCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999072, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL,
                0, 0, 1, 1, 0f, 0));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + 1));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HOLY_POWER);
        new AuraEngine().unapply(null, HOLY_POWER);
    }
}
