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
        assertFalse(ClasslessTrainerPolicy.mayBuy(p, Content.SPELL_BATTLE_SHOUT),
                "Battle Shout gated on follow-up quest 90002");
        assertFalse(ClasslessTrainerPolicy.listIncludes(p, Content.SPELL_BATTLE_SHOUT));
        assertTrue(ClasslessTrainerPolicy.mayBuy(p, 99999));
        assertTrue(ClasslessTrainerPolicy.listIncludes(p, 99999));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        assertTrue(ClasslessTrainerPolicy.mayBuy(p, Content.SPELL_BATTLE_SHOUT));
        assertTrue(ClasslessTrainerPolicy.listIncludes(p, Content.SPELL_BATTLE_SHOUT));
        Player warrior = new Player();
        warrior.clazz = 1;
        assertTrue(ClasslessTrainerPolicy.listIncludes(warrior, 99999));
        assertFalse(ClasslessTrainerPolicy.mayBuy(warrior, Content.SPELL_BATTLE_SHOUT));
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
        assertFalse(ClasslessTrainerPolicy.isTrainerOf(p, null, mgr));
        assertFalse(ClasslessTrainerPolicy.isTrainerOf(p, mage, null));
    }

    @Test
    void trainerPolicyWhenClasslessWarriorTrainerShouldRequireUnlockQuest() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature llane = new Creature();
        llane.entry = Content.NPC_LLANE_BESHERE;
        llane.npcFlags = Content.UNIT_NPC_FLAG_TRAINER;
        assertFalse(ClasslessTrainerPolicy.isTrainerOf(p, llane, mgr));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertTrue(ClasslessTrainerPolicy.isTrainerOf(p, llane, mgr));
        assertTrue(mgr.isTrainerOf(p, llane));
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

    /** TP-SL35-019 — Hero create grants Hearthstone 6948 bound to the race starter zone. */
    @Test
    void startingLoadoutWhenClasslessShouldGrantHearthstoneBoundToStarter() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 4L;
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        long[] next = {400L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_HEARTHSTONE));
        assertEquals(p.mapId, p.bindMap);
        assertEquals(p.zoneId, p.bindZone);
        assertEquals(p.x, p.bindX, 0.01f);
        assertEquals(p.y, p.bindY, 0.01f);
        assertEquals(p.z, p.bindZ, 0.01f);
        assertEquals(0, p.bindMap);
        assertEquals(12, p.bindZone);
    }

    /** Hero create backpack: Refreshing Spring Water ×5, Tough Hunk of Bread ×5, Minor Healing Potion ×1. */
    @Test
    void startingLoadoutWhenClasslessShouldGrantWaterBreadAndMinorHealingPotion() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 5L;
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        long[] next = {500L};
        ClasslessStartingLoadout.apply(p, mgr, () -> next[0]++);
        assertEquals(5, countOwned(p, Content.ITEM_REFRESHING_SPRING_WATER));
        assertEquals(5, countOwned(p, Content.ITEM_TOUGH_HUNK_OF_BREAD));
        assertEquals(1, countOwned(p, Content.ITEM_MINOR_HEALING_POTION));
    }

    private static int countOwned(Player p, int entry) {
        int n = 0;
        for (var it : p.items.values()) {
            if (it.entry == entry) {
                n += it.count;
            }
        }
        return n;
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

    /** TP-SL35-025 — ding VALUES must include Hero rage/energy bars, not only mana. */
    @Test
    void giveXpWhenClasslessDingsShouldIncludeRageAndEnergyFields() {
        Player p = new Player();
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(p);
        int[] changed = p.giveXp(400, null);
        assertEquals(2, p.level);
        assertTrue(p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1) > 0);
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1));
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER1));
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER2));
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER2));
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertTrue(java.util.Arrays.stream(changed).anyMatch(
                f -> f == org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER4));
    }

    /** TP-SL35-016 — buyCost = 100 × 2^learned (ignore trainer-row spellCost). */
    @Test
    void buyCostWhenLearnedShouldFollowGeometricHundred() {
        assertEquals(100, ClasslessTrainerPolicy.buyCost(0));
        assertEquals(200, ClasslessTrainerPolicy.buyCost(1));
        assertEquals(400, ClasslessTrainerPolicy.buyCost(2));
        assertEquals(800, ClasslessTrainerPolicy.buyCost(3));
        assertEquals(1_600, ClasslessTrainerPolicy.buyCost(4));
        assertEquals(Integer.MAX_VALUE, ClasslessTrainerPolicy.buyCost(31));
        assertEquals(Integer.MAX_VALUE, ClasslessTrainerPolicy.buyCost(25));
    }

    @Test
    void effectiveCostWhenClasslessShouldIgnoreRowSpellCost() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.spells.add(ClasslessConfig.AUTO_ATTACK);
        assertEquals(100, ClasslessTrainerPolicy.effectiveCost(p, 999));
        p.spells.add(6673);
        assertEquals(200, ClasslessTrainerPolicy.effectiveCost(p, 10));
        Player warrior = new Player();
        warrior.clazz = 1;
        assertEquals(500, ClasslessTrainerPolicy.effectiveCost(warrior, 500));
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

    /** TP-SL35-017 — classless RewardRage writes POWER2, not mana. */
    @Test
    void rewardRageFromHitWhenClasslessShouldRaiseRageNotMana() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        p.powerType = Player.POWER_MANA;
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_POWER1, 100);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_MAXPOWER2, Player.POWER_RAGE_MAX);
        p.setRage(0);
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.rewardRageFromHit(10, false);
        assertTrue(p.rage() > 0, "Hero melee should gain stored rage");
        assertEquals(100, p.power(), "mana primary must stay unchanged");
    }
}
