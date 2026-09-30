package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.spell.SpellEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL08-032 — Mana Wyrm 15274 must expose mana (UnitClass mage / POWER_MANA) so Blood Elf
 * Mana Tap 28734 can drain it and the client shows a mana bar. CMaNGOS Creature::SelectLevel.
 */
class ObjectMgrManaWyrmTest {
    static final int MANA_WYRM = 15274;

    @Test
    void spawnWhenManaWyrmShouldHaveManaBarAndPowerTypeMana() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(MANA_WYRM, 530, -9449f, -183f, 68f, 0f, new ScriptRegistry());
        assertTrue(c.maxPower() > 0, "Mana Wyrm must have MaxPower (mana bar)");
        assertEquals(c.maxPower(), c.power());
        int bytes0 = c.getInt(UpdateFields.UNIT_FIELD_BYTES_0);
        assertEquals(Unit.POWER_MANA, (bytes0 >>> 24) & 0xFF);
        assertEquals(Unit.CLASS_MAGE, (bytes0 >>> 8) & 0xFF);
    }

    @Test
    void manaTapWhenManaWyrmShouldDrainMana() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(MANA_WYRM, 530, 0, 0, 0, 0, new ScriptRegistry());
        Player p = new Player();
        p.guid = 1;
        p.powerType = 0; // POWER_MANA
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 0);
        int before = c.power();
        assertTrue(before > 0);
        SpellEngine eng = SpellEngine.alwaysHit();
        int taken = eng.powerDrain(p, c, 50);
        assertEquals(50, taken);
        assertEquals(before - 50, c.power());
        assertEquals(50, p.power());
    }
}
