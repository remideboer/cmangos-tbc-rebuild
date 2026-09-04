package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeleeTableTest {
    @Test
    void tpSl12MeleeTableOneRoll() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.MISS, table(0.01).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.PARRY, table(0.11).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.16).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        a.level = 11;
        v.level = 11;
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.22).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, MeleeTable.alwaysHit().rollOne(a, v, 4, 9).outcome());
        assertEquals(4, MeleeTable.alwaysHit().rollOne(a, v, 4, 9).damage());
        boolean wide = false;
        boolean flat = false;
        for (int i = 0; i < 64; i++) {
            if (MeleeTable.roll(a, v, 4, 9).damage() > 0) {
                wide = true;
            }
            if (MeleeTable.roll(a, v, 2, 2).damage() > 0) {
                flat = true;
            }
            if (wide && flat) {
                break;
            }
        }
        assertTrue(wide && flat);
    }

    @Test
    void rollOneWhenNpcDefenseExceedsWeaponSkillShouldRaiseMissChance() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 4;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 4);
        assertEquals(MeleeTable.Outcome.MISS, table(0.06).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.DODGE, table(0.10).rollOne(a, v, 2, 2).outcome());
        v.level = 2;
        assertEquals(MeleeTable.Outcome.MISS, table(0.054).rollOne(a, v, 2, 2).outcome());
        a.level = 20;
        v.level = 11;
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.level = 60;
        a.level = 1;
        assertEquals(MeleeTable.Outcome.MISS, table(0.99).rollOne(a, v, 2, 2).outcome());
        a.level = 60;
        v.level = 1;
        assertEquals(MeleeTable.Outcome.CRIT, table(0.01).rollOne(a, v, 2, 2).outcome());
        a.level = 1;
        v.level = 3;
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenPlayerHasOffhandWeaponShouldAddNineteenPercentMiss() {
        Player a = new Player();
        a.level = 1;
        Item off = new Item(1, 25);
        off.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        a.items.put(1, off);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.MISS, table(0.06).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.DODGE, table(0.26).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenNextMeleeSwingQueuedShouldSkipDualWieldMissPenalty() {
        Player a = new Player();
        a.level = 1;
        Item off = new Item(1, 25);
        off.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        a.items.put(1, off);
        a.queueNextMeleeSwing();
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenAttackerHasExpertiseShouldReduceDodgeAndParryNotBlock() {
        Player a = new Player();
        a.level = 1;
        a.setInt(UpdateFields.PLAYER_EXPERTISE, 20);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.06).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.HIT, table(0.11).rollOne(a, v, 2, 2).outcome());
        a.setInt(UpdateFields.PLAYER_EXPERTISE, 40);
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.06).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenOffhandShouldSubtractOffhandExpertiseFromDodgeAndParry() {
        Player a = new Player();
        a.level = 1;
        a.setInt(UpdateFields.PLAYER_EXPERTISE, 20);
        a.setInt(UpdateFields.PLAYER_OFFHAND_EXPERTISE, 0);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2, true).outcome());
        a.setInt(UpdateFields.PLAYER_OFFHAND_EXPERTISE, 20);
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.06).rollOne(a, v, 2, 2, true).outcome());
    }

    @Test
    void rollOneWhenNpcDefenseExceedsSkillShouldRaiseDodgeAtPointOnePerPoint() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 4;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 4);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.14).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.PARRY, table(0.22).rollOne(a, v, 2, 2).outcome());
        v.level = 2;
        assertEquals(MeleeTable.Outcome.DODGE, table(0.08).rollOne(a, v, 2, 2).outcome());
        a.level = 20;
        v.level = 11;
        assertEquals(MeleeTable.Outcome.PARRY, table(0.07).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenAttackerSkillBeatsNpcDefenseShouldNotAddBlockBonus() {
        Player a = new Player();
        a.level = 60;
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.01).rollOne(a, v, 2, 2).outcome());
        a.level = 1;
        v.level = 4;
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.32).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenPlayerDefenseExceedsWeaponSkillShouldRaiseMissAtFourHundredths() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 11;
        v.setHealth(100);
        assertEquals(MeleeTable.Outcome.MISS, table(0.06).rollOne(a, v, 2, 2).outcome());
        a.level = 4;
        v.level = 1;
        assertEquals(MeleeTable.Outcome.CRIT, table(0.048).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenPlayerHasFivePercentCritShouldCritAndDoubleDamage() {
        Player a = new Player();
        a.level = 1;
        a.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        MeleeTable.Result r = table(0.22).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRIT, r.outcome());
        assertEquals(4, r.damage());
        assertEquals(4, r.threat());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        a.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_CRIT_PERCENTAGE, 200f);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.22).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenOffhandShouldUseOffhandCritPercentage() {
        Player a = new Player();
        a.level = 1;
        a.setFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        a.setFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE, 0f);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.HIT, table(0.22).rollOne(a, v, 2, 2, true).outcome());
        a.setFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE, 5f);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.22).rollOne(a, v, 2, 2, true).outcome());
    }

    @Test
    void rollOneWhenPlayerVsNpcAboveLevel10ShouldGlanceAtTenPlusDefenseMinusSkill() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 11;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 11);
        assertEquals(MeleeTable.Outcome.MISS, table(0.22).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.PARRY, table(0.50).rollOne(a, v, 2, 2).outcome());
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.81).rollOne(a, v, 2, 2).outcome());
        v.level = 20;
        assertEquals(MeleeTable.Outcome.PARRY, table(0.99).rollOne(a, v, 2, 2).outcome());
        assertEquals(1.0, MeleeTable.glanceChance(a, v), 1e-9);
        a.level = 20;
        v.level = 11;
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        Creature npc = new Creature();
        npc.level = 1;
        Player defender = new Player();
        defender.level = 11;
        assertEquals(MeleeTable.Outcome.CRIT, table(0.08).rollOne(npc, defender, 2, 2).outcome());
        Player other = new Player();
        other.level = 11;
        assertEquals(MeleeTable.Outcome.HIT, table(0.22).rollOne(a, other, 2, 2).outcome());
    }

    @Test
    void rollOneWhenWandUserBelow30ShouldGlanceAtLevelPlusDefenseMinusSkill() {
        Creature v = new Creature();
        v.level = 29;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 29);
        Player mage = new Player();
        mage.level = 29;
        mage.clazz = Player.CLASS_MAGE;
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.35).rollOne(mage, v, 2, 2).outcome());
        Player priest = new Player();
        priest.level = 29;
        priest.clazz = Player.CLASS_PRIEST;
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.35).rollOne(priest, v, 2, 2).outcome());
        Player warlock = new Player();
        warlock.level = 29;
        warlock.clazz = Player.CLASS_WARLOCK;
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.35).rollOne(warlock, v, 2, 2).outcome());
        Player warrior = new Player();
        warrior.level = 29;
        warrior.clazz = 1;
        assertEquals(MeleeTable.Outcome.HIT, table(0.35).rollOne(warrior, v, 2, 2).outcome());
        mage.level = 30;
        Creature v30 = new Creature();
        v30.level = 30;
        v30.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 30);
        assertEquals(MeleeTable.Outcome.HIT, table(0.35).rollOne(mage, v30, 2, 2).outcome());
    }

    @Test
    void rollOneWhenGlanceShouldScaleDamageBetweenLowAndHighEnds() {
        Player a = new Player();
        a.level = 11;
        Creature v = new Creature();
        v.level = 11;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 11);
        MeleeTable.Result same = table(0.22).rollOne(a, v, 100, 100);
        assertEquals(MeleeTable.Outcome.GLANCE, same.outcome());
        assertEquals(91, same.damage());
        assertEquals(91, same.threat());
        a.level = 12;
        MeleeTable.Result skillAhead = table(0.22).rollOne(a, v, 100, 100);
        assertEquals(MeleeTable.Outcome.GLANCE, skillAhead.outcome());
        assertEquals(100, skillAhead.damage());
        a.level = 1;
        MeleeTable.Result yellow = table(0.85).rollOne(a, v, 100, 100);
        assertEquals(MeleeTable.Outcome.GLANCE, yellow.outcome());
        assertEquals(1, yellow.damage());
        Player mage = new Player();
        mage.level = 11;
        mage.clazz = Player.CLASS_MAGE;
        MeleeTable.Result caster = table(0.22).rollOne(mage, v, 100, 100);
        assertEquals(MeleeTable.Outcome.GLANCE, caster.outcome());
        assertEquals(60, caster.damage());
        assertEquals(60, caster.threat());
    }

    @Test
    void rollOneWhenPlayerVictimHasNoAvoidRatingShouldSkipDodgeParryBlock() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 1;
        v.setHealth(100);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 0f);
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_PARRY_PERCENTAGE, 5f);
        assertEquals(MeleeTable.Outcome.PARRY, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_PARRY_PERCENTAGE, 0f);
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_BLOCK_PERCENTAGE, 5f);
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 200f);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 0f);
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_BLOCK_PERCENTAGE, 200f);
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.06).rollOne(a, v, 2, 2).outcome());
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_BLOCK_PERCENTAGE, 0f);
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 0.004f);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.06).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenBlockShouldSubtractShieldBlockValue() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 1;
        v.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE, 5f);
        v.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 4);
        MeleeTable.Result partial = table(0.06).rollOne(a, v, 10, 10);
        assertEquals(MeleeTable.Outcome.BLOCK, partial.outcome());
        assertEquals(6, partial.damage());
        assertEquals(6, partial.threat());
        assertEquals(4, partial.blocked());
        v.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, 20);
        MeleeTable.Result full = table(0.06).rollOne(a, v, 10, 10);
        assertEquals(MeleeTable.Outcome.BLOCK, full.outcome());
        assertEquals(0, full.damage());
        assertEquals(0, full.threat());
        assertEquals(10, full.blocked());
    }

    @Test
    void rollOneWhenSittingPlayerShouldForceRemainingTableToCrit() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 1;
        v.setHealth(100);
        v.sit();
        assertEquals(MeleeTable.Outcome.MISS, table(0.01).rollOne(a, v, 2, 2).outcome());
        MeleeTable.Result r = table(0.06).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRIT, r.outcome());
        assertEquals(4, r.damage());
        v.stand();
        v.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_DODGE_PERCENTAGE, 5f);
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenCreatureShouldUseFivePercentBaseCrit() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 1;
        v.setHealth(100);
        MeleeTable.Result r = table(0.06).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRIT, r.outcome());
        assertEquals(4, r.damage());
    }

    @Test
    void rollOneWhenNpcSkillDeficitAtLeast15ShouldCrushAt150Percent() {
        Creature a = new Creature();
        a.level = 4;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 4);
        Player v = new Player();
        v.level = 1;
        MeleeTable.Result r = table(0.22).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRUSH, r.outcome());
        assertEquals(3, r.damage());
        assertEquals(3, r.threat());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        a.level = 1;
        assertEquals(MeleeTable.Outcome.HIT, table(0.30).rollOne(a, v, 2, 2).outcome());
        Creature other = new Creature();
        other.level = 4;
        a.level = 4;
        assertEquals(MeleeTable.Outcome.HIT, table(0.30).rollOne(a, other, 2, 2).outcome());
    }

    @Test
    void rollOneWhenVictimIsEvadingShouldReturnEvade() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        v.evading = true;
        MeleeTable.Result r = table(0.99).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.EVADE, r.outcome());
        assertEquals(0, r.damage());
        assertEquals(0, r.threat());
    }

    private static MeleeTable table(double r) {
        return new MeleeTable(() -> r, (min, max) -> min >= max ? min : min);
    }
}
