package org.tbc.world.combat;

import org.tbc.common.WowBuffer;
import org.tbc.world.ai.EventAi;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.loot.LootSlot;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatTest {
    private final Combat combat = new Combat(MeleeTable.alwaysHit());
    private Player p;
    private Creature c;

    @BeforeEach
    void setUp() {
        p = new Player();
        p.guid = 1;
        p.level = 1;
        c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        c.spawnX = 0;
        c.spawnY = 0;
        c.relocate(0, 0, 0, 0);
    }

    private static void setFaction(Unit u, int templateId) {
        u.faction = templateId;
        u.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, templateId);
    }

    @Test
    void swingKillsAndTags() {
        combat.startAttack(p, c, 1000);
        combat.startAttack(p, c, 1001);
        combat.swing(p, c, 1002);
        assertEquals(p.guid, c.taggedBy);
        assertTrue(c.alive());
        c.setHealth(1);
        MeleeTable.Result r = combat.swing(p, c, 1003);
        assertEquals(MeleeTable.Outcome.HIT, r.outcome());
        assertFalse(c.alive());
        assertTrue(c.lootable);
        assertEquals(p.guid, c.taggedBy);
        assertFalse(p.inCombat);
        byte[] loot = combat.lootResponse(p, c);
        assertNotNull(loot);
        assertEquals(2L, guidAt(loot));
        assertEquals(Combat.LOOT_CORPSE, loot[8] & 0xFF);
    }

    @Test
    void swingMissDoesNotTag() {
        Combat miss = new Combat(new MeleeTable(() -> 0.01d, (a, b) -> 1));
        miss.swing(p, c, 5);
        assertEquals(42, c.health());
        assertEquals(0, c.taggedBy);
    }

    @Test
    void swingWhenHitShouldAddWhiteMeleeThreatPerAttackerUntilEvade() {
        combat.swing(p, c, 1002);
        assertEquals(1f, c.threatManager.threatOf(p));
        combat.evade(c);
        assertEquals(0f, c.threatManager.threatOf(p));
        assertEquals(0, c.threat);
    }

    @Test
    void swingWhenSecondPlayerHasMoreThreatShouldRetargetVictim() {
        combat.startAttack(p, c, 1000);
        combat.swing(p, c, 1002);
        Player other = new Player();
        other.guid = 3;
        other.level = 1;
        combat.swing(other, c, 1003);
        combat.swing(other, c, 1004);
        assertEquals(other.guid, c.victim);
    }

    @Test
    void swingSkipsDeadAndEvading() {
        c.setHealth(0);
        assertEquals(0, combat.swing(p, c, 1).damage());
        c.setHealth(42);
        c.evading = true;
        assertEquals(MeleeTable.Outcome.EVADE, combat.swing(p, c, 1).outcome());
        assertEquals(0, combat.swing(p, c, 1).damage());
        assertEquals(42, c.health());
    }

    @Test
    void swingWhenNextMeleeQueuedShouldConsumeAfterTheRollNotWhenSkipped() {
        p.queueNextMeleeSwing();
        c.setHealth(0);
        combat.swing(p, c, 1);
        assertTrue(p.hasNextMeleeSwingQueued());
        c.setHealth(42);
        combat.swing(p, c, 2);
        assertFalse(p.hasNextMeleeSwingQueued());
    }

    @Test
    void encodeAttackWhenSpellSwingShouldSetHitInfoNoAction() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.HIT, 3, 3), true);
        assertEquals(Combat.HITINFO_NORMALSWING2 | Combat.HITINFO_NOACTION, u32le(pkt));
    }

    @Test
    void encodeAttackWhenOffhandShouldSetHitInfoLeftSwing() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.HIT, 2, 2), false, true);
        assertEquals(Combat.HITINFO_LEFTSWING, u32le(pkt));
    }

    @Test
    void encodeAttackWhenOffhandMissShouldKeepHitInfoLeftSwing() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.MISS, 0, 0), false, true);
        assertEquals(Combat.HITINFO_LEFTSWING | Combat.HITINFO_MISS, u32le(pkt));
    }

    @Test
    void swingWhenNextMeleeBonusShouldAddToWeaponRange() {
        p.queueNextMeleeSwing(2);
        assertEquals(3, combat.swing(p, c, 1).damage());
        assertEquals(39, c.health());
    }

    @Test
    void swingWhenWeaponDamageFieldsSetShouldRollThatRange() {
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 10f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 10f);
        assertEquals(10, combat.swing(p, c, 1).damage());
        assertEquals(32, c.health());
    }

    @Test
    void swingOffhandWhenOffhandDamageFieldsSetShouldRollThatRangeWithoutConsumingNextMelee() {
        p.setFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 7f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, 7f);
        p.queueNextMeleeSwing(2);
        assertEquals(7, combat.swingOffhand(p, c, 1).damage());
        assertTrue(p.hasNextMeleeSwingQueued());
        assertEquals(35, c.health());
    }

    @Test
    void evadeWhenAwayFromSpawnShouldStartHomeMotion() {
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(
                new EventAi.Script(EventAi.EVENT_EVADE, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(133, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_REACHED_HOME, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        c.relocate(20, 0, 0, 0);
        combat.startAttack(p, c, 10);
        List<Integer> casts = new ArrayList<>();
        combat.evade(c, (cr, t, id) -> casts.add(id));
        assertTrue(c.evading);
        assertEquals(org.tbc.world.ai.MotionMaster.HOME, c.motion.type());
        assertEquals(20f, c.x, 0.01f);
        assertEquals(c.maxHealth(), c.health());
        assertFalse(c.inCombat);
        assertEquals(List.of(133), casts);
        combat.finishEvade(c, (cr, t, id) -> casts.add(id));
        assertFalse(c.evading);
        assertEquals(0f, c.x, 0.01f);
        assertEquals(List.of(133, 7164), casts);
    }

    @Test
    void evadeWhenAwayWithoutEventAiShouldStartHomeMotion() {
        c.relocate(20, 0, 0, 0);
        combat.evade(c);
        assertTrue(c.evading);
        assertEquals(org.tbc.world.ai.MotionMaster.HOME, c.motion.type());
        assertEquals(20f, c.x, 0.01f);
    }

    @Test
    void evadeWhenAwayAndNullCastShouldStillMoveHome() {
        c.eventAi = new EventAi();
        c.relocate(20, 0, 0, 0);
        combat.evade(c, null);
        assertTrue(c.evading);
        assertEquals(org.tbc.world.ai.MotionMaster.HOME, c.motion.type());
    }

    @Test
    void swingWhenKillsShouldScheduleRespawn() {
        c.respawnDelayMs = 10;
        c.setHealth(1);
        combat.swing(p, c, 1000);
        assertFalse(c.alive());
        assertEquals(1010, c.respawnAtMs);
    }

    @Test
    void respawnWhenCalledShouldRestoreAliveAtSpawn() {
        c.setHealth(0);
        c.lootable = true;
        c.taggedBy = 1;
        c.respawnAtMs = 5;
        c.relocate(9, 9, 0, 0);
        combat.respawn(c);
        combat.respawn(null);
        assertTrue(c.alive());
        assertEquals(c.maxHealth(), c.health());
        assertEquals(0f, c.x, 0.01f);
        assertFalse(c.lootable);
        assertEquals(0, c.respawnAtMs);
    }

    @Test
    void evadeWhenInCombatShouldResetHealthAndClearLoot() {
        combat.startAttack(p, c, 10);
        c.setHealth(10);
        c.lootable = true;
        c.taggedBy = p.guid;
        combat.evade(c);
        assertEquals(c.maxHealth(), c.health());
        assertEquals(0f, c.x);
        assertFalse(c.inCombat);
        assertFalse(c.lootable);
        assertNull(combat.lootResponse(p, c));
    }

    @Test
    void shouldEvadeLeashTimeoutAndNullVictim() {
        assertFalse(combat.shouldEvade(c, p, 0));
        c.inCombat = true;
        c.setHealth(0);
        assertFalse(combat.shouldEvade(c, p, 0));
        c.setHealth(42);
        assertTrue(combat.shouldEvade(c, null, 0));
        p.relocate(31, 0, 0, 0);
        c.lastHitMs = 0;
        assertTrue(combat.shouldEvade(c, p, 100));
        p.relocate(0, 0, 0, 0);
        c.lastHitMs = 0;
        assertTrue(combat.shouldEvade(c, p, Combat.PURSUIT_MS));
        c.lastHitMs = 50_000;
        assertFalse(combat.shouldEvade(c, p, 50_000));
    }

    @Test
    void takeItemWhenOwnerLootableSlotShouldMoveItemToBackpack() {
        c.lootable = true;
        c.taggedBy = p.guid;
        c.lootItems.add(new LootSlot(0, 25, 1, 42));
        Item it = combat.takeItem(p, c, 0, 100);
        assertNotNull(it);
        assertEquals(25, it.entry);
        assertEquals(100, it.guid);
        assertEquals(42, it.displayId);
        assertEquals(1, it.count);
        assertTrue(it.slot >= Player.INVENTORY_SLOT_ITEM_START);
        assertEquals(it, p.items.get(Guid.low(100)));
        assertTrue(c.lootItems.isEmpty());
    }

    @Test
    void takeMoneyWhenOwnerShouldAddCopperAndClearGold() {
        c.lootable = true;
        c.taggedBy = p.guid;
        c.lootGold = 12;
        p.setMoney(5);
        assertTrue(combat.takeMoney(p, c));
        assertEquals(17, p.money);
        assertEquals(0, c.lootGold);
    }

    @Test
    void takeMoneyWhenCannotLootOrZeroGoldShouldNotPay() {
        assertFalse(combat.takeMoney(p, null));
        c.lootable = true;
        c.lootGold = 0;
        p.setMoney(3);
        assertTrue(combat.takeMoney(p, c));
        assertEquals(3, p.money);
        c.lootGold = 9;
        c.taggedBy = 99;
        assertFalse(combat.takeMoney(p, c));
        assertEquals(9, c.lootGold);
        assertEquals(3, p.money);
    }

    @Test
    void takeItemWhenWrongTaggerMissingSlotOrFullBagShouldReturnNull() {
        assertNull(combat.takeItem(p, null, 0, 1));
        assertNull(combat.takeItem(null, c, 0, 1));
        c.lootable = false;
        assertNull(combat.takeItem(p, c, 0, 1));
        c.lootable = true;
        c.taggedBy = p.guid;
        assertNull(combat.takeItem(p, c, 0, 1));
        c.taggedBy = 99;
        c.lootItems.add(new LootSlot(0, 25, 1, 0));
        assertNull(combat.takeItem(p, c, 0, 1));
        c.taggedBy = p.guid;
        assertNull(combat.takeItem(p, c, 1, 1));
        assertNull(combat.takeItem(p, c, 0, 0));
        for (int s = Player.INVENTORY_SLOT_ITEM_START; s < Player.INVENTORY_SLOT_ITEM_END; s++) {
            Item filler = new Item(2000 + s, 35);
            filler.slot = s;
            p.items.put(2000 + s, filler);
        }
        assertNull(combat.takeItem(p, c, 0, 99));
        assertEquals(1, c.lootItems.size());
    }

    @Test
    void encodeLootRemovedShouldBeLootIndexByte() {
        byte[] pkt = combat.encodeLootRemoved(3);
        assertEquals(1, pkt.length);
        assertEquals(3, pkt[0] & 0xFF);
    }

    @Test
    void lootRejectsWrongTagger() {
        c.lootable = true;
        c.taggedBy = 0;
        assertNotNull(combat.lootResponse(p, c));
        c.taggedBy = 99;
        assertNull(combat.lootResponse(p, c));
        assertNull(combat.lootResponse(p, null));
        assertNull(combat.lootResponse(null, c));
        c.lootable = false;
        assertNull(combat.lootResponse(p, c));
    }

    @Test
    void encodesOutcomesAndStop() {
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.HIT, 2, 2));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.MISS, 0, 0));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.DODGE, 0, 0));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.PARRY, 0, 0));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.BLOCK, 0, 0));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.GLANCE, 1, 1));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.CRIT, 4, 4));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.CRUSH, 3, 3));
        combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.EVADE, 0, 0));
        byte[] stop = combat.encodeAttackStop(1, 2, true);
        assertTrue(stop.length > 2);
        byte[] live = combat.encodeAttackStop(1, 2, false);
        assertTrue(live.length > 2);
        combat.stopAttack(p);
        assertFalse(p.inCombat);
        byte[] rel = combat.encodeLootRelease(2);
        assertEquals(9, rel.length);
        assertNotNull(combat.encodeAttackStart(1, 2));
        assertNotNull(combat.encodeLoot(2, 0, 0));
        assertNotNull(combat.encodeLoot(2, 0, null));
    }

    @Test
    void swingWhenEventAiShouldFireDeathCastAtKiller() {
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_DEATH, 0, 100, 0, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_HOSTILE), EventAi.Action.none(), EventAi.Action.none())));
        c.setHealth(1);
        List<Integer> casts = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        combat.swing(p, c, 1, (cr, t, id) -> {
            casts.add(id);
            targets.add(t.guid);
        });
        assertFalse(c.alive());
        assertEquals(List.of(7164), casts);
        assertEquals(p.guid, targets.get(0));
    }

    @Test
    void swingWhenEventAiAndNullDeathCastShouldStillKill() {
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_DEATH, 0, 100, 0, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        c.setHealth(1);
        combat.swing(p, c, 1, null);
        assertFalse(c.alive());
    }

    @Test
    void evadeWhenEventAiShouldFireEvadeAndReachedHome() {
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(
                new EventAi.Script(EventAi.EVENT_EVADE, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(133, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_REACHED_HOME, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        combat.evade(c, (cr, t, id) -> casts.add(id));
        assertEquals(List.of(133, 7164), casts);
        assertFalse(c.inCombat);
    }

    @Test
    void evadeWhenEventAiAndNullCastShouldReset() {
        c.eventAi = new EventAi();
        combat.startAttack(p, c, 10);
        combat.evade(c, null);
        assertFalse(c.inCombat);
        assertEquals(c.maxHealth(), c.health());
    }

    @Test
    void swingWhenCreatureHitsPlayerShouldReduceHealth() {
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        c.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME, 0);
        combat.startAttack(p, c, 1000);
        assertEquals(2000, c.meleeCooldownMs);
        MeleeTable.Result r = combat.swing(c, p, 3000);
        assertEquals(MeleeTable.Outcome.HIT, r.outcome());
        assertEquals(99, p.health());
        assertEquals(1000, c.lastMeleeMs);
    }

    @Test
    void swingWhenCreatureShouldSkipDeadPlayerAndEvading() {
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(0);
        assertEquals(0, combat.swing(c, p, 1).damage());
        p.setHealth(100);
        c.evading = true;
        assertEquals(0, combat.swing(c, p, 1).damage());
        assertEquals(100, p.health());
    }

    @Test
    void swingWhenCreatureMissShouldNotChangePlayerHealth() {
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        Combat miss = new Combat(new MeleeTable(() -> 0.01d, (a, b) -> 1));
        miss.swing(c, p, 5);
        assertEquals(100, p.health());
    }

    @Test
    void swingWhenCreatureKillsPlayerShouldClearCombat() {
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 1);
        p.setHealth(1);
        combat.startAttack(p, c, 10);
        MeleeTable.Result r = combat.swing(c, p, 20);
        assertEquals(MeleeTable.Outcome.HIT, r.outcome());
        assertFalse(p.alive());
        assertFalse(p.inCombat);
        assertFalse(c.inCombat);
        assertEquals(0, c.victim);
    }

    @Test
    void swingWhenCreatureKillsPlayerShouldFireKillCast() {
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 1);
        p.setHealth(1);
        c.eventAi = new EventAi();
        c.eventAi.load(List.of(new EventAi.Script(EventAi.EVENT_KILL, 0, 100, EventAi.EFLAG_REPEATABLE, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        combat.swing(c, p, 20, (cr, t, id) -> casts.add(id));
        assertFalse(p.alive());
        assertEquals(List.of(7164), casts);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXHEALTH, 1);
        p.setHealth(1);
        combat.swing(c, p, 21, null);
        assertFalse(p.alive());
    }

    @Test
    void meleeRangeWhenDefaultReachesShouldBeAttackDistance() {
        assertEquals(Combat.ATTACK_DISTANCE, Combat.meleeRange(p), 1e-6f);
        assertEquals(Combat.ATTACK_DISTANCE, Combat.meleeRange(p, c), 1e-6f);
    }

    @Test
    void meleeRangeWhenCombinedReachExceedsFiveShouldUseBothReachesPlusOffset() {
        p.setFloat(UpdateFields.UNIT_FIELD_COMBATREACH, 2f);
        c.setFloat(UpdateFields.UNIT_FIELD_COMBATREACH, 2f);
        assertEquals(2f + 2f + Combat.BASE_MELEERANGE_OFFSET, Combat.meleeRange(p, c), 1e-5f);
    }

    @Test
    void meleeRangeWhenMovingLeewayShouldAddMeleeLeeway() {
        p.setFloat(UpdateFields.UNIT_FIELD_COMBATREACH, 2f);
        c.setFloat(UpdateFields.UNIT_FIELD_COMBATREACH, 2f);
        float base = Combat.meleeRange(p, c, false);
        assertEquals(base + Combat.MELEE_LEEWAY, Combat.meleeRange(p, c, true), 1e-5f);
    }

    @Test
    void meleeLeewayWhenBothRunningShouldBeTrue() {
        p.movement.moveFlags = MovementInfo.MOVEFLAG_FORWARD;
        c.movement.moveFlags = MovementInfo.MOVEFLAG_FORWARD;
        assertTrue(Combat.meleeLeeway(p, c));
        p.movement.moveFlags = MovementInfo.MOVEFLAG_FORWARD | MovementInfo.MOVEFLAG_WALK_MODE;
        assertFalse(Combat.meleeLeeway(p, c));
        assertFalse(Combat.meleeLeeway(null, c));
        assertFalse(Combat.meleeLeeway(p, null));
        p.movement.moveFlags = MovementInfo.MOVEFLAG_FORWARD;
        c.movement.moveFlags = 0;
        assertFalse(Combat.meleeLeeway(p, c));
    }

    @Test
    void attackDistanceWhenSameLevelShouldBeDetectionRange() {
        p.level = 1;
        c.level = 1;
        c.detectionRange = Combat.DEFAULT_DETECTION;
        assertEquals(Combat.DEFAULT_DETECTION, Combat.attackDistance(c, p), 1e-4f);
    }

    @Test
    void attackDistanceWhenPlayerLowerLevelShouldGrowOneYardPerLevel() {
        p.level = 1;
        c.level = 5;
        c.detectionRange = Combat.DEFAULT_DETECTION;
        assertEquals(Combat.DEFAULT_DETECTION + 4f, Combat.attackDistance(c, p), 1e-4f);
    }

    @Test
    void attackDistanceWhenNullShouldBeZero() {
        assertEquals(0f, Combat.attackDistance(null, p), 1e-4f);
        assertEquals(0f, Combat.attackDistance(c, null), 1e-4f);
    }

    @Test
    void attackDistanceWhenDetectionZeroShouldBeZero() {
        c.detectionRange = 0;
        assertEquals(0f, Combat.attackDistance(c, p), 1e-4f);
    }

    @Test
    void attackDistanceWhenPlayerMoreThan25LevelsBelowShouldCapLevelDiff() {
        p.level = 1;
        c.level = 40;
        c.detectionRange = Combat.DEFAULT_DETECTION;
        assertEquals(Combat.DEFAULT_DETECTION + 25f, Combat.attackDistance(c, p), 1e-4f);
    }

    @Test
    void attackDistanceWhenPlayerHigherLevelShouldClampAtMelee() {
        p.level = 30;
        c.level = 1;
        c.detectionRange = Combat.DEFAULT_DETECTION;
        assertEquals(Combat.ATTACK_DISTANCE, Combat.attackDistance(c, p), 1e-4f);
    }

    @Test
    void canAggroOnSightWhenNullOrDeadOrInCombatShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        setFaction(p, 1);
        setFaction(c, 7);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        Factions f = Factions.seeded();
        assertFalse(Combat.canAggroOnSight(null, p, f));
        assertFalse(Combat.canAggroOnSight(c, null, f));
        c.setHealth(0);
        assertFalse(Combat.canAggroOnSight(c, p, f));
        c.setHealth(c.maxHealth());
        p.setHealth(0);
        assertFalse(Combat.canAggroOnSight(c, p, f));
        p.setHealth(50);
        c.inCombat = true;
        assertFalse(Combat.canAggroOnSight(c, p, f));
    }

    @Test
    void canAggroOnSightWhenNoAiOrNullAiOrTooFarOrNoFactionsShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        setFaction(p, 1);
        p.relocate(2, 0, 0, 0);
        setFaction(c, 7);
        c.relocate(0, 0, 0, 0);
        Factions f = Factions.seeded();
        assertFalse(Combat.canAggroOnSight(c, p, f));
        c.aiName = "NullAI";
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertFalse(Combat.canAggroOnSight(c, p, f));
        c.aiName = "";
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        p.relocate(50, 0, 0, 0);
        assertFalse(Combat.canAggroOnSight(c, p, f));
        p.relocate(2, 0, 0, 0);
        assertFalse(Combat.canAggroOnSight(c, p, null));
        Creature other = new Creature();
        other.applyTemplate(6, "Other", 1, 7, 42, 1);
        other.relocate(2, 0, 0, 0);
        assertFalse(Combat.canAggroOnSight(c, other, f));
    }

    @Test
    void canAggroOnSightWhenGhostPlayerShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        setFaction(p, 1);
        p.setInt(UpdateFields.PLAYER_FLAGS, Player.PLAYER_FLAGS_GHOST);
        p.relocate(2, 0, 0, 0);
        setFaction(c, 7);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertFalse(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenHostileInsideDetectionShouldBeTrue() {
        setFaction(p, 1);
        p.level = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(10, 0, 0, 0);
        setFaction(c, 7);
        c.level = 1;
        c.relocate(0, 0, 0, 0);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertTrue(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenPlayerHatesMonsterGroupShouldBeTrue() {
        setFaction(p, 115);
        p.level = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(10, 0, 0, 0);
        setFaction(c, 25);
        c.level = 1;
        c.relocate(0, 0, 0, 0);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertTrue(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenDbcPredator38VsGnomeShouldBeTrue() {
        setFaction(p, 115);
        p.level = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(10, 0, 0, 0);
        setFaction(c, 38);
        c.level = 1;
        c.relocate(0, 0, 0, 0);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertTrue(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenDbcTimberWolf32VsGnomeShouldBeFalse() {
        setFaction(p, 115);
        p.level = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(2, 0, 0, 0);
        setFaction(c, 32);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertFalse(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenFriendlyNpcShouldBeFalse() {
        setFaction(p, 1);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(2, 0, 0, 0);
        setFaction(c, 12);
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertFalse(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void canAggroOnSightWhenNoAggroOnSightFlagShouldBeFalse() {
        setFaction(p, 1);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        p.relocate(2, 0, 0, 0);
        setFaction(c, 7);
        c.extraFlags = Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT;
        org.tbc.world.ai.FactorySelector.selectAI(c, null);
        assertFalse(Combat.canAggroOnSight(c, p, Factions.seeded()));
    }

    @Test
    void startAttackWhenVictimShouldSetTargetGuid() {
        combat.startAttack(p, c, 10);
        assertEquals(p.guid, c.getGuid(UpdateFields.UNIT_FIELD_TARGET));
        assertEquals(Unit.UNIT_FLAG_IN_COMBAT,
                c.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_IN_COMBAT);
    }

    @Test
    void startAttackWhenCombatMovementShouldChaseVictim() {
        combat.startAttack(p, c, 10);
        assertEquals(org.tbc.world.ai.MotionMaster.CHASE, c.motion.type());
    }

    @Test
    void startAttackWhenCombatMovementDisabledShouldStayIdle() {
        c.combatMovement = false;
        combat.startAttack(p, c, 10);
        assertEquals(org.tbc.world.ai.MotionMaster.IDLE, c.motion.type());
    }

    @Test
    void encodeAttackWhenCreatureAttackerShouldPackCreatureGuid() {
        byte[] pkt = combat.encodeAttack(c, p, new MeleeTable.Result(MeleeTable.Outcome.HIT, 2, 2));
        assertTrue(pkt.length > 8);
        int mask = pkt[4] & 0xFF;
        assertEquals(1, mask & 1, "packed guid low byte of creature guid 2");
        assertEquals(2, pkt[5] & 0xFF);
    }

    @Test
    void encodeAttackWhenCritShouldSetHitInfoCriticalHit() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.CRIT, 4, 4));
        assertEquals(Combat.HITINFO_NORMALSWING2 | Combat.HITINFO_CRITICALHIT, u32le(pkt));
    }

    @Test
    void encodeAttackWhenCrushShouldSetHitInfoCrushing() {
        byte[] pkt = combat.encodeAttack(c, p, new MeleeTable.Result(MeleeTable.Outcome.CRUSH, 3, 3));
        assertEquals(Combat.HITINFO_NORMALSWING2 | Combat.HITINFO_CRUSHING, u32le(pkt));
    }

    @Test
    void encodeAttackWhenEvadeShouldSetVictimStateEvades() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.EVADE, 0, 0));
        assertEquals(Combat.HITINFO_NORMALSWING2 | Combat.HITINFO_MISS | Combat.HITINFO_SWINGNOHITSOUND, u32le(pkt));
        WowBuffer b = new WowBuffer(pkt);
        b.getU32();
        b.getPackedGuid();
        b.getPackedGuid();
        b.getU32();
        b.getU8();
        b.getU32();
        b.getFloat();
        b.getU32();
        b.getU32();
        b.getU32();
        assertEquals(Combat.VICTIM_EVADES, b.getU32());
    }

    @Test
    void encodeAttackWhenBlockShouldWriteBlockedAmount() {
        byte[] pkt = combat.encodeAttack(p, c, new MeleeTable.Result(MeleeTable.Outcome.BLOCK, 6, 6, 4));
        WowBuffer b = new WowBuffer(pkt);
        b.getU32();
        b.getPackedGuid();
        b.getPackedGuid();
        b.getU32();
        b.getU8();
        b.getU32();
        b.getFloat();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
        assertEquals(4, b.getU32());
    }

    private static int u32le(byte[] p) {
        return (p[0] & 0xFF) | ((p[1] & 0xFF) << 8) | ((p[2] & 0xFF) << 16) | ((p[3] & 0xFF) << 24);
    }

    private static long guidAt(byte[] p) {
        long lo = (p[0] & 0xFFL) | ((p[1] & 0xFFL) << 8) | ((p[2] & 0xFFL) << 16) | ((p[3] & 0xFFL) << 24);
        long hi = (p[4] & 0xFFL) | ((p[5] & 0xFFL) << 8) | ((p[6] & 0xFFL) << 16) | ((p[7] & 0xFFL) << 24);
        return lo | (hi << 32);
    }
}
