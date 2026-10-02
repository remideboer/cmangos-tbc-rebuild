package org.tbc.world.classless;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.spell.SpellEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL35-020 — Hero caster armor halvings + Battlecaster + heavy-armor stealth. */
class CasterArmorPolicyTest {

    @AfterEach
    void reset() {
        ClasslessConfig.reset();
    }

    @Test
    void casterEffectivenessWhenClothShouldBeFull() {
        ObjectMgr mgr = seededMgr();
        Player p = hero();
        equip(p, mgr, 3, clothChest(mgr));
        assertEquals(0, CasterArmorPolicy.heaviestArmorStep(p, mgr));
        assertEquals(1f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
    }

    @Test
    void casterEffectivenessWhenLeatherMailPlateShouldHalveEachStep() {
        ObjectMgr mgr = seededMgr();
        Player p = hero();
        equip(p, mgr, 4, leatherChest(mgr));
        assertEquals(0.5f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, mailChest(mgr));
        assertEquals(0.25f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, plateChest(mgr));
        assertEquals(0.125f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
    }

    @Test
    void battlecasterLeatherWhenWearingLeatherShouldRestoreFull() {
        ObjectMgr mgr = seededMgr();
        Player p = hero();
        p.spells.add(CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER);
        equip(p, mgr, 4, leatherChest(mgr));
        assertEquals(1f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, mailChest(mgr));
        assertEquals(0.5f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, plateChest(mgr));
        assertEquals(0.25f, CasterArmorPolicy.casterEffectiveness(p, mgr), 1e-6f);
    }

    @Test
    void scaleCasterAmountWhenFireballInPlateShouldBeOneEighth() {
        ObjectMgr mgr = seededMgr();
        Player p = hero();
        equip(p, mgr, 4, plateChest(mgr));
        SpellEngine eng = new SpellEngine();
        SpellEngine.SpellInfo fb = eng.info(SpellEngine.FIREBALL);
        // Fireball avg (8+12)/2 = 10 → plate 1/8 → 1 (rounded, floor at 1).
        assertEquals(10, (fb.minDmg() + fb.maxDmg()) / 2);
        assertEquals(1, CasterArmorPolicy.scaleCasterAmount(p, 10, fb, mgr));
        assertEquals(1, CasterArmorPolicy.scaleCasterAmount(p, 8, fb, mgr));
        p.spells.clear();
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        equip(p, mgr, 4, leatherChest(mgr));
        assertEquals(4, CasterArmorPolicy.scaleCasterAmount(p, 8, fb, mgr));
    }

    @Test
    void scaleCasterAmountWhenPhysicalOrWarriorShouldIgnore() {
        ObjectMgr mgr = seededMgr();
        Player hero = hero();
        equip(hero, mgr, 4, plateChest(mgr));
        SpellEngine.SpellInfo phys = new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_SCHOOL_DAMAGE, 0, 0, 0, 20, 20, 5f);
        assertEquals(20, CasterArmorPolicy.scaleCasterAmount(hero, 20, phys, mgr));
        Player warrior = new Player();
        warrior.clazz = 1;
        equip(warrior, mgr, 4, plateChest(mgr));
        SpellEngine eng = new SpellEngine();
        assertEquals(10, CasterArmorPolicy.scaleCasterAmount(warrior, 10, eng.info(SpellEngine.FIREBALL), mgr));
    }

    @Test
    void stealthEffectivenessWhenMailOrPlateShouldBeOneThird() {
        ObjectMgr mgr = seededMgr();
        Player p = hero();
        equip(p, mgr, 4, leatherChest(mgr));
        assertEquals(1f, CasterArmorPolicy.stealthEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, mailChest(mgr));
        assertEquals(1f / 3f, CasterArmorPolicy.stealthEffectiveness(p, mgr), 1e-6f);
        equip(p, mgr, 4, plateChest(mgr));
        assertEquals(1f / 3f, CasterArmorPolicy.stealthEffectiveness(p, mgr), 1e-6f);
    }

    @Test
    void battlecasterReqLevelsShouldMatchMaxLevelFractions() {
        assertEquals(8, CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_LEATHER);
        assertEquals(17, CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_MAIL);
        assertEquals(35, CasterArmorPolicy.REQ_LEVEL_BATTLECASTER_PLATE);
        assertTrue(CasterArmorPolicy.isBattlecasterSpell(CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER));
        assertFalse(CasterArmorPolicy.isBattlecasterSpell(133));
    }

    @Test
    void isCasterSchoolWhenHolyFireFrostShadowArcaneShouldPass() {
        assertTrue(CasterArmorPolicy.isCasterSchool(CasterArmorPolicy.SCHOOL_MASK_FIRE));
        assertTrue(CasterArmorPolicy.isCasterSchool(CasterArmorPolicy.SCHOOL_MASK_SHADOW));
        assertTrue(CasterArmorPolicy.isCasterSchool(5));
        assertFalse(CasterArmorPolicy.isCasterSchool(0));
        assertFalse(CasterArmorPolicy.isCasterSchool(3)); // nature index
        assertFalse(CasterArmorPolicy.isCasterSchool(0x8)); // nature mask
    }

    private static ObjectMgr seededMgr() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        putArmor(mgr, 900_001, ClasslessConfig.ARMOR_CLOTH);
        putArmor(mgr, 900_002, ClasslessConfig.ARMOR_LEATHER);
        putArmor(mgr, 900_003, ClasslessConfig.ARMOR_MAIL);
        putArmor(mgr, 900_004, ClasslessConfig.ARMOR_PLATE);
        return mgr;
    }

    private static void putArmor(ObjectMgr mgr, int entry, int sub) {
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.itemClass = Player.ITEM_CLASS_ARMOR;
        t.subClass = sub;
        t.inventoryType = 5;
        t.armor = 100;
        mgr.items.put(entry, t);
    }

    private static ObjectMgr.ItemTemplate clothChest(ObjectMgr mgr) {
        return mgr.items.get(900_001);
    }

    private static ObjectMgr.ItemTemplate leatherChest(ObjectMgr mgr) {
        return mgr.items.get(900_002);
    }

    private static ObjectMgr.ItemTemplate mailChest(ObjectMgr mgr) {
        return mgr.items.get(900_003);
    }

    private static ObjectMgr.ItemTemplate plateChest(ObjectMgr mgr) {
        return mgr.items.get(900_004);
    }

    private static Player hero() {
        Player p = new Player();
        p.guid = 1L;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        return p;
    }

    private static void equip(Player p, ObjectMgr mgr, int slot, ObjectMgr.ItemTemplate t) {
        Item it = new Item(10L + slot, t.entry);
        it.bag = 0;
        it.slot = slot;
        it.count = 1;
        p.items.values().removeIf(i -> i.bag == 0 && i.slot == slot);
        p.items.put((int) it.guid, it);
    }
}
