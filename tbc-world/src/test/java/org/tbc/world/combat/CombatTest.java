package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void swingSkipsDeadAndEvading() {
        c.setHealth(0);
        assertEquals(0, combat.swing(p, c, 1).damage());
        c.setHealth(42);
        c.evading = true;
        assertEquals(0, combat.swing(p, c, 1).damage());
        assertEquals(42, c.health());
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

    private static long guidAt(byte[] p) {
        long lo = (p[0] & 0xFFL) | ((p[1] & 0xFFL) << 8) | ((p[2] & 0xFFL) << 16) | ((p[3] & 0xFFL) << 24);
        long hi = (p[4] & 0xFFL) | ((p[5] & 0xFFL) << 8) | ((p[6] & 0xFFL) << 16) | ((p[7] & 0xFFL) << 24);
        return lo | (hi << 32);
    }
}
