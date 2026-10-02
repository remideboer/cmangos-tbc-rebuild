package org.tbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.classless.ClasslessConfig;
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
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(Content.TRAINER_SPELL_BATTLE_SHOUT_COST);
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
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

    /** TP-SL35-004 — any class trainer list/buy; warrior cannot use mage trainer. */
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
        // Cumulative: BS 200×1 + Rank2 500×2 + Fireball 10×3 = 1230+
        p.setMoney(5_000);
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
        assertTrue(sawBattleShout, "Llane list must include Battle Shout 6673");
        client.clear();
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(warriorTrainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT));

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
        WowBuffer mageList = new WowBuffer(8);
        mageList.putU64(mageTrainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, mageList.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST), "classless opens mage trainer");
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
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        Creature mage = find(world, Content.NPC_KHELDEN_BREMEN);
        assertNotNull(mage);

        // learned 0 → 100 copper (not Battle Shout row 200).
        p.setMoney(ClasslessConfig.STARTING_MONEY_COPPER);
        client.clear();
        WowBuffer buyBs = new WowBuffer(12);
        buyBs.putU64(trainer.guid);
        buyBs.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buyBs.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT));
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER - 100, p.money);

        // learned 1 → 200; short money refuses.
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

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
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
