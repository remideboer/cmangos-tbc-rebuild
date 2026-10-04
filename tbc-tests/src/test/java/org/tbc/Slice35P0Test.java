package org.tbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.classless.HeroClassUnlock;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice 35 — classless character mode (ChrClasses id 6).
 */
class Slice35P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    @AfterEach
    void resetConfig() {
        ClasslessConfig.reset();
    }

    /** TP-SL35-001 — create class 6 succeeds when enabled; rejected when disabled. */
    @Test
    void tpSl35CreateClasslessWhenEnabledShouldSucceed() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        client.handle(world, Opcodes.CMSG_CHAR_CREATE, charCreate("Classlessone", 1, ClasslessConfig.CLASS_CLASSLESS).array());
        assertEquals(Codes.CHAR_CREATE_SUCCESS, client.payload(Opcodes.SMSG_CHAR_CREATE)[0] & 0xFF);
        Player p = world.characters.enumAccount(ACC.id(), world.objectMgr).stream()
                .filter(c -> c.name.equals("Classlessone")).findFirst().orElseThrow();
        assertEquals(ClasslessConfig.CLASS_CLASSLESS, p.clazz);
    }

    @Test
    void tpSl35CreateClasslessWhenDisabledShouldCharCreateError() {
        ClasslessConfig.set(ClasslessConfig.defaults().withEnabled(false));
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        client.handle(world, Opcodes.CMSG_CHAR_CREATE, charCreate("Noclassless", 1, ClasslessConfig.CLASS_CLASSLESS).array());
        assertEquals(Codes.CHAR_CREATE_ERROR, client.payload(Opcodes.SMSG_CHAR_CREATE)[0] & 0xFF);
    }

    /** TP-SL35-002 — Auto Attack only on bar; Recruit cloth + Worn Shortsword; full proficiencies; 3 silver. */
    @Test
    void tpSl35BlankStartShouldHaveAutoAttackOnly() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Blankslate", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        assertNotNull(created);
        assertTrue(created.spells.contains(ClasslessConfig.AUTO_ATTACK));
        assertFalse(created.spells.contains(78), "no Heroic Strike");
        assertFalse(created.spells.contains(Content.SPELL_BATTLE_SHOUT));
        assertEquals(ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK,
                created.armorProficiency() & ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK);
        assertEquals(ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK,
                created.weaponProficiency() & ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK);
        assertTrue((created.weaponProficiency() & (1 << 7)) != 0, "sword proficiency on create");
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER, created.money);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_SHIRT, created.itemAt(0, 3).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_PANTS, created.itemAt(0, 6).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_BOOTS, created.itemAt(0, 7).entry);
        assertEquals(ClasslessConfig.STARTER_WEAPON, created.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND).entry);
        assertTrue(created.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_HEARTHSTONE),
                "Hero create must include Hearthstone 6948");
        assertEquals(created.mapId, created.bindMap);
        assertEquals(created.zoneId, created.bindZone);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertTrue(p.spells.contains(ClasslessConfig.AUTO_ATTACK));
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER, p.money);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_SHIRT, p.itemAt(0, 3).entry);
        assertEquals(ClasslessConfig.STARTER_WEAPON, p.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND).entry);
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_HEARTHSTONE));
        assertEquals(p.mapId, p.bindMap);
        assertEquals(p.zoneId, p.bindZone);
        byte[] bar = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(ClasslessConfig.AUTO_ATTACK, WowClientDouble.u32le(bar, 0));
        assertEquals(0, WowClientDouble.u32le(bar, 73 * 4), "no HS on stance bar");
    }

    /**
     * TP-SL35-019 — Hero create: Hearthstone 6948 in bag; homebind = race starter (Northshire).
     */
    @Test
    void tpSl35CreateShouldGrantHearthstoneBoundToStarterZone() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Hearthero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        assertNotNull(created);
        assertTrue(created.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_HEARTHSTONE));
        assertEquals(0, created.bindMap);
        assertEquals(12, created.bindZone);
        assertEquals(created.x, created.bindX, 0.01f);
        assertEquals(created.y, created.bindY, 0.01f);
        assertEquals(created.z, created.bindZ, 0.01f);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_HEARTHSTONE));
        assertEquals(p.mapId, p.bindMap);
        assertEquals(p.zoneId, p.bindZone);
    }

    /**
     * TP-SL35-015 — Hero create: full armor/weapon proficiency masks; combat skills at 1.
     */
    @Test
    void tpSl35CreateShouldGrantFullProficienciesAndWeaponSkillsAtOne() {
        World world = World.inMemory();
        Player created = world.characters.create(ACC.id(), "Fullskill", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        assertNotNull(created);
        assertEquals(ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK,
                created.armorProficiency() & ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK);
        assertEquals(ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK,
                created.weaponProficiency() & ClasslessConfig.ALL_WEAPON_PROFICIENCY_MASK);
        assertEquals(1, created.skillValue(org.tbc.world.content.WeaponSkills.SKILL_SWORDS));
        assertEquals(1, created.skillValue(org.tbc.world.content.WeaponSkills.SKILL_AXES));
        assertEquals(1, created.skillValue(org.tbc.world.content.WeaponSkills.SKILL_UNARMED));
        assertEquals(1, created.skillValue(org.tbc.world.content.WeaponSkills.SKILL_DEFENSE));
        assertEquals(5, created.skillMax(org.tbc.world.content.WeaponSkills.SKILL_SWORDS));
        assertTrue(created.spells.contains(ClasslessConfig.AUTO_ATTACK));
        assertFalse(created.spells.contains(78), "no Heroic Strike");
        assertFalse(created.spells.contains(Content.SPELL_BATTLE_SHOUT), "no free class abilities");
        assertEquals(0, org.tbc.world.classless.ClasslessTrainerPolicy.classSpellsLearned(created));
    }

    /** TP-SL35-007 — create-self exposes mana + rage + energy MAXPOWER. */
    @Test
    void tpSl35CreateSelfShouldExposeManaRageEnergy() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Tripower", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertEquals(Player.POWER_MANA, p.powerType);
        assertTrue(p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1) > 0, "mana");
        assertEquals(Player.POWER_RAGE_MAX, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER2));
        assertEquals(Player.POWER_ENERGY_MAX, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        int bytes0 = p.getInt(UpdateFields.UNIT_FIELD_BYTES_0);
        assertEquals(ClasslessConfig.CLASS_CLASSLESS, (bytes0 >>> 8) & 0xFF);
        assertEquals(Player.POWER_MANA, (bytes0 >>> 24) & 0xFF);
    }

    /** TP-SL35-003 — relog keeps class 6 + learned trainer spell. */
    @Test
    void tpSl35RelogShouldKeepClassAndLearnedSpell() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Relogless", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        unlockBattleShout(p);
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT));
        long guid = p.guid;
        client.session().logout(world, true);

        WowClientDouble again = new WowClientDouble();
        again.connect(ACC);
        again.login(world, guid);
        Player p2 = again.session().player();
        assertEquals(ClasslessConfig.CLASS_CLASSLESS, p2.clazz);
        assertTrue(p2.spells.contains(ClasslessConfig.AUTO_ATTACK));
        assertTrue(p2.spells.contains(Content.SPELL_BATTLE_SHOUT));
    }

    /** TP-SL35-004 — mage trainer after unlock; warrior trainer after unlock; warrior class unchanged. */
    @Test
    void tpSl35TrainerBuyEligibleAbility() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Trainless", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature warriorTrainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(warriorTrainer);
        p.relocate(warriorTrainer.x, warriorTrainer.y, warriorTrainer.z, warriorTrainer.o);
        p.setMoney(5_000);
        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(warriorTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST), "Hero warrior trainer locked until unlock quest");
        WowBuffer buyLocked = new WowBuffer(12);
        buyLocked.putU64(warriorTrainer.guid);
        buyLocked.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertFalse(p.spells.contains(Content.SPELL_BATTLE_SHOUT));

        unlockWarrior(p);
        client.clear();
        WowBuffer list = new WowBuffer(8);
        list.putU64(warriorTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        WowBuffer llaneList = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_LIST));
        assertEquals(warriorTrainer.guid, llaneList.getU64());
        llaneList.getU32();
        int llaneCount = llaneList.getU32();
        assertTrue(llaneCount >= 1, "classless must see Llane spells");
        boolean sawBattleShout = false;
        for (int i = 0; i < llaneCount; i++) {
            int spell = llaneList.getU32();
            llaneList.getU8();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU8();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            if (spell == Content.SPELL_BATTLE_SHOUT) {
                sawBattleShout = true;
            }
        }
        assertFalse(sawBattleShout, "Battle Shout hidden until follow-up 90002");

        unlockBattleShout(p);
        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT), "90002 teaches Battle Shout");

        // Rank2 needs prev 6673 + level 12 (spell_chain.prev kept for classless).
        client.clear();
        WowBuffer buyR2Early = new WowBuffer(12);
        buyR2Early.putU64(warriorTrainer.guid);
        buyR2Early.putU32(Content.SPELL_BATTLE_SHOUT_RANK2);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyR2Early.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED), "rank2 red under level");
        assertFalse(p.spells.contains(Content.SPELL_BATTLE_SHOUT_RANK2));
        p.level = 12;
        p.setMoney(Math.max(p.money, 2_000));
        client.clear();
        WowBuffer buyR2 = new WowBuffer(12);
        buyR2.putU64(warriorTrainer.guid);
        buyR2.putU32(Content.SPELL_BATTLE_SHOUT_RANK2);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyR2.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT_RANK2));

        Creature mageTrainer = find(world, Content.NPC_KHELDEN_BREMEN);
        assertNotNull(mageTrainer);
        p.relocate(mageTrainer.x, mageTrainer.y, mageTrainer.z, mageTrainer.o);
        client.clear();
        WowBuffer mageListLocked = new WowBuffer(8);
        mageListLocked.putU64(mageTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, mageListLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST), "Hero mage trainer locked until unlock quest");
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        client.clear();
        WowBuffer mageList = new WowBuffer(8);
        mageList.putU64(mageTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, mageList.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST), "classless opens mage trainer after unlock");
        WowBuffer magePayload = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_LIST));
        assertEquals(mageTrainer.guid, magePayload.getU64());
        magePayload.getU32();
        int mageCount = magePayload.getU32();
        assertTrue(mageCount >= 1, "classless must see mage trainer spells");
        boolean sawFireball = false;
        for (int i = 0; i < mageCount; i++) {
            int spell = magePayload.getU32();
            magePayload.getU8();
            magePayload.getU32();
            magePayload.getU32();
            magePayload.getU32();
            magePayload.getU8();
            magePayload.getU32();
            magePayload.getU32();
            magePayload.getU32();
            magePayload.getU32();
            magePayload.getU32();
            if (spell == Content.SPELL_FIREBALL) {
                sawFireball = true;
            }
        }
        assertTrue(sawFireball, "Khelden list must include Fireball 133");
        client.clear();
        WowBuffer buyFb = new WowBuffer(12);
        buyFb.putU64(mageTrainer.guid);
        buyFb.putU32(Content.SPELL_FIREBALL);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyFb.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_FIREBALL));
    }

    @Test
    void tpSl35WarriorCreateAndTrainUnchanged() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Stillwar", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertEquals(1, p.clazz);
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(Content.TRAINER_SPELL_BATTLE_SHOUT_COST);
        client.clear();
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));

        Creature mageTrainer = find(world, Content.NPC_KHELDEN_BREMEN);
        assertNotNull(mageTrainer);
        p.relocate(mageTrainer.x, mageTrainer.y, mageTrainer.z, mageTrainer.o);
        client.clear();
        WowBuffer mageList = new WowBuffer(8);
        mageList.putU64(mageTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, mageList.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST), "warrior cannot open mage trainer");
    }

    /** TP-SL35-005 — unproficient leather/mail/plate apply per-step reductions. */
    @Test
    void tpSl35UnproficientArmorShouldApplyPerStepPenalty() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Armorp", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        // Penalty oracle needs cloth-only; create now grants full armor proficiency.
        p.clearArmorProficiency(ClasslessConfig.ALL_ARMOR_PROFICIENCY_MASK & ~ClasslessConfig.ARMOR_CLOTH_MASK);
        stripNonWeaponGear(p);
        world.objectMgr.applyEquippedMelee(p);
        int baseStr = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int baseAgi = p.getInt(UpdateFields.UNIT_FIELD_STAT1);

        equipChest(world, p, client, Content.ITEM_TUNIC_OF_WESTFALL);
        // Leather step 1: armor −10%, Str/Agi −10%. Tunic: armor 92, AGI 11.
        // RESISTANCES = (createAgi + itemAgi) * 2 + itemArmor.
        int leatherAgi = Math.round(11 * 0.9f);
        int leatherArmor = Math.round(92 * 0.9f);
        assertEquals(baseAgi + leatherAgi, p.getInt(UpdateFields.UNIT_FIELD_STAT1));
        assertEquals((baseAgi + leatherAgi) * 2 + leatherArmor, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-5f, p.equipmentSpeedPenaltyPct(), 0.01f);

        unequipChest(world, p, client);
        world.objectMgr.applyEquippedMelee(p);
        assertEquals(baseAgi * 2, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0f, p.equipmentSpeedPenaltyPct(), 0.01f);

        equipChest(world, p, client, Content.ITEM_BRACKWATER_VEST);
        // Mail step 2: −20% armor/str, −10% speed. Brackwater armor 162 STR 4.
        int mailStr = Math.round(4 * 0.8f);
        int mailArmor = Math.round(162 * 0.8f);
        assertEquals(baseStr + mailStr, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(baseAgi * 2 + mailArmor, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-10f, p.equipmentSpeedPenaltyPct(), 0.01f);

        unequipChest(world, p, client);
        equipChest(world, p, client, Content.ITEM_LIGHTFORGE_BREASTPLATE);
        // Plate step 3: −30% armor/str, −15% speed. Lightforge armor 657 STR 13.
        int plateStr = Math.round(13 * 0.7f);
        int plateArmor = Math.round(657 * 0.7f);
        assertEquals(baseStr + plateStr, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(baseAgi * 2 + plateArmor, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-15f, p.equipmentSpeedPenaltyPct(), 0.01f);

        unequipChest(world, p, client);
        equipChest(world, p, client, Content.ITEM_SEERS_ROBE);
        // Cloth proficient: full armor 35, no speed penalty.
        assertEquals(baseAgi * 2 + 35, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0f, p.equipmentSpeedPenaltyPct(), 0.01f);
    }

    /** TP-SL35-006 — untrained weapon still swings; miss rate ≥ proficient baseline. */
    @Test
    void tpSl35UntrainedWeaponShouldStillSwingWithMissPenalty() {
        World world = World.inMemory();
        Player p = world.characters.create(ACC.id(), "Swingless", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        assertNotNull(p);
        Creature target = new Creature();
        target.guid = 0xF130000000000001L;
        target.level = p.level;
        Item main = p.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND);
        assertNotNull(main, "starter sword equipped");
        // Create grants sword bit; clear it to exercise untrained miss path.
        p.clearWeaponProficiency(1 << 7);
        double untrainedAdd = org.tbc.world.classless.WeaponPenaltyPolicy.missAddPercent(p, false);
        assertEquals(ClasslessConfig.get().untrainedWeaponMissAddPct(), untrainedAdd, 1e-9);
        p.addWeaponProficiency(1 << 7); // 1H swords
        assertEquals(0.0, org.tbc.world.classless.WeaponPenaltyPolicy.missAddPercent(p, false), 1e-9);
        MeleeTable.Result hit = MeleeTable.alwaysHit().rollOne(p, target, 2, 4);
        assertTrue(hit.damage() > 0);
    }

    /**
     * TP-SL35-010 — classless create sets spirit regen rates (mage GT proxy); OOC HP+mana rise;
     * in combat neither spirit HP nor spirit mana (INTERRUPT rate 0 without Meditation).
     */
    @Test
    void tpSl35ClasslessOocRegenShouldRaiseHealthAndMana() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Regenhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setHealth(10);
        p.setPower(50);
        client.clear();

        world.tick(2000);

        // spi 20 * mage gtOCTRegenHP L1 0.079365 * 2s → +3; sqrt(20)*20*0.034965*2s → +6
        assertEquals(13, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(56, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(0f, p.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 0.001f);

        p.setHealth(10);
        p.setPower(50);
        p.inCombat = true;
        client.clear();
        world.tick(2000);
        assertEquals(10, p.health(), "no spirit HP in combat");
        assertEquals(50, p.power(), "combat mana uses interrupt rate (0 without Meditation)");
    }

    /**
     * TP-SL35-013 — HeroPowerBars enable via LANG_ADDON → AddonEnabled + PowerUpdate for mana/rage/energy.
     */
    @Test
    void tpSl35HeroPowerAddonEnableShouldPushAddonEnabledAndPowerUpdates() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Addonhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();

        sendHeroPowerEnable(client, world);
        List<String> chats = messageChatBodies(client);
        assertTrue(chats.stream().anyMatch(m -> m.equals("HeroPowerBars\tAddonEnabled")),
                "AddonEnabled: " + chats);
        assertTrue(chats.stream().anyMatch(m -> m.startsWith("HeroPowerBars\tPowerUpdate#0;")), "mana update");
        assertTrue(chats.stream().anyMatch(m -> m.matches("HeroPowerBars\\tPowerUpdate#1;\\d+;100")),
                "rage display max 100: " + chats);
        assertTrue(chats.stream().anyMatch(m -> m.matches("HeroPowerBars\\tPowerUpdate#3;\\d+;100")),
                "energy max 100: " + chats);
    }

    /**
     * TP-SL35-025 — after a ding, VALUES must carry max/current mana, rage, and energy, and
     * HeroPowerBars must get PowerUpdate#0/#1/#3 so the triple bars refill without a relog.
     */
    @Test
    void tpSl35HeroDingShouldPushRageEnergyManaValuesAndAddon() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Dinghero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        created.xp = 350;
        world.characters.save(created);
        client.login(world, created.guid);
        sendHeroPowerEnable(client, world);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        client.clear();
        int n = 0;
        while (c.alive() && n++ < 400) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        assertTrue(client.saw(Opcodes.SMSG_LEVELUP_INFO));
        assertEquals(2, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_LEVEL));
        assertTrue(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER1) > 0);
        assertEquals(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER1),
                client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(Player.POWER_RAGE_MAX, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER2));
        assertEquals(Player.POWER_ENERGY_MAX, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(Player.POWER_ENERGY_MAX, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER4));
        List<String> chats = messageChatBodies(client);
        assertTrue(chats.stream().anyMatch(m -> m.startsWith("HeroPowerBars\tPowerUpdate#0;")),
                "mana addon after ding: " + chats);
        assertTrue(chats.stream().anyMatch(m -> m.startsWith("HeroPowerBars\tPowerUpdate#1;")),
                "rage addon after ding: " + chats);
        assertTrue(chats.stream().anyMatch(m -> m.startsWith("HeroPowerBars\tPowerUpdate#3;")),
                "energy addon after ding: " + chats);
        int unspent = p.heroStats.unspent();
        assertTrue(unspent > 0);
        assertTrue(chats.stream().anyMatch(m -> m.equals(
                "HeroPowerBars\tStatUpdate;" + unspent + ";0;0;0;0;0")),
                "StatUpdate must follow ding without relog: " + chats);
    }

    /** TP-SL35-026 — ding does not auto-apply STAT0-4; SMSG_LEVELUP_INFO stat deltas are 0. */
    @Test
    void tpSl35HeroDingShouldKeepL1StatsAndZeroLevelupStatDeltas() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Statding", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        created.xp = 350;
        world.characters.save(created);
        client.login(world, created.guid);
        Player p = client.session().player();
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        client.clear();
        int n = 0;
        while (c.alive() && n++ < 400) {
            world.meleeHit(p, c);
        }
        assertFalse(c.alive());
        assertTrue(client.saw(Opcodes.SMSG_LEVELUP_INFO));
        byte[] info = client.payload(Opcodes.SMSG_LEVELUP_INFO);
        assertEquals(2, WowClientDouble.u32le(info, 0));
        for (int i = 1; i <= 11; i++) {
            assertEquals(0, WowClientDouble.u32le(info, i * 4), "LEVELUP_INFO dword " + i);
        }
        assertEquals(str, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        assertTrue(p.heroStats.unspent() > 0);
    }

    /** TP-SL35-029 — SpendStat raises STAT0 and StatUpdate; warriors are ignored. */
    @Test
    void tpSl35HeroSpendStatShouldPushStatUpdateAndValues() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Spendhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        created.xp = 350;
        world.characters.save(created);
        client.login(world, created.guid);
        sendHeroPowerEnable(client, world);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.attackSwing(world, c.guid);
        int n = 0;
        while (c.alive() && n++ < 400) {
            world.meleeHit(p, c);
        }
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int unspent = p.heroStats.unspent();
        client.clear();
        sendHeroSpend(client, world, 0, 1);
        assertEquals(str + 1, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        List<String> chats = messageChatBodies(client);
        assertTrue(chats.stream().anyMatch(m -> m.equals(
                "HeroPowerBars\tStatUpdate;" + (unspent - 1) + ";1;0;0;0;0")), chats.toString());
    }

    @Test
    void tpSl35WarriorSpendStatShouldNotChangeStats() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Spendwar", 1, 1,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        client.clear();
        sendHeroSpend(client, world, 0, 1);
        assertEquals(str, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        List<String> chats = messageChatBodies(client);
        assertFalse(chats.stream().anyMatch(m -> m.contains("StatUpdate")), chats.toString());
    }

    @Test
    void tpSl35HeroPowerAddonEnableWhenWarriorShouldNotEnable() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Addonwar", 1, 1,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        sendHeroPowerEnable(client, world);
        List<String> chats = messageChatBodies(client);
        assertFalse(chats.stream().anyMatch(m -> m.contains("AddonEnabled")), chats.toString());
    }

    /**
     * Spirit healer revive for classless Hero — living again with 50% HP/mana/energy on the wire
     * (issues.md / same contract as TP-SL17-011).
     */
    @Test
    void tpSl35HeroSpiritHealerReviveShouldRestoreLivingHpManaEnergy() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), "Revivehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 0);
        p.setInt(UpdateFields.UNIT_FIELD_POWER2, 500);
        p.setInt(UpdateFields.UNIT_FIELD_POWER4, 0);
        int entry = 6491;
        world.objectMgr.creatures.put(entry, new ObjectMgr.CreatureTemplate(
                entry, "Spirit Healer", 0, 35, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_SPIRITHEALER, "", "", 0));
        Creature healer = world.objectMgr.spawnCreature(entry, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(healer);
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        healer.relocate(p.x, p.y, p.z, p.o);
        client.clear();
        client.gossipHello(world, healer.guid);
        client.gossipSelect(world, healer.guid, 0, 0);
        assertTrue(client.saw(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM));
        client.clear();
        WowBuffer activate = new WowBuffer(8);
        activate.putU64(healer.guid);
        client.handle(world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, activate.array());
        assertFalse(p.ghost);
        assertEquals(p.maxHealth() / 2, p.health());
        assertEquals(p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1) / 2, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_POWER2));
        assertEquals(p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4) / 2, p.getInt(UpdateFields.UNIT_FIELD_POWER4));
        assertEquals(p.maxHealth() / 2, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
    }

    /**
     * TP-SL35-016 — Hero trainer cost 100×2^learned (ignore row spellCost): 100 then 200 refuse/buy; list U32 400.
     */
    @Test
    void tpSl35CumulativeTrainerCostShouldScaleWithLearnedSpells() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Costhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        unlockBattleShout(p);
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        Creature mage = find(world, Content.NPC_KHELDEN_BREMEN);
        assertNotNull(mage);

        // 90002 taught Battle Shout (learned 1) → Rank2 costs 200; short money refuses.
        p.level = 12;
        p.setMoney(199);
        client.clear();
        WowBuffer buyR2 = new WowBuffer(12);
        buyR2.putU64(trainer.guid);
        buyR2.putU32(Content.SPELL_BATTLE_SHOUT_RANK2);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyR2.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertFalse(p.spells.contains(Content.SPELL_BATTLE_SHOUT_RANK2));
        assertEquals(199, p.money);

        p.setMoney(200);
        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyR2.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT_RANK2));
        assertEquals(0, p.money);

        // learned 2 → list Fireball U32 400 (ignore Fireball row spellCost).
        p.relocate(mage.x, mage.y, mage.z, mage.o);
        client.clear();
        WowBuffer list = new WowBuffer(8);
        list.putU64(mage.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        WowBuffer payload = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_LIST));
        payload.getU64();
        payload.getU32();
        int count = payload.getU32();
        int fireballCost = -1;
        for (int i = 0; i < count; i++) {
            int spell = payload.getU32();
            payload.getU8();
            int cost = payload.getU32();
            payload.getU32();
            payload.getU32();
            payload.getU8();
            payload.getU32();
            payload.getU32();
            payload.getU32();
            payload.getU32();
            payload.getU32();
            if (spell == Content.SPELL_FIREBALL) {
                fireballCost = cost;
            }
        }
        assertEquals(400, fireballCost);
    }

    /**
     * TP-SL35-017 — Hero melee hit raises UNIT_FIELD_POWER2 (warrior formula); mana unchanged.
     */
    @Test
    void tpSl35MeleeHitShouldRaiseRageNotMana() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Ragehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        int manaBefore = p.power();
        assertEquals(0, p.rage());
        client.clear();
        WowBuffer atk = new WowBuffer(8);
        atk.putU64(c.guid);
        client.handle(world, Opcodes.CMSG_ATTACKSWING, atk.array());
        client.session().tick(world, 0);
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertTrue(p.rage() > 0, "Hero melee should gain rage on POWER2");
        assertEquals(manaBefore, p.power(), "mana must not change from melee rage");
        assertEquals(p.rage(), client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER2));
    }

    /**
     * TP-SL35-018 — Battle Shout 6673 costs rage from POWER2; mana unchanged.
     */
    @Test
    void tpSl35BattleShoutShouldSpendRageNotMana() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Shouthero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.spells.add(Content.SPELL_BATTLE_SHOUT);
        p.setRage(100);
        int manaBefore = p.power();
        client.clear();
        client.castSpell(world, Content.SPELL_BATTLE_SHOUT, 1, p.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO), "Battle Shout should cast");
        assertEquals(90, p.rage(), "rage cost 10 from POWER2");
        assertEquals(manaBefore, p.power(), "mana unchanged");
        assertEquals(90, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER2));
    }

    @Test
    void tpSl35BattleShoutWhenLowRageShouldFailNoPower() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Norager", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.spells.add(Content.SPELL_BATTLE_SHOUT);
        p.setRage(5);
        int manaBefore = p.power();
        client.clear();
        client.castSpell(world, Content.SPELL_BATTLE_SHOUT, 1, p.guid);
        assertTrue(client.saw(Opcodes.SMSG_CAST_RESULT));
        byte[] fail = client.payload(Opcodes.SMSG_CAST_RESULT);
        assertEquals(Content.SPELL_BATTLE_SHOUT, WowClientDouble.u32le(fail, 0));
        assertEquals(org.tbc.world.spell.SpellEngine.SPELL_FAILED_NO_POWER, fail[4] & 0xFF);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(5, p.rage());
        assertEquals(manaBefore, p.power());
    }

    /**
     * TP-SL35-020 — Hero fireball in plate: school damage 1/8; Llane lists Battlecaster ranks.
     */
    @Test
    void tpSl35PlateShouldHalveCasterDamageEightfold() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Casterarm", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.spells.add(Content.SPELL_FIREBALL);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 500);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 500);
        equipChest(world, p, client, Content.ITEM_LIGHTFORGE_BREASTPLATE);
        assertEquals(3, org.tbc.world.classless.CasterArmorPolicy.heaviestArmorStep(p, world.objectMgr));

        unlockWarrior(p);
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        client.clear();
        WowBuffer list = new WowBuffer(8);
        list.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        WowBuffer llaneList = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_LIST));
        llaneList.getU64();
        llaneList.getU32();
        int count = llaneList.getU32();
        boolean sawLeather = false;
        boolean sawMail = false;
        boolean sawPlate = false;
        for (int i = 0; i < count; i++) {
            int spell = llaneList.getU32();
            llaneList.getU8();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU8();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            llaneList.getU32();
            if (spell == org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_LEATHER) {
                sawLeather = true;
            }
            if (spell == org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_MAIL) {
                sawMail = true;
            }
            if (spell == org.tbc.world.classless.CasterArmorPolicy.SPELL_BATTLECASTER_PLATE) {
                sawPlate = true;
            }
        }
        assertTrue(sawLeather && sawMail && sawPlate, "Llane must list Battlecaster ranks");

        Creature kobold = null;
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == 6) {
                kobold = c;
                break;
            }
        }
        assertNotNull(kobold);
        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        int hpBefore = kobold.health();
        client.clear();
        client.castSpell(world, Content.SPELL_FIREBALL, 1, kobold.guid);
        world.tick(1_500);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(hpBefore - 1, kobold.health(), "plate → 10×1/8 → 1 damage");
    }

    private static void sendHeroSpend(WowClientDouble client, World world, int stat, int amount) {
        WowBuffer b = new WowBuffer(64);
        b.putU32(0x01);
        b.putU32(0xFFFFFFFF);
        b.putCString("HeroPowerBars\tSpendStat;" + stat + ";" + amount);
        client.handle(world, Opcodes.CMSG_MESSAGECHAT, b.array());
    }

    private static void sendHeroPowerEnable(WowClientDouble client, World world) {
        WowBuffer b = new WowBuffer(48);
        b.putU32(0x01); // say
        b.putU32(0xFFFFFFFF); // LANG_ADDON
        b.putCString("HeroPowerBars\tenable");
        client.handle(world, Opcodes.CMSG_MESSAGECHAT, b.array());
    }

    private static List<String> messageChatBodies(WowClientDouble client) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_MESSAGECHAT) {
                continue;
            }
            WowBuffer buf = new WowBuffer(client.payloads.get(i));
            buf.getU8();
            buf.getU32();
            buf.getU64();
            buf.getU32();
            buf.getU64();
            buf.getU32();
            out.add(buf.getCString());
        }
        return out;
    }

    private static final int EQUIPMENT_SLOT_CHEST = 4;

    /** Drop Recruit shirt/pants/boots so armor-penalty asserts see only the test chest. */
    private static void stripNonWeaponGear(Player p) {
        for (int slot : new int[]{3, 6, 7}) {
            Item it = p.itemAt(0, slot);
            if (it == null) {
                continue;
            }
            p.items.remove((int) it.guid);
            p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + slot * 2, 0);
        }
    }

    private static void equipChest(World world, Player p, WowClientDouble client, int itemId) {
        unequipChest(world, p, client);
        int src = p.firstFreeBagSlot();
        Item it = new Item(world.nextItemGuid(), itemId);
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(itemId);
        assertNotNull(t);
        it.itemClass = t.itemClass;
        it.subClass = t.subClass;
        it.slot = src;
        p.items.put((int) it.guid, it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + src * 2, UpdateBuilder.itemGuid(it));
        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        world.objectMgr.applyEquippedMelee(p);
    }

    private static void unequipChest(World world, Player p, WowClientDouble client) {
        Item chest = p.itemAt(0, EQUIPMENT_SLOT_CHEST);
        if (chest == null) {
            return;
        }
        int dst = p.firstFreeBagSlot();
        if (dst < 0) {
            return;
        }
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + EQUIPMENT_SLOT_CHEST * 2, 0);
        chest.slot = dst;
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2, UpdateBuilder.itemGuid(chest));
        world.objectMgr.applyEquippedMelee(p);
    }

    /**
     * TP-SL35-021 — only Hero accepts Warrior unlock quest; Warrior class does not.
     */
    @Test
    void tpSl35HeroOnlyWarriorUnlockQuest() {
        World world = World.inMemory();
        WowClientDouble heroClient = new WowClientDouble();
        heroClient.connect(ACC);
        Player heroCreated = world.characters.create(ACC.id(), "Heroquest", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        heroClient.login(world, heroCreated.guid);
        Player hero = heroClient.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        assertNotNull(trainer);
        hero.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        heroClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON, hero.questLogId[0]);

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(2, "WAR", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warquest", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /**
     * TP-SL35-022 — 5 successful melee hits on 15274 advance the hit objective; spells do not.
     */
    @Test
    void tpSl35MeleeHitsShouldAdvanceWarriorUnlockObjective() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Hitquest", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());

        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        wyrm.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50_000);
        wyrm.setHealth(50_000);
        world.map(p.mapId, p.instanceId).add(wyrm);

        p.spells.add(Content.SPELL_FIREBALL);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 500);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 500);
        client.clear();
        client.castSpell(world, Content.SPELL_FIREBALL, 1, wyrm.guid);
        assertEquals(0, p.questLogCounts[0][1], "non-melee must not count as weapon hits");

        for (int i = 1; i <= 5; i++) {
            client.clear();
            swingOnce(world, client, p, wyrm);
            assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
            WowBuffer add = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
            assertEquals(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON, add.getU32());
            assertEquals(HeroClassUnlock.CREATURE_MANA_WYRM, add.getU32());
            assertEquals(i, add.getU32());
            assertEquals(HeroClassUnlock.REQUIRED_HITS, add.getU32());
        }
        client.clear();
        swingOnce(world, client, p, wyrm);
        assertEquals(5, p.questLogCounts[0][1]);
    }

    /**
     * TP-SL35-023 — kill + alive turn-in learns Heroic Strike 78 and persists on relog.
     */
    @Test
    void tpSl35WarriorUnlockTurnInShouldLearnHeroicStrike() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Turnhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        p.setHealth(0);
        client.clear();
        WowBuffer deadChoose = new WowBuffer(16);
        deadChoose.putU64(trainer.guid);
        deadChoose.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        deadChoose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, deadChoose.array());
        assertFalse(p.spells.contains(org.tbc.world.spell.SpellEngine.HEROIC_STRIKE));
        assertFalse(p.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));

        p.setHealth(p.maxHealth());
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.HEROIC_STRIKE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.HEROIC_STRIKE));
        assertTrue(p.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));

        long guid = p.guid;
        client.session().logout(world, true);
        WowClientDouble again = new WowClientDouble();
        again.connect(ACC);
        again.login(world, guid);
        Player p2 = again.session().player();
        assertTrue(p2.spells.contains(org.tbc.world.spell.SpellEngine.HEROIC_STRIKE));
        assertTrue(p2.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
    }

    /**
     * TP-SL35-024 — warrior trainer list/buy refused until unlock; Battle Shout after.
     */
    @Test
    void tpSl35WarriorTrainerShouldGateOnUnlockQuest() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Gatehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(ClasslessConfig.STARTING_MONEY_COPPER);
        client.clear();
        WowBuffer list = new WowBuffer(8);
        list.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
        assertFalse(p.spells.contains(Content.SPELL_BATTLE_SHOUT));

        p.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED), "6673 gated on 90002");
        assertFalse(p.spells.contains(Content.SPELL_BATTLE_SHOUT));
    }

    /**
     * TP-SL35-031 — Rally the Line (90002) teaches Battle Shout; warrior refused; trainer lists 6673 after.
     */
    @Test
    void tpSl35RallyTheLineShouldTeachBattleShout() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rallyhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        WowBuffer acceptEarly = new WowBuffer(12);
        acceptEarly.putU64(trainer.guid);
        acceptEarly.putU32(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, acceptEarly.array());
        assertEquals(0, p.questLogId[0], "90002 requires prev 90001");

        unlockWarrior(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_RALLY_THE_LINE, p.questLogId[0]);

        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(0, p.questLogState[0], "90002 also requires /roar near the warrior trainer");

        p.relocate(trainer.x + 40f, trainer.y, trainer.z, trainer.o);
        client.clear();
        client.handle(world, Opcodes.CMSG_TEXT_EMOTE, textEmote(HeroClassUnlock.TEXT_EMOTE_ROAR, 0L).array());
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO), "roar out of range does not rally");
        assertEquals(0, p.questLogState[0]);

        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        client.clear();
        client.handle(world, Opcodes.CMSG_TEXT_EMOTE, textEmote(1, trainer.guid).array());
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO), "wave is not the rally roar");
        assertEquals(0, p.questLogState[0]);

        client.clear();
        client.handle(world, Opcodes.CMSG_TEXT_EMOTE, textEmote(HeroClassUnlock.TEXT_EMOTE_ROAR, trainer.guid).array());
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        byte[] go = client.payload(Opcodes.SMSG_SPELL_GO);
        assertEquals(trainer.guid, packedGuid(go, 0), "warrior trainer casts the shout");
        assertEquals(Content.SPELL_BATTLE_SHOUT, spellGoId(go));
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == Content.SPELL_BATTLE_SHOUT));
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(Content.SPELL_BATTLE_SHOUT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT));
        assertTrue(p.rewardedQuests.contains(HeroClassUnlock.QUEST_RALLY_THE_LINE));

        long guid = p.guid;
        client.session().logout(world, true);
        WowClientDouble again = new WowClientDouble();
        again.connect(ACC);
        again.login(world, guid);
        Player p2 = again.session().player();
        assertTrue(p2.spells.contains(Content.SPELL_BATTLE_SHOUT));
        assertTrue(p2.rewardedQuests.contains(HeroClassUnlock.QUEST_RALLY_THE_LINE));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(3, "WAR2", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warrally", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /**
     * TP-SL35-032 — Close the Distance (90003): 3 melee hits, teach Charge 100.
     */
    @Test
    void tpSl35CloseTheDistanceShouldTeachCharge() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Chargehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockWarrior(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE, p.questLogId[0]);

        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        wyrm.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50_000);
        wyrm.setHealth(50_000);
        world.map(p.mapId, p.instanceId).add(wyrm);
        for (int i = 0; i < HeroClassUnlock.FOLLOWUP_CHARGE_HITS; i++) {
            swingOnce(world, client, p, wyrm);
        }
        assertEquals(HeroClassUnlock.FOLLOWUP_CHARGE_HITS, p.questLogCounts[0][1]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_CLOSE_THE_DISTANCE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_CHARGE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_CHARGE));
    }

    /**
     * TP-SL35-033 — A Wound to Remember (90004): collect Training Strip, teach Rend 772.
     */
    @Test
    void tpSl35WoundToRememberShouldTeachRend() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rendhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockWarrior(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_TRAINING_STRIP, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_TRAINING_STRIP, 1, client.session()::send);
        assertEquals(1, p.questLogItemCount[0][0]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_WOUND_TO_REMEMBER);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_REND,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_REND));
    }

    /**
     * TP-SL35-034 — Paladin unlock 90005 teaches Seal of Righteousness; trainer gated; warrior refused.
     */
    @Test
    void tpSl35VowTestedShouldTeachSealAndGatePaladinTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Vowhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PALADIN_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_VOW_TESTED);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_A_VOW_TESTED, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_PROTECTIVE_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_PROTECTIVE_TOKEN, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        p.setHealth(0);
        client.clear();
        WowBuffer dead = new WowBuffer(16);
        dead.putU64(trainer.guid);
        dead.putU32(HeroClassUnlock.QUEST_A_VOW_TESTED);
        dead.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, dead.array());
        assertFalse(p.spells.contains(org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS));

        p.setHealth(p.maxHealth());
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_VOW_TESTED);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        assertTrue(p.rewardedQuests.contains(HeroClassUnlock.QUEST_A_VOW_TESTED));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        WowBuffer list = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_LIST));
        list.getU64();
        list.getU32();
        int count = list.getU32();
        boolean sawAura = false;
        for (int i = 0; i < count; i++) {
            int spell = list.getU32();
            list.getU8();
            list.getU32();
            list.getU32();
            list.getU32();
            list.getU8();
            list.getU32();
            list.getU32();
            list.getU32();
            list.getU32();
            list.getU32();
            if (spell == org.tbc.world.spell.SpellEngine.DEVOTION_AURA) {
                sawAura = true;
            }
        }
        assertFalse(sawAura, "Devotion Aura gated on 90006");

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(4, "WAR3", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warvow", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_A_VOW_TESTED);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-035 — Stand Fast 90006 teaches Devotion Aura 465. */
    @Test
    void tpSl35StandFastShouldTeachDevotionAura() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Standhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PALADIN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPaladin(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_STAND_FAST);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_STAND_FAST);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.DEVOTION_AURA,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.DEVOTION_AURA));
    }

    /** TP-SL35-036 — Strength in Service 90007 teaches Blessing of Might 19740. */
    @Test
    void tpSl35StrengthInServiceShouldTeachBlessingOfMight() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Mighthero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PALADIN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPaladin(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_STRENGTH_IN_SERVICE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_BLESSING_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_BLESSING_TOKEN, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_STRENGTH_IN_SERVICE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_BLESSING_OF_MIGHT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_BLESSING_OF_MIGHT));
    }

    /** TP-SL35-037 — Mercy's Lesson 90008 teaches Holy Light 635. */
    @Test
    void tpSl35MercysLessonShouldTeachHolyLight() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Mercyhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PALADIN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPaladin(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_MERCYS_LESSON);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_HEALING_KIT, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_HEALING_KIT, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_MERCYS_LESSON);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.HOLY_LIGHT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.HOLY_LIGHT));
    }

    /**
     * TP-SL35-038 — Hunter unlock 90009 teaches Hunter's Mark; trainer gated; warrior refused.
     */
    @Test
    void tpSl35MarkedTrailShouldTeachHuntersMarkAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Markhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_HUNTER_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_THE_MARKED_TRAIL);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_THE_MARKED_TRAIL, p.questLogId[0]);

        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        wyrm.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50_000);
        wyrm.setHealth(50_000);
        world.map(p.mapId, p.instanceId).add(wyrm);
        for (int i = 0; i < HeroClassUnlock.FOLLOWUP_MARKED_HITS; i++) {
            swingOnce(world, client, p, wyrm);
        }
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_THE_MARKED_TRAIL);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.HUNTERS_MARK,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.HUNTERS_MARK));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        // Warrior/Paladin unlocks remain independent.
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(5, "WAR4", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warmark", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_THE_MARKED_TRAIL);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-039 — Steady Aim 90010 teaches Auto Shot 75. */
    @Test
    void tpSl35SteadyAimShouldTeachAutoShot() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Steadyhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_HUNTER_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockHunter(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_STEADY_AIM);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_STEADY_AIM);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_AUTO_SHOT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_AUTO_SHOT));
    }

    /** TP-SL35-040 — Venom in the Field 90011 teaches Serpent Sting 1978. */
    @Test
    void tpSl35VenomInTheFieldShouldTeachSerpentSting() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Venomhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_HUNTER_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockHunter(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_VENOM_IN_THE_FIELD);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_VENOM_SAMPLE, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_VENOM_SAMPLE, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_VENOM_IN_THE_FIELD);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SERPENT_STING,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SERPENT_STING));
    }

    /** TP-SL35-041 — A Clean Shot 90012 teaches Arcane Shot 3044. */
    @Test
    void tpSl35CleanShotShouldTeachArcaneShot() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Cleanhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_HUNTER_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockHunter(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_CLEAN_SHOT);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        wyrm.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50_000);
        wyrm.setHealth(50_000);
        world.map(p.mapId, p.instanceId).add(wyrm);
        for (int i = 0; i < HeroClassUnlock.FOLLOWUP_CLEAN_SHOT_HITS; i++) {
            swingOnce(world, client, p, wyrm);
        }
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_CLEAN_SHOT);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_ARCANE_SHOT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_ARCANE_SHOT));
    }

    private static void unlockWarrior(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
    }

    private static void unlockBattleShout(Player p) {
        unlockWarrior(p);
        p.rewardedQuests.add(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        if (!p.spells.contains(Content.SPELL_BATTLE_SHOUT)) {
            p.spells.add(Content.SPELL_BATTLE_SHOUT);
        }
    }

    private static void unlockPaladin(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_VOW_TESTED);
        if (!p.spells.contains(org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS)) {
            p.spells.add(org.tbc.world.spell.SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        }
    }

    private static void unlockHunter(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_THE_MARKED_TRAIL);
        if (!p.spells.contains(org.tbc.world.spell.SpellEngine.HUNTERS_MARK)) {
            p.spells.add(org.tbc.world.spell.SpellEngine.HUNTERS_MARK);
        }
    }

    /**
     * TP-SL35-042 — Rogue unlock 90013 teaches Sinister Strike; trainer gated; warrior refused.
     */
    @Test
    void tpSl35QuietHandShouldTeachSinisterStrikeAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Quiethero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_ROGUE_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_QUIET_HAND);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_A_QUIET_HAND, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_CAMP_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_CAMP_TOKEN, 1, client.session()::send);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        p.setHealth(0);
        client.clear();
        WowBuffer dead = new WowBuffer(16);
        dead.putU64(trainer.guid);
        dead.putU32(HeroClassUnlock.QUEST_A_QUIET_HAND);
        dead.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, dead.array());
        assertFalse(p.spells.contains(HeroClassUnlock.SPELL_SINISTER_STRIKE));

        p.setHealth(p.maxHealth());
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_QUIET_HAND);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SINISTER_STRIKE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SINISTER_STRIKE));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_HUNTER));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(6, "WAR5", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warquiet", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_A_QUIET_HAND);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-043 — Disappear from Sight 90014 teaches Stealth 1784. */
    @Test
    void tpSl35DisappearFromSightShouldTeachStealth() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Stealthhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_ROGUE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockRogue(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_SHADOWED_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_SHADOWED_TOKEN, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.SPELL_STEALTH,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.SPELL_STEALTH));
    }

    /** TP-SL35-044 — Finish the Opening 90015 teaches Eviscerate 2098. */
    @Test
    void tpSl35FinishTheOpeningShouldTeachEviscerate() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Finishhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_ROGUE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockRogue(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_FINISH_THE_OPENING);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        wyrm.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50_000);
        wyrm.setHealth(50_000);
        world.map(p.mapId, p.instanceId).add(wyrm);
        for (int i = 0; i < HeroClassUnlock.FOLLOWUP_EVISCERATE_HITS; i++) {
            swingOnce(world, client, p, wyrm);
        }
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_FINISH_THE_OPENING);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_EVISCERATE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_EVISCERATE));
    }

    /** TP-SL35-045 — Keep the Advantage 90016 teaches Slice and Dice 5171. */
    @Test
    void tpSl35KeepTheAdvantageShouldTeachSliceAndDice() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Slicehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_ROGUE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockRogue(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_KEEP_THE_ADVANTAGE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_FINISHING_NOTES, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_FINISHING_NOTES, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_KEEP_THE_ADVANTAGE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SLICE_AND_DICE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SLICE_AND_DICE));
    }

    private static void unlockRogue(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_QUIET_HAND);
        if (!p.spells.contains(HeroClassUnlock.SPELL_SINISTER_STRIKE)) {
            p.spells.add(HeroClassUnlock.SPELL_SINISTER_STRIKE);
        }
    }

    /**
     * TP-SL35-046 — Priest unlock 90017 teaches Lesser Heal; trainer gated; warrior refused.
     */
    @Test
    void tpSl35MercyAndJudgmentShouldTeachLesserHealAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Mercyjudg", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PRIEST_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_HEALING_SUPPLIES, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_HEALING_SUPPLIES, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_LESSER_HEAL,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_LESSER_HEAL));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_ROGUE));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(7, "WAR6", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warmj", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-047 — Judgment from Afar 90018 teaches Smite 585. */
    @Test
    void tpSl35JudgmentFromAfarShouldTeachSmite() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Smitehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PRIEST_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPriest(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SMITE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SMITE));
    }

    /** TP-SL35-048 — A Guarding Word 90019 teaches Power Word: Fortitude 1243. */
    @Test
    void tpSl35GuardingWordShouldTeachPowerWordFortitude() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Forthhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PRIEST_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPriest(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_GUARDING_WORD);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_WARDING_SCROLL, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_WARDING_SCROLL, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_GUARDING_WORD);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.POWER_WORD_FORTITUDE));
    }

    /** TP-SL35-049 — Pain as Warning 90020 teaches Shadow Word: Pain 589. */
    @Test
    void tpSl35PainAsWarningShouldTeachShadowWordPain() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Painhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_PRIEST_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockPriest(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_PAIN_AS_WARNING);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_SHADOW_MARKED_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_SHADOW_MARKED_TOKEN, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_PAIN_AS_WARNING);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SHADOW_WORD_PAIN,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SHADOW_WORD_PAIN));
    }

    private static void unlockPriest(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT);
        if (!p.spells.contains(HeroClassUnlock.SPELL_LESSER_HEAL)) {
            p.spells.add(HeroClassUnlock.SPELL_LESSER_HEAL);
        }
    }

    /**
     * TP-SL35-050 — Mage unlock 90021 teaches Fireball; trainer gated; warrior refused.
     */
    @Test
    void tpSl35ControlledSparkShouldTeachFireballAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Sparkhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_MAGE_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_ARCANE_FRAGMENTS, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_ARCANE_FRAGMENTS, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.FIREBALL,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.FIREBALL));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PRIEST));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(8, "WAR7", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warspark", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-051 — A Cooler Head 90022 teaches Frost Armor 168. */
    @Test
    void tpSl35CoolerHeadShouldTeachFrostArmor() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Frosthero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_MAGE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockMage(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_COOLER_HEAD);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_FROST_TREATED_FOCUS, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_FROST_TREATED_FOCUS, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_COOLER_HEAD);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.FROST_ARMOR,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.FROST_ARMOR));
    }

    /** TP-SL35-052 — Share the Study 90023 teaches Arcane Intellect 1459. */
    @Test
    void tpSl35ShareTheStudyShouldTeachArcaneIntellect() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Studyhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_MAGE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockMage(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_SHARE_THE_STUDY);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_STUDY_NOTES, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_STUDY_NOTES, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_SHARE_THE_STUDY);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_ARCANE_INTELLECT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_ARCANE_INTELLECT));
    }

    /** TP-SL35-053 — A Second School 90024 teaches Frostbolt 116. */
    @Test
    void tpSl35SecondSchoolShouldTeachFrostbolt() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BoltHero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_MAGE_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockMage(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_SECOND_SCHOOL);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_SECOND_SCHOOL);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(org.tbc.world.spell.SpellEngine.FROSTBOLT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(org.tbc.world.spell.SpellEngine.FROSTBOLT));
    }

    private static void unlockMage(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        if (!p.spells.contains(org.tbc.world.spell.SpellEngine.FIREBALL)) {
            p.spells.add(org.tbc.world.spell.SpellEngine.FIREBALL);
        }
    }

    /**
     * TP-SL35-054 — Warlock unlock 90025 teaches Corruption; trainer gated; warrior refused.
     */
    @Test
    void tpSl35BoundFlameShouldTeachCorruptionAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Boundhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_THE_BOUND_FLAME);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_THE_BOUND_FLAME, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_BINDING_MARK, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_BINDING_MARK, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_THE_BOUND_FLAME);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_CORRUPTION,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_CORRUPTION));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_MAGE));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(9, "WAR8", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warbound", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_THE_BOUND_FLAME);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-055 — Shadow in Reserve 90026 teaches Shadow Bolt 686. */
    @Test
    void tpSl35ShadowInReserveShouldTeachShadowBolt() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Bolthero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockWarlock(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_SHADOW_IN_RESERVE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_SHADOWED_PAGE, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_SHADOWED_PAGE, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_SHADOW_IN_RESERVE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SHADOW_BOLT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SHADOW_BOLT));
    }

    /** TP-SL35-056 — Fel at the Edge 90027 teaches Immolate 348. */
    @Test
    void tpSl35FelAtTheEdgeShouldTeachImmolate() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Felhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockWarlock(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_FEL_AT_THE_EDGE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_FEL_EMBER, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_FEL_EMBER, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_FEL_AT_THE_EDGE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_IMMOLATE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_IMMOLATE));
    }

    /** TP-SL35-057 — A Familiar's First Task 90028 teaches Summon Imp 688. */
    @Test
    void tpSl35FamiliarsFirstTaskShouldTeachSummonImp() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Imphero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockWarlock(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_FAMILIARS_FIRST_TASK);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_BINDING_REAGENTS, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_BINDING_REAGENTS, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_FAMILIARS_FIRST_TASK);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_SUMMON_IMP,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_SUMMON_IMP));
    }

    private static void unlockWarlock(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_THE_BOUND_FLAME);
        if (!p.spells.contains(HeroClassUnlock.SPELL_CORRUPTION)) {
            p.spells.add(HeroClassUnlock.SPELL_CORRUPTION);
        }
    }

    /**
     * TP-SL35-058 — Shaman unlock 90029 teaches Lightning Bolt; trainer gated; warrior refused.
     */
    @Test
    void tpSl35ListenToTheElementsShouldTeachLightningBoltAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Elemhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_ELEMENTAL_TOKEN, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_ELEMENTAL_TOKEN, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_LIGHTNING_BOLT,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_LIGHTNING_BOLT));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARLOCK));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(10, "WAR9", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Warelem", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-059 — Mend the Wounded 90030 teaches Healing Wave 331. */
    @Test
    void tpSl35MendTheWoundedShouldTeachHealingWave() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Healhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockShaman(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_MEND_THE_WOUNDED);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_HEALING_HERBS, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_HEALING_HERBS, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_MEND_THE_WOUNDED);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_HEALING_WAVE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_HEALING_WAVE));
    }

    /** TP-SL35-060 — Answering Shock 90031 teaches Earth Shock 8042. */
    @Test
    void tpSl35AnsweringShockShouldTeachEarthShock() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Shockhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockShaman(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_ANSWERING_SHOCK);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_ELEMENTAL_MARKER, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_ELEMENTAL_MARKER, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_ANSWERING_SHOCK);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_EARTH_SHOCK,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_EARTH_SHOCK));
    }

    /** TP-SL35-061 — Call of Earth 90032 teaches Stoneskin Totem 8071. */
    @Test
    void tpSl35CallOfEarthShouldTeachStoneskinTotem() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Earthhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockShaman(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_CALL_OF_EARTH);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_EARTH_SAMPLE, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_EARTH_SAMPLE, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_CALL_OF_EARTH);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_STONESKIN_TOTEM,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_STONESKIN_TOTEM));
    }

    private static void unlockShaman(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS);
        if (!p.spells.contains(HeroClassUnlock.SPELL_LIGHTNING_BOLT)) {
            p.spells.add(HeroClassUnlock.SPELL_LIGHTNING_BOLT);
        }
    }

    /**
     * TP-SL35-062 — Druid unlock 90033 teaches Wrath; trainer gated; warrior refused.
     */
    @Test
    void tpSl35LivingBalanceShouldTeachWrathAndGateTrainer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Grovehero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_DRUID_TRAINER);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);

        client.clear();
        WowBuffer listLocked = new WowBuffer(8);
        listLocked.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertFalse(client.saw(Opcodes.SMSG_TRAINER_LIST));

        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_LIVING_BALANCE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        assertEquals(HeroClassUnlock.QUEST_A_LIVING_BALANCE, p.questLogId[0]);

        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_BLIGHTED_SEED, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_BLIGHTED_SEED, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);

        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_LIVING_BALANCE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_WRATH,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_WRATH));

        client.clear();
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, listLocked.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));

        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_SHAMAN));

        WowClientDouble warClient = new WowClientDouble();
        World.Account warAcc = new World.Account(11, "WAR10", new byte[40], 0, 1, "Win", "x86");
        warClient.connect(warAcc);
        Player warCreated = world.characters.create(warAcc.id(), "Wargrove", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        warClient.login(world, warCreated.guid);
        Player warrior = warClient.session().player();
        warrior.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        WowBuffer warAccept = new WowBuffer(12);
        warAccept.putU64(trainer.guid);
        warAccept.putU32(HeroClassUnlock.QUEST_A_LIVING_BALANCE);
        warClient.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, warAccept.array());
        assertEquals(0, warrior.questLogId[0]);
    }

    /** TP-SL35-063 — Touch of the Grove 90034 teaches Healing Touch 5185. */
    @Test
    void tpSl35TouchOfTheGroveShouldTeachHealingTouch() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Touchhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_DRUID_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockDruid(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_TOUCH_OF_THE_GROVE);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_GROVE_SALVE, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_GROVE_SALVE, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_TOUCH_OF_THE_GROVE);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_HEALING_TOUCH,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_HEALING_TOUCH));
    }

    /** TP-SL35-064 — A Silent Mark 90035 teaches Moonfire 8921. */
    @Test
    void tpSl35SilentMarkShouldTeachMoonfire() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Moonhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_DRUID_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockDruid(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_SILENT_MARK);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_MOONLIGHT_MARK, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_MOONLIGHT_MARK, 1, client.session()::send);
        Creature wyrm = world.objectMgr.spawnCreature(HeroClassUnlock.CREATURE_MANA_WYRM, 0, p.x, p.y, p.z, p.o,
                world.scripts);
        world.map(p.mapId, p.instanceId).add(wyrm);
        world.onCreatureKilled(p, wyrm);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_SILENT_MARK);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_MOONFIRE,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_MOONFIRE));
    }

    /** TP-SL35-065 — A Gift of the Wild 90036 teaches Mark of the Wild 1126. */
    @Test
    void tpSl35GiftOfTheWildShouldTeachMarkOfTheWild() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wildhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, HeroClassUnlock.NPC_HERO_DRUID_TRAINER);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        unlockDruid(p);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(trainer.guid);
        accept.putU32(HeroClassUnlock.QUEST_A_GIFT_OF_THE_WILD);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        world.objectMgr.storeNewItem(p, HeroClassUnlock.ITEM_WILD_OFFERING, 1, world::nextItemGuid);
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId),
                HeroClassUnlock.ITEM_WILD_OFFERING, 1, client.session()::send);
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(trainer.guid);
        choose.putU32(HeroClassUnlock.QUEST_A_GIFT_OF_THE_WILD);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(HeroClassUnlock.SPELL_MARK_OF_THE_WILD,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_LEARNED_SPELL), 0));
        assertTrue(p.spells.contains(HeroClassUnlock.SPELL_MARK_OF_THE_WILD));
    }

    private static void unlockDruid(Player p) {
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_LIVING_BALANCE);
        if (!p.spells.contains(HeroClassUnlock.SPELL_WRATH)) {
            p.spells.add(HeroClassUnlock.SPELL_WRATH);
        }
    }

    private static void swingOnce(World world, WowClientDouble client, Player p, Creature c) {
        p.relocate(c.x, c.y, c.z, c.o);
        WowBuffer atk = new WowBuffer(8);
        atk.putU64(c.guid);
        client.handle(world, Opcodes.CMSG_ATTACKSWING, atk.array());
        client.session().tick(world, 0);
        WowBuffer stop = new WowBuffer(0);
        client.handle(world, Opcodes.CMSG_ATTACKSTOP, stop.array());
    }

    /**
     * TP-SL35-066 — Blood Elf Sunstrider Hero trainers 91001–91009 all offer unlock quests
     * with yellow ! ({@code DIALOG_STATUS_AVAILABLE}).
     */
    @Test
    void tpSl35SunstriderHeroTrainersShouldShowUnlockExclamation() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Sunhero", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature warrior = findOn(world, 530, HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        assertNotNull(warrior);
        world.map(p.mapId, p.instanceId).remove(p);
        p.mapId = 530;
        p.relocate(warrior.x, warrior.y, warrior.z, warrior.o);
        world.map(530, 0).add(p);
        int[] trainers = {
                HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER,
                HeroClassUnlock.NPC_HERO_PALADIN_TRAINER,
                HeroClassUnlock.NPC_HERO_HUNTER_TRAINER,
                HeroClassUnlock.NPC_HERO_ROGUE_TRAINER,
                HeroClassUnlock.NPC_HERO_PRIEST_TRAINER,
                HeroClassUnlock.NPC_HERO_MAGE_TRAINER,
                HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER,
                HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER,
                HeroClassUnlock.NPC_HERO_DRUID_TRAINER
        };
        java.util.Map<Integer, Long> guids = new java.util.HashMap<>();
        for (int entry : trainers) {
            Creature c = findOn(world, 530, entry);
            assertNotNull(c, "Sunstrider trainer " + entry);
            guids.put(entry, c.guid);
        }
        client.clear();
        client.handle(world, Opcodes.CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY, new byte[0]);
        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        WowBuffer st = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        int count = st.getU32();
        java.util.Set<Integer> available = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            long guid = st.getU64();
            int status = st.getU8() & 0xFF;
            for (int entry : trainers) {
                if (guid == guids.get(entry) && status == Content.DIALOG_STATUS_AVAILABLE) {
                    available.add(entry);
                }
            }
        }
        for (int entry : trainers) {
            assertTrue(available.contains(entry), "yellow ! missing for " + entry);
        }
    }

    private static Creature findOn(World world, int mapId, int entry) {
        for (Creature c : world.map(mapId, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }

    private static WowBuffer textEmote(int textEmote, long target) {
        WowBuffer emote = new WowBuffer(16);
        emote.putU32(textEmote);
        emote.putU32(0);
        emote.putU64(target);
        return emote;
    }

    private static int spellGoId(byte[] p) {
        int off = WowClientDouble.skipPackedGuid(p, 0);
        off = WowClientDouble.skipPackedGuid(p, off);
        return WowClientDouble.u32le(p, off);
    }

    private static long packedGuid(byte[] p, int off) {
        int mask = p[off++] & 0xFF;
        long g = 0;
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                g |= (long) (p[off++] & 0xFF) << (8 * i);
            }
        }
        return g;
    }

    private static WowBuffer charCreate(String name, int race, int clazz) {
        WowBuffer create = new WowBuffer(32);
        create.putCString(name);
        create.putU8(race);
        create.putU8(clazz);
        create.putU8(0);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(0);
        create.putU8(0);
        return create;
    }
}
