package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Public helper no-ops that apply() short-circuits before reaching. */
class SpellEngineEffectGuardsTest {
    @Test
    void instakillAndTeleportWhenNullTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.instakill(null);
        Player p = livingPlayer();
        p.relocate(1f, 2f, 3f, 0f);
        eng.teleportUnits(null, 0, 9f, 9f, 9f, 0f);
        assertEquals(1f, p.x, 0.01f);
    }

    @Test
    void healthLeechWhenNullOrDeadCasterShouldNotHeal() {
        SpellEngine eng = new SpellEngine();
        Player caster = livingPlayer();
        Creature target = livingCreature();
        assertEquals(0, eng.healthLeech(null, target, 10));
        assertEquals(0, eng.healthLeech(caster, null, 10));
        caster.setHealth(0);
        assertEquals(10, eng.healthLeech(caster, target, 10));
        assertEquals(90, target.health());
        assertEquals(0, caster.health());
    }

    @Test
    void powerDrainWhenNullShouldReturnZero() {
        SpellEngine eng = new SpellEngine();
        Player caster = livingPlayer();
        Creature target = livingCreature();
        assertEquals(0, eng.powerDrain(null, target, 10));
        assertEquals(0, eng.powerDrain(caster, null, 10));
    }

    @Test
    void addComboPointsWhenNullTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player rogue = new Player();
        eng.addComboPoints(rogue, null, 2);
        assertEquals(0, rogue.comboPoints());
    }

    @Test
    void sanctuaryDispelAndDualWieldWhenNullShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.sanctuary(null);
        assertEquals(0, eng.dispel(null, 1));
        eng.dispelMechanic(null, 7, 1);
        eng.dualWield(null);
        eng.addExtraAttacks(null, 2);
        eng.leapForward(null, 8f);
        eng.pullTowardsDest(null, 1f, 0f, 0f, 150);
    }

    @Test
    void summonPlayerWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player target = new Player();
        SpellEngine.SpellInfo ritual = new SpellEngine.SpellInfo(
                7720, SpellEngine.EFFECT_SUMMON_PLAYER, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, target, ritual);
        assertEquals(0, target.summonerGuid);
        assertEquals(0, SpellEngine.encodeSummonRequest(null, 1).length);
    }

    @Test
    void attackMeWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.guid = 10;
        mob.victim = 99;
        SpellEngine.SpellInfo taunt = new SpellEngine.SpellInfo(355, SpellEngine.EFFECT_ATTACK_ME, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, mob, taunt);
        assertEquals(99, mob.victim);
    }

    @Test
    void resurrectWhenNullCasterOrNonPlayerShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = livingPlayer();
        dead.setHealth(0);
        SpellEngine.SpellInfo res = new SpellEngine.SpellInfo(2006, SpellEngine.EFFECT_RESURRECT, 0, 0, 0, 20, 20, 0f);
        eng.apply(null, dead, res);
        assertEquals(0, dead.resurrectGuid);
        Creature npc = livingCreature();
        npc.setHealth(0);
        eng.apply(new Player(), npc, res);
        SpellEngine.SpellInfo resNew = new SpellEngine.SpellInfo(
                2006, SpellEngine.EFFECT_RESURRECT_NEW, 0, 0, 0, 69, 69, 0f, 0);
        eng.apply(null, dead, resNew);
        assertEquals(0, dead.resurrectGuid);
        eng.apply(new Player(), npc, resNew);
    }

    @Test
    void spiritHealWhenNonPlayerOrNonSpiritHealSpellShouldSkipAuraGate() {
        SpellEngine eng = new SpellEngine();
        Creature npc = livingCreature();
        npc.setHealth(0);
        SpellEngine.SpellInfo spirit = new SpellEngine.SpellInfo(
                22012, SpellEngine.EFFECT_SPIRIT_HEAL, 0, 0, 0, 99, 99, 0f);
        eng.apply(new Player(), npc, spirit);
        Player dead = livingPlayer();
        dead.setHealth(0);
        SpellEngine.SpellInfo other = new SpellEngine.SpellInfo(
                2050, SpellEngine.EFFECT_SPIRIT_HEAL, 0, 0, 0, 99, 99, 0f);
        eng.apply(new Player(), dead, other);
        assertEquals(100, dead.health());
    }

    @Test
    void powerBurnWhenNullOrCreatureShouldMatchTypeZero() {
        SpellEngine eng = new SpellEngine();
        Player live = livingPlayer();
        live.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        live.setPower(40);
        assertEquals(0, eng.powerBurn(null, 10, 0));
        Creature mob = livingCreature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        mob.setPower(40);
        SpellEngine.SpellInfo burn = new SpellEngine.SpellInfo(
                8129, SpellEngine.EFFECT_POWER_BURN, 0, 6, 0, 10, 10, 30f, 0);
        int dmg = eng.apply(new Player(), mob, burn);
        assertEquals(10, dmg);
        assertEquals(30, mob.power());
        assertEquals(90, mob.health());
        assertEquals(0, eng.powerBurn(live, 0, 0));
        assertEquals(40, live.power());
    }

    @Test
    void addThreatWhenNullCasterOrZeroAmountShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature mob = livingCreature();
        SpellEngine.SpellInfo threat = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_THREAT, 0, 0, 0, 50, 50, 0f);
        eng.apply(null, mob, threat);
        assertEquals(0f, mob.threatManager.threatOf(new Player()), 0.01f);
        Player caster = livingPlayer();
        caster.guid = 2;
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_THREAT, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, mob, zero);
        assertEquals(0f, mob.threatManager.threatOf(caster), 0.01f);
    }

    @Test
    void healPctEnergizePctAndMechanicalWhenNullShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.healPct(null, 50);
        eng.energizePct(null, 50, 0);
        eng.healMechanical(null, 10);
        Creature mob = livingCreature();
        mob.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        mob.setPower(10);
        SpellEngine.SpellInfo pct = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENERGIZE_PCT, 0, 0, 0, 50, 50, 0f, 0);
        eng.apply(new Player(), mob, pct);
        assertEquals(60, mob.power());
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_ENERGIZE_PCT, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Player(), mob, zero);
        assertEquals(60, mob.power());
    }

    @Test
    void weaponPercentChargePullLeapAndKnockWhenNullShouldNoOp() {
        SpellEngine eng = SpellEngine.alwaysHit();
        Creature target = livingCreature();
        Player caster = livingPlayer();
        assertEquals(0, eng.weaponPercentDamage(null, target, 150));
        assertEquals(0, eng.weaponPercentDamage(caster, null, 150));
        assertEquals(0, eng.weaponPercentDamage(caster, target, 0));
        eng.distract(null, 0f, 0f);
        eng.charge(caster, null);
        eng.pullTowards(caster, null, 300);
        SpellEngine.SpellInfo jump = new SpellEngine.SpellInfo(
                40622, SpellEngine.EFFECT_LEAP_BACK, 0, 0, 0, -350, -350, 0f, 100);
        eng.apply(null, caster, jump);
        eng.leapBack(caster, null, 10f, -35f);
        SpellEngine.SpellInfo kb = new SpellEngine.SpellInfo(
                10689, SpellEngine.EFFECT_KNOCK_BACK, 0, 0, 0, 100, 100, 0f, 100);
        eng.apply(null, target, kb);
        eng.knockBack(caster, null, 10f, 10f);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(null, 1).length);
    }

    @Test
    void stealBeneficialBuffWhenNullZeroMaxOrEmptyShouldNoOpOrDefaultOne() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        Player target = new Player();
        eng.stealBeneficialBuff(null, target, 1);
        eng.stealBeneficialBuff(caster, null, 1);
        SpellEngine.SpellInfo steal = new SpellEngine.SpellInfo(
                30449, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF, 0, 0, 0, 0, 0, 30f);
        target.auras.add(new Unit.Aura(1459, 30_000, 1));
        eng.apply(caster, target, steal);
        assertEquals(0, target.auras.size());
        assertEquals(1, caster.auras.size());
        Player empty = new Player();
        SpellEngine.SpellInfo two = new SpellEngine.SpellInfo(
                30449, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF, 0, 0, 0, 2, 2, 30f);
        eng.apply(caster, empty, two);
        Player stacked = new Player();
        stacked.auras.add(new Unit.Aura(1459, 30_000, 1));
        stacked.auras.add(new Unit.Aura(133, 30_000, 1));
        SpellEngine.SpellInfo one = new SpellEngine.SpellInfo(
                30449, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF, 0, 0, 0, 1, 1, 30f);
        Player thief = new Player();
        eng.apply(thief, stacked, one);
        assertEquals(1, stacked.auras.size());
        assertEquals(1, thief.auras.size());
    }

    @Test
    void modifyThreatReputationSelfResurrectAndFarsightGuards() {
        SpellEngine eng = new SpellEngine();
        Creature mob = livingCreature();
        SpellEngine.SpellInfo shatter = new SpellEngine.SpellInfo(
                32835, SpellEngine.EFFECT_MODIFY_THREAT_PERCENT, 0, 0, 0, -50, -50, 0f);
        eng.apply(null, mob, shatter);
        Player p = livingPlayer();
        SpellEngine.SpellInfo zeroRep = new SpellEngine.SpellInfo(
                21187, SpellEngine.EFFECT_REPUTATION, 0, 0, 0, 0, 0, 0f, 730);
        eng.apply(p, p, zeroRep);
        assertEquals(0, p.reputationStanding(730));
        Creature caster = livingCreature();
        SpellEngine.SpellInfo soulstone = new SpellEngine.SpellInfo(
                20707, SpellEngine.EFFECT_SELF_RESURRECT, 0, 0, 0, 20, 20, 0f);
        eng.apply(caster, p, soulstone);
        assertTrue(p.alive());
        SpellEngine.SpellInfo zeroPct = new SpellEngine.SpellInfo(
                20707, SpellEngine.EFFECT_SELF_RESURRECT, 0, 0, 0, 0, 0, 0f);
        Player dead = livingPlayer();
        dead.setHealth(0);
        eng.apply(dead, dead, zeroPct);
        assertEquals(0, dead.health());
        Creature focus = new Creature();
        focus.guid = 0;
        SpellEngine.SpellInfo eagle = new SpellEngine.SpellInfo(
                6197, SpellEngine.EFFECT_ADD_FARSIGHT, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, focus, eagle);
        assertEquals(0L, p.farSightGuid());
        eng.addFarsight(null, 8L);
        assertEquals(0L, p.farSightGuid());
    }

    private static Player livingPlayer() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(50);
        return p;
    }

    private static Creature livingCreature() {
        Creature c = new Creature();
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        c.setHealth(100);
        c.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        c.setPower(80);
        return c;
    }
}
