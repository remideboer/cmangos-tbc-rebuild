package org.tbc.world.entity;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL17-013 — CMaNGOS Player::Update only RegenerateAll if IsAlive() (not ghost / corpse). */
class PlayerRegenerateAllTest {
    private Player p;

    @BeforeEach
    void mageWithSpiritRegen() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        p = new Player();
        p.race = 1;
        p.clazz = 8;
        p.level = 1;
        p.initStatsForLevel(mgr.levelStats);
        p.applyCreateFields();
        p.setHealth(20);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 100);
    }

    @Test
    void regenerateAllWhenAliveOutOfCombatShouldRaiseHealthAndMana() {
        int[] changed = p.regenerateAll(Player.REGEN_TIME_FULL);
        assertTrue(changed.length > 0);
        assertTrue(p.health() > 20);
        assertTrue(p.getInt(UpdateFields.UNIT_FIELD_POWER1) > 100);
    }

    @Test
    void regenerateAllWhenGhostShouldNotRaiseHealthOrMana() {
        p.setGhost(true);
        p.setHealth(1);
        int[] changed = p.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(0, changed.length);
        assertEquals(1, p.health());
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void regenerateAllWhenCorpseShouldNotRaiseHealthOrMana() {
        p.setHealth(0);
        int[] changed = p.regenerateAll(Player.REGEN_TIME_FULL);
        assertEquals(0, changed.length);
        assertEquals(0, p.health());
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }
}
