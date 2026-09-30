package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * applyEquippedMelee must heal zero-delay Item weapon lines from the template so
 * UNIT_FIELD attack stats (and SoR) match the equipped weapon, not fist defaults.
 */
class ObjectMgrEquippedMeleeTest {

    private static final int ENTRY_2H = 900_351;

    @Test
    void applyEquippedMeleeWhenMainhandDelayZeroShouldHealItemAndUnitFields() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = ENTRY_2H;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(ENTRY_2H, t);

        Player p = new Player();
        p.guid = 1;
        p.name = "P";
        Item mh = new Item(10, ENTRY_2H);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        mh.delay = 0;
        mh.dmgMin = 0f;
        mh.dmgMax = 0f;
        p.items.put(10, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3f);

        mgr.applyEquippedMelee(p);

        assertEquals(3500, mh.delay);
        assertEquals(80f, mh.dmgMin);
        assertEquals(100f, mh.dmgMax);
        assertEquals(SpellEngine.INVTYPE_2HWEAPON, mh.inventoryType);
        assertEquals(3500, p.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(80f, p.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
        assertEquals(100f, p.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE));
    }

    @Test
    void applyEquippedMeleeAfterCreateFieldsShouldKeepWeaponUnitFields() {
        // Login order: applyCreateFields then applyEquippedMelee — weapon avg must win for action bar.
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = ENTRY_2H;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(ENTRY_2H, t);

        Player p = new Player();
        p.guid = 1;
        p.name = "P";
        p.race = 1;
        p.clazz = 2;
        p.level = 1;
        Item mh = new Item(12, ENTRY_2H);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.delay = 0;
        p.items.put(12, mh);

        p.applyCreateFields();
        mgr.applyEquippedMelee(p);

        assertEquals(3500, p.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(80f, p.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
        assertEquals(100f, p.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE));
    }

    @Test
    void applyEquippedMeleeWhenInventoryTypeWrongButDelaySetShouldHealHandedness() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = ENTRY_2H;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(ENTRY_2H, t);

        Player p = new Player();
        p.guid = 1;
        Item mh = new Item(11, ENTRY_2H);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = 13;
        mh.delay = 3500;
        mh.dmgMin = 80f;
        mh.dmgMax = 100f;
        p.items.put(11, mh);

        mgr.applyEquippedMelee(p);

        assertEquals(SpellEngine.INVTYPE_2HWEAPON, mh.inventoryType);
        assertEquals(3500, mh.delay);
        assertEquals(80f, mh.dmgMin);
        assertEquals(100f, mh.dmgMax);
    }

    @Test
    void applyWeaponProtoWhenTwoHandSubclassShouldNormalizeInventoryType() {
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.subClass = org.tbc.world.combat.MainhandWeaponStats.SUBCLASS_MACE2;
        t.inventoryType = 21;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        Item it = new Item(1, 2361);
        ObjectMgr.applyWeaponProto(it, t);
        assertEquals(SpellEngine.INVTYPE_2HWEAPON, it.inventoryType);
        assertEquals(org.tbc.world.combat.MainhandWeaponStats.SUBCLASS_MACE2, it.subClass);
        assertEquals(3500, it.delay);
    }

    @Test
    void applyWeaponProtoWhenNullShouldNoOp() {
        ObjectMgr.applyWeaponProto(null, null);
        Item it = new Item(1, 25);
        ObjectMgr.applyWeaponProto(it, null);
        assertEquals(0, it.delay);
    }

    @Test
    void applyEquippedMeleeWhenOnEquipSpellDamageRegisteredShouldSetHolySpellPower() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        int entry = 900_360;
        int spellId = 900_361;
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.inventoryType = 11; // finger
        t.spellId[0] = spellId;
        t.spellTrigger[0] = 1; // ON_EQUIP
        mgr.items.put(entry, t);
        mgr.registerOnEquipSpellDamageDone(spellId, 1, 14);

        Player p = new Player();
        p.guid = 1;
        Item ring = new Item(20, entry);
        ring.slot = 10; // finger1
        p.items.put(20, ring);
        p.updateSpellDamageBonusDone(99, 0, 0, 0, 0, 0);

        mgr.applyEquippedMelee(p);

        assertEquals(14, p.holySpellPower());
    }

    @Test
    void applyEquippedMeleeWhenNoSpellDamageGearShouldClearHolySpellPower() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1;
        p.updateSpellDamageBonusDone(14, 0, 0, 0, 0, 0);
        mgr.applyEquippedMelee(p);
        assertEquals(0, p.holySpellPower());
    }

    @Test
    void registerOnEquipSpellDamageDoneWhenSchoolOutOfRangeShouldIgnore() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.registerOnEquipSpellDamageDone(1, 0, 10);
        mgr.registerOnEquipSpellDamageDone(0, 1, 10);
        mgr.registerOnEquipSpellDamageDone(1, 7, 10);
        Player p = new Player();
        mgr.applyEquippedMelee(p);
        assertEquals(0, p.holySpellPower());
    }

    /** Shield is armor — must not keep dual-wield OH damage / LEFTSWING swings. */
    @Test
    void applyEquippedMeleeWhenShieldInOffhandShouldClearOffhandDamageFields() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1;
        p.setFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 7f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, 7f);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, 1600);
        Item shield = new Item(8, Content.ITEM_WORN_WOODEN_SHIELD);
        shield.bag = 0;
        shield.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        p.items.put(8, shield);

        mgr.applyEquippedMelee(p);

        assertEquals(0f, p.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE));
        assertEquals(0f, p.getFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE));
    }

    @Test
    void applyEquippedMeleeWhenOffhandWeaponShouldSetOffhandDamageFields() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        int entry = 900_370;
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.itemClass = Player.ITEM_CLASS_WEAPON;
        t.inventoryType = 13;
        t.delay = 1800;
        t.dmgMin[0] = 4f;
        t.dmgMax[0] = 6f;
        mgr.items.put(entry, t);

        Player p = new Player();
        p.guid = 1;
        Item oh = new Item(9, entry);
        oh.bag = 0;
        oh.slot = Player.EQUIPMENT_SLOT_OFFHAND;
        p.items.put(9, oh);

        mgr.applyEquippedMelee(p);

        assertEquals(4f, p.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE));
        assertEquals(6f, p.getFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE));
        assertEquals(1800, p.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }
}
