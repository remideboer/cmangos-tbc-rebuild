package org.tbc.world.combat;

import org.tbc.common.WowBuffer;
import org.tbc.world.ai.EventAi;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
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
    void evadeResetsAndBlocksLoot() {
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
    void lootRejectsWrongTagger() {
        c.lootable = true;
        c.taggedBy = 0;
        assertNotNull(combat.lootResponse(p, c));
        c.taggedBy = 99;
        assertNull(combat.lootResponse(p, c));
        assertNull(combat.lootResponse(p, null));
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
