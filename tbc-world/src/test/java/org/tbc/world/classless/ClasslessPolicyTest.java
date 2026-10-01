package org.tbc.world.classless;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasslessPolicyTest {
    @AfterEach
    void reset() {
        ClasslessConfig.reset();
    }

    @Test
    void configWhenDefaultsShouldMapArmorSteps() {
        ClasslessConfig cfg = ClasslessConfig.defaults();
        assertEquals(0, cfg.armorStep(ClasslessConfig.ARMOR_CLOTH));
        assertEquals(1, cfg.armorStep(ClasslessConfig.ARMOR_LEATHER));
        assertEquals(2, cfg.armorStep(ClasslessConfig.ARMOR_MAIL));
        assertEquals(3, cfg.armorStep(ClasslessConfig.ARMOR_PLATE));
        assertEquals(0.10f, cfg.armorReductionForStep(1), 1e-6);
        assertEquals(0.30f, cfg.strAgiReductionForStep(3), 1e-6);
        assertTrue(cfg.trainerSpellEligible(6673));
    }

    @Test
    void armorPenaltyWhenUnproficientLeatherShouldReduce() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.addArmorProficiency(ClasslessConfig.ARMOR_CLOTH_MASK);
        ObjectMgr.ItemTemplate leather = ObjectMgr.ItemTemplate.tunicOfWestfall();
        var mods = ArmorPenaltyPolicy.forPiece(p, leather);
        assertEquals(Math.round(92 * 0.9f), mods.armor());
        assertEquals(Math.round(11 * 0.9f), mods.agility());
        assertEquals(0.05f, mods.speedPenaltyPct(), 1e-6);
    }

    @Test
    void armorPenaltyWhenClothProficientShouldPassThrough() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.addArmorProficiency(ClasslessConfig.ARMOR_CLOTH_MASK);
        ObjectMgr.ItemTemplate cloth = ObjectMgr.ItemTemplate.seersRobe();
        var mods = ArmorPenaltyPolicy.forPiece(p, cloth);
        assertEquals(35, mods.armor());
        assertEquals(0f, mods.speedPenaltyPct(), 1e-6);
    }

    @Test
    void trainerPolicyWhenClasslessShouldAllowClassTrainer() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertTrue(ClasslessTrainerPolicy.mayBuy(p, 6673));
        assertFalse(ClasslessTrainerPolicy.mayBuy(p, 99999));
        assertTrue(ClasslessTrainerPolicy.listIncludes(p, 6673));
        assertFalse(ClasslessTrainerPolicy.listIncludes(p, 99999));
        Player warrior = new Player();
        warrior.clazz = 1;
        assertTrue(ClasslessTrainerPolicy.listIncludes(warrior, 99999));
    }

    @Test
    void characterPolicyWhenDisabledShouldNotBeClassless() {
        ClasslessConfig.set(ClasslessConfig.defaults().withEnabled(false));
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(ClasslessCharacterPolicy.isClassless(p));
    }

    @Test
    void startingLoadoutWhenAppliedShouldEquipRecruitGearAndThreeSilver() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1L;
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        long[] next = {100L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER, p.money);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_SHIRT, p.itemAt(0, 3).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_PANTS, p.itemAt(0, 6).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_BOOTS, p.itemAt(0, 7).entry);
        assertEquals(ClasslessConfig.STARTER_WEAPON, p.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND).entry);
    }
}
