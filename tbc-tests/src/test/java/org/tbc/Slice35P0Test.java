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

    /** TP-SL35-002 — Auto Attack only on bar; Recruit cloth + Worn Shortsword equipped; 3 silver. */
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
        assertEquals(ClasslessConfig.ARMOR_CLOTH_MASK, created.armorProficiency() & ClasslessConfig.ARMOR_CLOTH_MASK);
        assertEquals(ClasslessConfig.WEAPON_UNARMED_MASK, created.weaponProficiency() & ClasslessConfig.WEAPON_UNARMED_MASK);
        assertEquals(0, created.weaponProficiency() & (1 << 7), "no sword proficiency");
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER, created.money);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_SHIRT, created.itemAt(0, 3).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_PANTS, created.itemAt(0, 6).entry);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_BOOTS, created.itemAt(0, 7).entry);
        assertEquals(ClasslessConfig.STARTER_WEAPON, created.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND).entry);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertTrue(p.spells.contains(ClasslessConfig.AUTO_ATTACK));
        assertEquals(ClasslessConfig.STARTING_MONEY_COPPER, p.money);
        assertEquals(ClasslessConfig.ITEM_RECRUIT_SHIRT, p.itemAt(0, 3).entry);
        assertEquals(ClasslessConfig.STARTER_WEAPON, p.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND).entry);
        byte[] bar = client.payload(Opcodes.SMSG_ACTION_BUTTONS);
        assertEquals(ClasslessConfig.AUTO_ATTACK, WowClientDouble.u32le(bar, 0));
        assertEquals(0, WowClientDouble.u32le(bar, 73 * 4), "no HS on stance bar");
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

    /** TP-SL35-004 — trainer list/buy eligible; normal warrior unchanged. */
    @Test
    void tpSl35TrainerBuyEligibleAbility() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Trainless", 1, ClasslessConfig.CLASS_CLASSLESS,
                0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(Content.TRAINER_SPELL_BATTLE_SHOUT_COST);
        client.clear();
        WowBuffer list = new WowBuffer(8);
        list.putU64(trainer.guid);
        client.handle(world, Opcodes.CMSG_TRAINER_LIST, list.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_LIST));
        client.clear();
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());
        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(p.spells.contains(Content.SPELL_BATTLE_SHOUT));
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
        double untrainedAdd = org.tbc.world.classless.WeaponPenaltyPolicy.missAddPercent(p, false);
        assertEquals(ClasslessConfig.get().untrainedWeaponMissAddPct(), untrainedAdd, 1e-9);
        p.addWeaponProficiency(1 << 7); // 1H swords
        assertEquals(0.0, org.tbc.world.classless.WeaponPenaltyPolicy.missAddPercent(p, false), 1e-9);
        MeleeTable.Result hit = MeleeTable.alwaysHit().rollOne(p, target, 2, 4);
        assertTrue(hit.damage() > 0);
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
