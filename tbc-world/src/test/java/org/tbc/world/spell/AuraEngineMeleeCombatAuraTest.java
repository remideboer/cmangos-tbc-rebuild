package org.tbc.world.spell;

import org.tbc.world.combat.MeleeTable;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-271 through TP-SL26-280 — attacker hit/crit, ignored and added combat
 * results, crit-damage bonus, and melee damage taken.
 */
class AuraEngineMeleeCombatAuraTest {
    private static final int HOLY = 2;

    @Test
    void catalogShouldKnowTheTenCombatAuras() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_HIT_CHANCE));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACKER_RANGED_HIT_CHANCE));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_CRIT_CHANCE));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_ATTACKER_RANGED_CRIT_CHANCE));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_IGNORE_COMBAT_RESULT));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_COMBAT_RESULT_CHANCE));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_CRIT_DAMAGE_BONUS));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_TAKEN));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_MELEE_DAMAGE_TAKEN));
    }

    @Test
    void meleeHitOnVictimShouldSumAndClearTheMissChance() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        MeleeTable low = roll(0.0);
        assertEquals(MeleeTable.Outcome.MISS, low.rollOne(attacker, victim, 10, 10).outcome());
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, false), 1e-6);

        SpellEngine.SpellInfo first = aura(999271, AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_HIT_CHANCE, 3, 0);
        SpellEngine.SpellInfo second = aura(999272, AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_HIT_CHANCE, 2, 0);
        eng.auras().apply(victim, first);
        assertEquals(0.02, MeleeTable.missChance(attacker, victim, false, false), 1e-6);
        eng.auras().apply(victim, second);
        assertEquals(5, victim.attackerMeleeHitChance());
        assertEquals(0.0, MeleeTable.missChance(attacker, victim, false, false), 1e-6);
        assertEquals(MeleeTable.Outcome.HIT, low.rollOne(attacker, victim, 10, 10).outcome());
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, true), 1e-6);

        eng.auras().unapply(victim, first);
        eng.auras().unapply(victim, second);
        assertEquals(0, victim.attackerMeleeHitChance());
        assertEquals(MeleeTable.Outcome.MISS, low.rollOne(attacker, victim, 10, 10).outcome());
    }

    @Test
    void rangedHitOnVictimShouldLeaveMeleeMissAlone() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, true), 1e-6);

        SpellEngine.SpellInfo ranged = aura(999273, AuraEngine.SPELL_AURA_MOD_ATTACKER_RANGED_HIT_CHANCE, 5, 0);
        eng.auras().apply(victim, ranged);
        assertEquals(0.0, MeleeTable.missChance(attacker, victim, false, true), 1e-6);
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, false), 1e-6);

        SpellEngine.SpellInfo melee = aura(999274, AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_HIT_CHANCE, 5, 0);
        eng.auras().apply(victim, melee);
        assertEquals(0.0, MeleeTable.missChance(attacker, victim, false, true), 1e-6);
        assertEquals(0.0, MeleeTable.missChance(attacker, victim, false, false), 1e-6);

        eng.auras().unapply(victim, ranged);
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, true), 1e-6);
        assertEquals(0, victim.attackerRangedHitChance());
    }

    @Test
    void critTakenShouldAddMeleeAndRangedSeparately() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        assertEquals(0.0, MeleeTable.critChance(attacker, victim, false, false), 1e-6);
        assertEquals(0.0, MeleeTable.critChance(attacker, victim, false, true), 1e-6);

        SpellEngine.SpellInfo melee = aura(999275, AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_CRIT_CHANCE, 10, 0);
        eng.auras().apply(victim, melee);
        assertEquals(0.10, MeleeTable.critChance(attacker, victim, false, false), 1e-6);
        assertEquals(0.0, MeleeTable.critChance(attacker, victim, false, true), 1e-6);

        SpellEngine.SpellInfo ranged = aura(999276, AuraEngine.SPELL_AURA_MOD_ATTACKER_RANGED_CRIT_CHANCE, 10, 0);
        eng.auras().apply(victim, ranged);
        assertEquals(0.10, MeleeTable.critChance(attacker, victim, false, true), 1e-6);
        assertEquals(0.10, MeleeTable.critChance(attacker, victim, false, false), 1e-6);
        assertEquals(10, victim.attackerMeleeCritChance());
        assertEquals(10, victim.attackerRangedCritChance());

        eng.auras().unapply(victim, melee);
        eng.auras().unapply(victim, ranged);
        assertEquals(0.0, MeleeTable.critChance(attacker, victim, false, false), 1e-6);
        assertEquals(0.0, MeleeTable.critChance(attacker, victim, false, true), 1e-6);
    }

    @Test
    void ignoreCombatResultShouldSkipDodgeParryAndBlock() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        MeleeTable mid = roll(0.10);

        victim.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 20f);
        assertEquals(MeleeTable.Outcome.DODGE, mid.rollOne(attacker, victim, 10, 10).outcome());
        SpellEngine.SpellInfo dodge = aura(999277, AuraEngine.SPELL_AURA_IGNORE_COMBAT_RESULT, 0,
                Unit.MELEE_HIT_DODGE);
        eng.auras().apply(attacker, dodge);
        assertTrue(attacker.ignoresCombatResult(Unit.MELEE_HIT_DODGE));
        assertEquals(MeleeTable.Outcome.HIT, mid.rollOne(attacker, victim, 10, 10).outcome());
        eng.auras().unapply(attacker, dodge);
        assertFalse(attacker.ignoresCombatResult(Unit.MELEE_HIT_DODGE));
        assertEquals(MeleeTable.Outcome.DODGE, mid.rollOne(attacker, victim, 10, 10).outcome());

        victim.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 0f);
        victim.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 20f);
        assertEquals(MeleeTable.Outcome.PARRY, mid.rollOne(attacker, victim, 10, 10).outcome());
        SpellEngine.SpellInfo parry = aura(999278, AuraEngine.SPELL_AURA_IGNORE_COMBAT_RESULT, 0,
                Unit.MELEE_HIT_PARRY);
        eng.auras().apply(attacker, parry);
        assertEquals(MeleeTable.Outcome.HIT, mid.rollOne(attacker, victim, 10, 10).outcome());
        eng.auras().unapply(attacker, parry);

        victim.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 0f);
        victim.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE, 20f);
        assertEquals(MeleeTable.Outcome.BLOCK, mid.rollOne(attacker, victim, 10, 10).outcome());
        SpellEngine.SpellInfo block = aura(999279, AuraEngine.SPELL_AURA_IGNORE_COMBAT_RESULT, 0,
                Unit.MELEE_HIT_BLOCK);
        eng.auras().apply(attacker, block);
        assertEquals(MeleeTable.Outcome.HIT, mid.rollOne(attacker, victim, 10, 10).outcome());

        victim.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 20f);
        victim.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 20f);
        eng.auras().apply(attacker, dodge);
        eng.auras().apply(attacker, parry);
        assertEquals(MeleeTable.Outcome.HIT, mid.rollOne(attacker, victim, 10, 10).outcome());
    }

    @Test
    void combatResultChanceShouldAddDodgeParryAndBlockThenCap() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        MeleeTable mid = roll(0.10);

        SpellEngine.SpellInfo dodge = aura(999280, AuraEngine.SPELL_AURA_MOD_COMBAT_RESULT_CHANCE, 20,
                Unit.VICTIMSTATE_DODGE);
        eng.auras().apply(attacker, dodge);
        assertEquals(20, attacker.combatResultChance(Unit.VICTIMSTATE_DODGE));
        assertEquals(0, attacker.combatResultChance(Unit.VICTIMSTATE_PARRY));
        assertEquals(MeleeTable.Outcome.DODGE, mid.rollOne(attacker, victim, 10, 10).outcome());
        assertEquals(MeleeTable.Outcome.HIT, roll(0.30).rollOne(attacker, victim, 10, 10).outcome());
        eng.auras().unapply(attacker, dodge);
        assertEquals(MeleeTable.Outcome.HIT, mid.rollOne(attacker, victim, 10, 10).outcome());

        SpellEngine.SpellInfo parry = aura(999281, AuraEngine.SPELL_AURA_MOD_COMBAT_RESULT_CHANCE, 20,
                Unit.VICTIMSTATE_PARRY);
        eng.auras().apply(attacker, parry);
        assertEquals(MeleeTable.Outcome.PARRY, mid.rollOne(attacker, victim, 10, 10).outcome());
        assertEquals(MeleeTable.Outcome.HIT, roll(0.30).rollOne(attacker, victim, 10, 10).outcome());
        eng.auras().unapply(attacker, parry);

        SpellEngine.SpellInfo block = aura(999282, AuraEngine.SPELL_AURA_MOD_COMBAT_RESULT_CHANCE, 20,
                Unit.VICTIMSTATE_BLOCKS);
        eng.auras().apply(attacker, block);
        assertEquals(MeleeTable.Outcome.BLOCK, mid.rollOne(attacker, victim, 10, 10).outcome());
        eng.auras().unapply(attacker, block);

        SpellEngine.SpellInfo huge = aura(999283, AuraEngine.SPELL_AURA_MOD_COMBAT_RESULT_CHANCE, 10000,
                Unit.VICTIMSTATE_DODGE);
        eng.auras().apply(attacker, huge);
        assertEquals(MeleeTable.Outcome.DODGE, roll(0.99).rollOne(attacker, victim, 10, 10).outcome());
    }

    @Test
    void physicalCritDamageBonusShouldScaleASittingCrit() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        victim.sit();
        assertEquals(20, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        assertEquals(MeleeTable.Outcome.CRIT, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).outcome());

        SpellEngine.SpellInfo holy = aura(999284, AuraEngine.SPELL_AURA_MOD_CRIT_DAMAGE_BONUS, 100, HOLY);
        eng.auras().apply(attacker, holy);
        assertEquals(20, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        SpellEngine.SpellInfo physical = aura(999285, AuraEngine.SPELL_AURA_MOD_CRIT_DAMAGE_BONUS, 100,
                Unit.SPELL_SCHOOL_MASK_NORMAL);
        eng.auras().apply(attacker, physical);
        assertEquals(100, attacker.critDamageBonus(Unit.SPELL_SCHOOL_MASK_NORMAL));
        assertEquals(100, attacker.critDamageBonus(HOLY));
        assertEquals(40, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        eng.auras().unapply(attacker, holy);
        assertEquals(0, attacker.critDamageBonus(HOLY));
        assertEquals(40, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        eng.auras().unapply(attacker, physical);
        assertEquals(20, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
    }

    @Test
    void damageTakenShouldAddFlatThenPercentAndMeleeFlat() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        assertEquals(10, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        SpellEngine.SpellInfo holy = aura(999286, AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN, 5, HOLY);
        eng.auras().apply(victim, holy);
        assertEquals(10, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        SpellEngine.SpellInfo flat = aura(999287, AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN, 5,
                Unit.SPELL_SCHOOL_MASK_NORMAL);
        eng.auras().apply(victim, flat);
        assertEquals(5, victim.damageTakenFlat(Unit.SPELL_SCHOOL_MASK_NORMAL));
        assertEquals(15, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        SpellEngine.SpellInfo pct = aura(999288, AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_TAKEN, 100,
                Unit.SPELL_SCHOOL_MASK_NORMAL);
        SpellEngine.SpellInfo pctAgain = aura(999289, AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_TAKEN, 100,
                Unit.SPELL_SCHOOL_MASK_NORMAL);
        eng.auras().apply(victim, pct);
        assertEquals(30, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        eng.auras().apply(victim, pctAgain);
        assertEquals(4f, victim.damageTakenMultiplier(Unit.SPELL_SCHOOL_MASK_NORMAL), 1e-6);
        assertEquals(60, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        eng.auras().unapply(victim, pctAgain);
        assertEquals(30, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        eng.auras().unapply(victim, flat);
        eng.auras().unapply(victim, pct);
        SpellEngine.SpellInfo melee = aura(999290, AuraEngine.SPELL_AURA_MOD_MELEE_DAMAGE_TAKEN, 3, 0);
        eng.auras().apply(victim, melee);
        assertEquals(3, victim.meleeDamageTakenFlat());
        assertEquals(13, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        eng.auras().unapply(victim, melee);
        assertEquals(10, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());

        SpellEngine.SpellInfo negative = aura(999291, AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN, -100,
                Unit.SPELL_SCHOOL_MASK_NORMAL);
        eng.auras().apply(victim, negative);
        assertEquals(0, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
        eng.auras().unapply(victim, negative);
        assertEquals(10, MeleeTable.alwaysHit().rollOne(attacker, victim, 10, 10).damage());
    }

    @Test
    void aMissShouldStayZeroWhenFlatDamageTakenIsPositive() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        eng.auras().apply(victim, aura(999292, AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN, 50,
                Unit.SPELL_SCHOOL_MASK_NORMAL));
        MeleeTable.Result miss = roll(0.0).rollOne(attacker, victim, 10, 10);
        assertEquals(MeleeTable.Outcome.MISS, miss.outcome());
        assertEquals(0, miss.damage());
    }

    @Test
    void zeroAmountShouldStoreOnlyIgnoreCombatResult() {
        SpellEngine eng = new SpellEngine();
        Player attacker = new Player();
        Player victim = new Player();
        eng.auras().apply(victim, aura(999293, AuraEngine.SPELL_AURA_MOD_ATTACKER_MELEE_HIT_CHANCE, 0, 0));
        eng.auras().apply(victim, aura(999294, AuraEngine.SPELL_AURA_MOD_DAMAGE_TAKEN, 0,
                Unit.SPELL_SCHOOL_MASK_NORMAL));
        eng.auras().apply(attacker, aura(999295, AuraEngine.SPELL_AURA_MOD_CRIT_DAMAGE_BONUS, 0,
                Unit.SPELL_SCHOOL_MASK_NORMAL));
        assertEquals(0, victim.attackerMeleeHitChance());
        assertEquals(0, victim.damageTakenFlat(Unit.SPELL_SCHOOL_MASK_NORMAL));
        assertEquals(0, attacker.critDamageBonus(Unit.SPELL_SCHOOL_MASK_NORMAL));
        assertEquals(0.05, MeleeTable.missChance(attacker, victim, false, false), 1e-6);

        victim.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE, 20f);
        eng.auras().apply(attacker, aura(999296, AuraEngine.SPELL_AURA_IGNORE_COMBAT_RESULT, 0,
                Unit.MELEE_HIT_DODGE));
        assertEquals(MeleeTable.Outcome.HIT, roll(0.10).rollOne(attacker, victim, 10, 10).outcome());
    }

    private static MeleeTable roll(double r) {
        return new MeleeTable(() -> r, (min, max) -> min);
    }

    private static SpellEngine.SpellInfo aura(int id, int type, int amount, int misc) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA, type, 0, 0, amount, amount, 0f, misc);
    }
}
