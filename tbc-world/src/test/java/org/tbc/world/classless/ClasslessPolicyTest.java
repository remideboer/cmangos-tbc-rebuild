package org.tbc.world.classless;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
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
    void trainerPolicyWhenClasslessShouldAllowAnyClassTrainerSpell() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertTrue(ClasslessTrainerPolicy.mayBuy(p, 6673));
        assertTrue(ClasslessTrainerPolicy.mayBuy(p, 99999));
        assertTrue(ClasslessTrainerPolicy.listIncludes(p, 6673));
        assertTrue(ClasslessTrainerPolicy.listIncludes(p, 99999));
        Player warrior = new Player();
        warrior.clazz = 1;
        assertTrue(ClasslessTrainerPolicy.listIncludes(warrior, 99999));
        assertFalse(ClasslessTrainerPolicy.mayBuy(warrior, 6673));
    }

    @Test
    void trainerPolicyWhenClasslessShouldAcceptAnyClassTrainerNpc() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature mage = new Creature();
        mage.entry = Content.NPC_KHELDEN_BREMEN;
        assertTrue(ClasslessTrainerPolicy.isTrainerOf(p, mage, mgr));
        assertTrue(mgr.isTrainerOf(p, mage));
        Player warrior = new Player();
        warrior.clazz = 1;
        assertFalse(mgr.isTrainerOf(warrior, mage));
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

    /** TP-SL35-008 — Blood Elf has no warrior createinfo; classless must still use Sunstrider, not Northshire. */
    @Test
    void startingLoadoutWhenBloodElfClasslessShouldSpawnSunstriderIsle() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        // SQL-shaped: BE mage only (no warrior row) — mirrors tbcmangos playercreateinfo.
        mgr.createInfo.put(ObjectMgr.key(10, 8),
                new ObjectMgr.CreateInfo(10, 8, 530, 3431, 10349.6f, -6357.29f, 33.4026f, 0f));
        Player p = new Player();
        p.guid = 2L;
        p.race = 10;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        long[] next = {200L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        assertEquals(530, p.mapId);
        assertEquals(3431, p.zoneId);
        assertEquals(10349.6f, p.x, 0.01f);
        assertEquals(-6357.29f, p.y, 0.01f);
        assertEquals(530, p.bindMap);
        assertEquals(3431, p.bindZone);
    }

    @Test
    void startingLoadoutWhenHumanClasslessShouldStayNorthshire() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 3L;
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        long[] next = {300L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        assertEquals(0, p.mapId);
        assertEquals(12, p.zoneId);
    }

    /** TP-SL35-011 — Hero L1 STAT0..4 = rounded mean of that race's class rows (flat, racial lean kept). */
    @Test
    void startingStatsWhenHumanClasslessShouldUseRaceClassAverage() {
        Player p = new Player();
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(p);
        // Human L1 mean of war/pal/rogue/priest/mage/lock seeds → 21/21/21/21/21
        assertEquals(21, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(21, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT1));
        assertEquals(21, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(21, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(21, p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT4));
    }

    @Test
    void startingStatsWhenOrcClasslessShouldCarryRacialLeanVsHuman() {
        Player human = new Player();
        human.race = 1;
        human.clazz = ClasslessConfig.CLASS_CLASSLESS;
        human.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(human);
        Player orc = new Player();
        orc.race = 2;
        orc.clazz = ClasslessConfig.CLASS_CLASSLESS;
        orc.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(orc);
        assertTrue(orc.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT0)
                        > human.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT0),
                "orc Hero STR above human Hero");
        assertTrue(orc.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT3)
                        < human.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_STAT3),
                "orc Hero INT below human Hero");
    }

    /** TP-SL35-014 — classless must set PLAYER_NEXT_LEVEL_XP so the client XP bar shows. */
    @Test
    void startingStatsWhenClasslessShouldSetNextLevelXpForBar() {
        Player p = new Player();
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(p);
        assertEquals(400, p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_NEXT_LEVEL_XP));
        assertEquals(0, p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_XP));
        int[] changed = p.giveXp(50, null);
        assertTrue(changed.length > 0);
        assertEquals(50, p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_XP));
    }

    /** TP-SL35-016 — buyCost = spellcost × (learned + 1). */
    @Test
    void buyCostWhenZeroLearnedShouldBeOneTimesSpellCost() {
        assertEquals(100, ClasslessTrainerPolicy.buyCost(100, 0));
        assertEquals(100, ClasslessTrainerPolicy.buyCost(100, 1) / 2);
        assertEquals(200, ClasslessTrainerPolicy.buyCost(100, 1));
        assertEquals(300, ClasslessTrainerPolicy.buyCost(100, 2));
        assertEquals(0, ClasslessTrainerPolicy.buyCost(0, 5));
        assertEquals(Integer.MAX_VALUE, ClasslessTrainerPolicy.buyCost(Integer.MAX_VALUE / 2, 2));
    }

    @Test
    void classSpellsLearnedShouldExcludeAutoAttack() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        p.spells.add(ChrStatic.SPELL_LANG_COMMON);
        assertEquals(0, ClasslessTrainerPolicy.classSpellsLearned(p));
        p.spells.add(6673);
        assertEquals(1, ClasslessTrainerPolicy.classSpellsLearned(p));
        p.spells.add(133);
        assertEquals(2, ClasslessTrainerPolicy.classSpellsLearned(p));
    }
}
