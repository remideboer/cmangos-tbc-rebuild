package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.classless.CasterArmorPolicy;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL35-020 — SpellEngine applies Hero caster-armor scaling on school damage / DoT. */
class SpellEngineCasterArmorTest {

    @Test
    void applyFireballWhenHeroInPlateShouldDealOneEighth() {
        SpellEngine eng = engWithArmor();
        Player p = hero();
        equipPlate(p);
        Creature target = creature(100);
        int dealt = eng.apply(p, target, eng.info(SpellEngine.FIREBALL));
        assertEquals(1, dealt);
        assertEquals(99, target.health());
    }

    @Test
    void applyFireballWhenHeroInClothShouldDealFull() {
        SpellEngine eng = engWithArmor();
        Player p = hero();
        Creature target = creature(100);
        int dealt = eng.apply(p, target, eng.info(SpellEngine.FIREBALL));
        assertEquals(10, dealt);
    }

    @Test
    void applyHealWhenHeroInLeatherShouldHalve() {
        SpellEngine eng = engWithArmor();
        Player p = hero();
        equipLeather(p);
        Creature target = creature(50);
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        eng.apply(p, target, eng.info(SpellEngine.HOLY_LIGHT));
        // Holy Light avg (42+51)/2 = 46 → leather half → 23
        assertEquals(50 + 23, target.health());
    }

    @Test
    void tickPeriodicWhenHeroInLeatherShouldHalveDot() {
        SpellEngine eng = engWithArmor();
        Player p = hero();
        equipLeather(p);
        eng.putTemplate(900_520, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE,
                CasterArmorPolicy.SCHOOL_MASK_SHADOW, 0, 20, 20, 30f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Creature target = creature(100);
        java.util.ArrayList<Integer> ops = new java.util.ArrayList<>();
        eng.tickPeriodic(p, target, eng.info(900_520), (op, pl) -> ops.add(op));
        assertEquals(90, target.health());
        assertFalse(ops.isEmpty());
    }

    @Test
    void visibleToWhenStealthedHeroInMailAndCloseShouldReveal() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        putArmor(mgr, 900_003, ClasslessConfig.ARMOR_MAIL);
        Player stealthed = hero();
        stealthed.x = 0;
        stealthed.y = 0;
        equipSlot(stealthed, 4, 900_003);
        stealthed.setVisibility(Unit.Visibility.GROUP_STEALTH);
        Player observer = new Player();
        observer.guid = 99L;
        observer.x = 5;
        observer.y = 0;
        assertTrue(CasterArmorPolicy.visibleTo(stealthed, observer, mgr));
        observer.x = 40;
        assertFalse(CasterArmorPolicy.visibleTo(stealthed, observer, mgr));
    }

    @Test
    void applyAndTickWhenCreatureCasterShouldNotScale() {
        SpellEngine eng = engWithArmor();
        Creature caster = creature(100);
        Creature target = creature(100);
        int dealt = eng.apply(caster, target, eng.info(SpellEngine.FIREBALL));
        assertEquals(10, dealt);
        eng.putTemplate(900_521, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PERIODIC_DAMAGE,
                CasterArmorPolicy.SCHOOL_MASK_SHADOW, 0, 20, 20, 30f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        target.setHealth(100);
        eng.tickPeriodic(caster, target, eng.info(900_521), (op, pl) -> { });
        assertEquals(80, target.health());
        Creature healTarget = creature(10);
        healTarget.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        eng.apply(caster, healTarget, eng.info(SpellEngine.HOLY_LIGHT));
        assertEquals(10 + 46, healTarget.health());
    }

    @Test
    void applyWeaponDamageWhenHeroInPlateShouldNotScale() {
        SpellEngine eng = engWithArmor();
        Player p = hero();
        equipPlate(p);
        eng.putTemplate(900_522, SpellEngine.EFFECT_WEAPON_DAMAGE, 0, 0, 0, 20, 20, 5f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Creature target = creature(100);
        int dealt = eng.apply(p, target, eng.info(900_522));
        assertEquals(20, dealt);
    }

    private static SpellEngine engWithArmor() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        putArmor(mgr, 900_002, ClasslessConfig.ARMOR_LEATHER);
        putArmor(mgr, 900_004, ClasslessConfig.ARMOR_PLATE);
        return SpellEngine.alwaysHit(SpellWiring.defaults().withObjectMgr(mgr));
    }

    private static void putArmor(ObjectMgr mgr, int entry, int sub) {
        ObjectMgr.ItemTemplate t = new ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.itemClass = Player.ITEM_CLASS_ARMOR;
        t.subClass = sub;
        t.inventoryType = 5;
        t.armor = 50;
        mgr.items.put(entry, t);
    }

    private static Creature creature(int hp) {
        Creature c = new Creature();
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, hp);
        c.setHealth(hp);
        return c;
    }

    private static Player hero() {
        Player p = new Player();
        p.guid = 1L;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 10;
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        return p;
    }

    private static void equipPlate(Player p) {
        equipSlot(p, 4, 900_004);
    }

    private static void equipLeather(Player p) {
        equipSlot(p, 4, 900_002);
    }

    private static void equipSlot(Player p, int slot, int entry) {
        Item it = new Item(20L + slot, entry);
        it.bag = 0;
        it.slot = slot;
        it.count = 1;
        p.items.values().removeIf(i -> i.bag == 0 && i.slot == slot);
        p.items.put((int) it.guid, it);
    }
}
