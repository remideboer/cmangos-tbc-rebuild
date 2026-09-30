package org.tbc.world.combat;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SoR combat inputs follow stock tip tokens $MW/$mw/$MWS (UNIT when synced). */
class MainhandWeaponStatsTest {

    @Test
    void fromWhenUnitSyncedShouldPreferUnitOverStaleItem() {
        Player p = player();
        Item mh = new Item(1, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(SpellEngine.INVTYPE_2HWEAPON, MainhandWeaponStats.SUBCLASS_SWORD2, 3500, 80f, 100f);
        p.items.put(1, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2900);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 3f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 5f);

        MainhandWeaponStats s = MainhandWeaponStats.from(p);
        assertEquals(2.9f, s.speedSec());
        assertEquals(4f, s.avgDamage());
        assertEquals(3f, s.damageMin());
        assertEquals(5f, s.damageMax());
    }

    @Test
    void fromWhenUnitIsFistDefaultShouldPreferItemProto() {
        Player p = player();
        Item mh = new Item(1, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(SpellEngine.INVTYPE_2HWEAPON, MainhandWeaponStats.SUBCLASS_SWORD2, 3500, 80f, 100f);
        p.items.put(1, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3f);

        MainhandWeaponStats s = MainhandWeaponStats.from(p);
        assertEquals(3.5f, s.speedSec());
        assertEquals(90f, s.avgDamage());
        assertTrue(s.twoHand());
    }

    @Test
    void fromWhenTwoHandSubclassButInventoryTypeOneHandShouldStillBeTwoHand() {
        Player p = player();
        Item mh = new Item(3, 2361);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(21, MainhandWeaponStats.SUBCLASS_MACE2, 3500, 80f, 100f);
        p.items.put(3, mh);

        MainhandWeaponStats s = MainhandWeaponStats.from(p);
        assertTrue(s.twoHand());
    }

    @Test
    void fromWhenMainhandDelayZeroShouldFallBackToUnitField() {
        Player p = player();
        Item mh = new Item(2, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = 13;
        mh.delay = 0;
        p.items.put(2, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2400);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 10f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 14f);

        MainhandWeaponStats s = MainhandWeaponStats.from(p);
        assertEquals(2.4f, s.speedSec());
        assertEquals(12f, s.avgDamage());
        assertFalse(s.twoHand());
    }

    @Test
    void fromWhenNoMainhandAndZeroAttackTimeShouldUseFistDefaults() {
        Player p = player();
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 0);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 0f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 0f);

        MainhandWeaponStats s = MainhandWeaponStats.from(p);
        assertEquals(2.0f, s.speedSec());
        assertEquals(2000, s.delayMs());
    }

    @Test
    void fromWhenNullPlayerShouldReturnFistDefaults() {
        MainhandWeaponStats s = MainhandWeaponStats.from(null);
        assertEquals(2.0f, s.speedSec());
        assertFalse(s.twoHand());
    }

    private static Player player() {
        Player p = new Player();
        p.guid = 1;
        p.name = "W";
        return p;
    }
}
