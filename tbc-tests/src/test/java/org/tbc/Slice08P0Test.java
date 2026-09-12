package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL08-004: MovementType 1 creatures wander OOC so the 8606 client sees them walk. */
class Slice08P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl08WanderWhenMovementTypeRandomShouldSendMonsterMove() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wander", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, -5000f, -5000f, 80f, 0f, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        c.movementType = 1;
        c.spawnDist = 10f;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.startOocMotion();
        float ox = p.x;
        float oy = p.y;
        p.relocate(c.x + 40, c.y, c.z, c.o);
        world.map(p.mapId, p.instanceId).reindex(p, ox, oy);
        client.clear();
        world.tick(1000);
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
        WowBuffer move = new WowBuffer(client.payload(Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(c.guid, move.getPackedGuid());
        move.getFloat();
        move.getFloat();
        move.getFloat();
        move.getU32();
        move.getU8();
        move.getU32();
        int duration = move.getU32();
        assertEquals(1, move.getU32());
        float destX = move.getFloat();
        float destY = move.getFloat();
        move.getFloat();
        assertTrue(c.spawnDistance2d(destX, destY) <= 10f + 0.01f);
        float path = (float) Math.hypot(destX - c.spawnX, destY - c.spawnY);
        assertEquals(Math.max(1, (int) (path / UpdateBuilder.WALK * 1000f)), duration);
    }

    /**
     * TP-SL08-019 — HandleQuestgiverStatusQueryOpcode / getDialogStatus: Willem 823 offers 783
     * (QUEST_STATUS_NONE + CanSeeStartQuest) → SMSG_QUESTGIVER_STATUS raw guid + DIALOG_STATUS_AVAILABLE 6.
     */
    @Test
    void tpSl08QuestgiverStatusQuery() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Quester", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);

        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(willem.guid);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_STATUS_QUERY, q.array());

        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_STATUS));
        WowBuffer st = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_STATUS));
        assertEquals(willem.guid, st.getU64());
        assertEquals(DIALOG_STATUS_AVAILABLE, st.getU8());
    }

    /**
     * TP-SL08-020 — HandleQuestgiverStatusMultipleQuery / SendQuestGiverStatusMultiple:
     * empty C2S; SMSG_QUESTGIVER_STATUS_MULTIPLE count + raw guid + status for each visible
     * UNIT_NPC_FLAG_QUESTGIVER creature (kobold 6 has no flag).
     */
    @Test
    void tpSl08QuestgiverStatusMultiple() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "QIcons", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        Creature kobold = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        world.map(p.mapId, p.instanceId).add(kobold);

        client.clear();
        client.handle(world, Opcodes.CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY, new byte[0]);

        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        WowBuffer st = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        int count = st.getU32();
        boolean sawWillem = false;
        for (int i = 0; i < count; i++) {
            long guid = st.getU64();
            int status = st.getU8();
            assertTrue(guid != kobold.guid);
            if (guid == willem.guid) {
                sawWillem = true;
                assertEquals(DIALOG_STATUS_AVAILABLE, status);
            }
        }
        assertTrue(sawWillem);
    }

    /**
     * TP-SL08-021 — Player::KilledMonsterCredit / SendQuestUpdateAddCreatureOrGo:
     * Kobold Camp Cleanup 7 ReqCreatureOrGOId1 6 count 10 → SMSG_QUESTUPDATE_ADD_KILL;
     * 10th kill marks QUEST_STATE_COMPLETE + SMSG_QUESTUPDATE_COMPLETE.
     */
    @Test
    void tpSl08KillObjectiveCounts() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "KoboldHunt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature mcbride = world.objectMgr.spawnCreature(Content.NPC_MARSHAL_MCBRIDE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(mcbride);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(mcbride.guid);
        accept.putU32(Content.QUEST_KOBOLD_CAMP_CLEANUP);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());

        Creature first = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(first);
        client.clear();
        world.onCreatureKilled(p, first);

        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        WowBuffer add = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, add.getU32());
        assertEquals(6, add.getU32());
        assertEquals(1, add.getU32());
        assertEquals(10, add.getU32());
        assertEquals(first.guid, add.getU64());
        int counts = p.getInt(UpdateFields.PLAYER_QUEST_LOG_1_1 + 2);
        assertEquals(1, counts & 0xFF);

        for (int i = 2; i <= 10; i++) {
            Creature k = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
            world.map(p.mapId, p.instanceId).add(k);
            client.clear();
            world.onCreatureKilled(p, k);
            WowBuffer row = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
            row.getU32();
            row.getU32();
            assertEquals(i, row.getU32());
            assertEquals(10, row.getU32());
        }
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        WowBuffer done = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, done.getU32());
    }

    /**
     * TP-SL08-022 — Player::ItemAddedQuestCheck / SendQuestUpdateAddItem:
     * Brotherhood of Thieves 18 ReqItemId1 752 × 12. C++ packet is item u32 + add-count u32
     * (not quest id). Looting 12 completes the objective.
     */
    @Test
    void tpSl08ItemObjectiveCounts() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Bandanas", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(willem.guid);
        accept.putU32(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());

        world.objectMgr.creatureLoot.put(6, java.util.List.of(
                new org.tbc.world.content.ObjectMgr.LootRow(Content.ITEM_RED_BURLAP_BANDANA, 100f, 12, 12)));
        Creature corpse = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(corpse);
        corpse.setHealth(0);
        world.combat.creatureDied(corpse, p, world.nowMs(), null);
        world.objectMgr.fillCorpseLoot(corpse);

        client.loot(world, corpse.guid);
        client.clear();
        client.autostoreLootItem(world, 0);

        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        WowBuffer add = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        assertEquals(Content.ITEM_RED_BURLAP_BANDANA, add.getU32());
        assertEquals(12, add.getU32());
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertTrue(client.saw(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        WowBuffer done = new WowBuffer(client.payload(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertEquals(Content.QUEST_BROTHERHOOD_OF_THIEVES, done.getU32());
    }

    /**
     * TP-SL08-023 — Player::RewardQuest / SendQuestReward.
     * Quest 783: XP 40 (RewMoneyMaxLevel 24 / 0.6). Quest 2158: RewItemId1 159 × 5 + PUSH.
     */
    @Test
    void tpSl08QuestRewardXpAndItems() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rewarded", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        Creature mcbride = world.objectMgr.spawnCreature(Content.NPC_MARSHAL_MCBRIDE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        world.map(p.mapId, p.instanceId).add(mcbride);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(willem.guid);
        accept.putU32(Content.QUEST_A_THREAT_WITHIN);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(mcbride.guid);
        choose.putU32(Content.QUEST_A_THREAT_WITHIN);
        choose.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        WowBuffer complete = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertEquals(Content.QUEST_A_THREAT_WITHIN, complete.getU32());
        assertEquals(0x03, complete.getU32());
        assertEquals(40, complete.getU32());
        assertEquals(0, complete.getU32());
        assertEquals(0, complete.getU32());
        assertEquals(0, complete.getU32());
        assertEquals(40, p.xp);
        assertEquals(40, p.getInt(UpdateFields.PLAYER_XP));

        world.objectMgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY,
                new java.util.ArrayList<>(java.util.List.of(Content.QUEST_REST_AND_RELAXATION)));
        world.objectMgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY,
                new java.util.ArrayList<>(java.util.List.of(Content.QUEST_REST_AND_RELAXATION)));
        Creature farley = world.objectMgr.spawnCreature(Content.NPC_INNKEEPER_FARLEY, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(farley);
        WowBuffer acceptInn = new WowBuffer(12);
        acceptInn.putU64(farley.guid);
        acceptInn.putU32(Content.QUEST_REST_AND_RELAXATION);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, acceptInn.array());
        client.clear();
        WowBuffer chooseInn = new WowBuffer(16);
        chooseInn.putU64(farley.guid);
        chooseInn.putU32(Content.QUEST_REST_AND_RELAXATION);
        chooseInn.putU32(0);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, chooseInn.array());
        assertTrue(client.saw(Opcodes.SMSG_ITEM_PUSH_RESULT));
        WowBuffer innDone = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertEquals(Content.QUEST_REST_AND_RELAXATION, innDone.getU32());
        innDone.getU32();
        innDone.getU32();
        innDone.getU32();
        innDone.getU32();
        assertEquals(1, innDone.getU32());
        assertEquals(Content.ITEM_REFRESHING_SPRING_WATER, innDone.getU32());
        assertEquals(5, innDone.getU32());
        assertTrue(p.items.values().stream().anyMatch(it ->
                it.entry == Content.ITEM_REFRESHING_SPRING_WATER && it.count == 5));
    }

    /**
     * TP-SL08-024 — HandleQuestgiverChooseRewardOpcode reward index.
     * Quest 18 RewChoiceItemId[1] is Militia Hammer 5580; index 0 is Dagger 2224.
     */
    @Test
    void tpSl08ChoiceRewardIndex() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Chooser", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(willem.guid);
        accept.putU32(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        client.clear();
        WowBuffer choose = new WowBuffer(16);
        choose.putU64(willem.guid);
        choose.putU32(Content.QUEST_BROTHERHOOD_OF_THIEVES);
        choose.putU32(1);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD, choose.array());
        assertTrue(client.saw(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_MILITIA_HAMMER && it.count == 1));
        assertFalse(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_MILITIA_DAGGER));
    }

    /**
     * TP-SL08-025 — HandleQuestgiverRequestRewardOpcode → PlayerMenu::SendQuestGiverOfferReward.
     */
    @Test
    void tpSl08RequestRewardOffer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Offered", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        Creature mcbride = world.objectMgr.spawnCreature(Content.NPC_MARSHAL_MCBRIDE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        world.map(p.mapId, p.instanceId).add(mcbride);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(willem.guid);
        accept.putU32(Content.QUEST_A_THREAT_WITHIN);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        client.clear();
        WowBuffer req = new WowBuffer(12);
        req.putU64(mcbride.guid);
        req.putU32(Content.QUEST_A_THREAT_WITHIN);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_REQUEST_REWARD, req.array());
        assertTrue(client.saw(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        WowBuffer offer = new WowBuffer(client.payload(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        assertEquals(mcbride.guid, offer.getU64());
        assertEquals(Content.QUEST_A_THREAT_WITHIN, offer.getU32());
        assertEquals("A Threat Within", offer.getCString());
        assertEquals("", offer.getCString());
        assertEquals(1, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0x08, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
    }

    /**
     * TP-SL08-026 — HandleQuestLogRemoveQuest → SetQuestSlot(slot, 0).
     * PLAYER_QUEST_LOG_1_1..3 VALUES to 0.
     */
    @Test
    void tpSl08AbandonQuest() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Abandoner", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature willem = world.objectMgr.spawnCreature(Content.NPC_DEPUTY_WILLEM, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(willem);
        WowBuffer accept = new WowBuffer(12);
        accept.putU64(willem.guid);
        accept.putU32(Content.QUEST_A_THREAT_WITHIN);
        client.handle(world, Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST, accept.array());
        client.clear();
        WowBuffer remove = new WowBuffer(1);
        remove.putU8(0);
        client.handle(world, Opcodes.CMSG_QUESTLOG_REMOVE_QUEST, remove.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_QUEST_LOG_1_1));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_QUEST_LOG_1_2));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_QUEST_LOG_1_3));
    }

    /**
     * TP-SL08-027 — CMSG_SET_WATCHED_FACTION int32 → PLAYER_FIELD_WATCHED_FACTION_INDEX VALUES.
     */
    @Test
    void tpSl08SetWatchedFaction() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "RepWatch", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        client.clear();
        WowBuffer in = new WowBuffer(4);
        in.putU32(72);
        client.handle(world, Opcodes.CMSG_SET_WATCHED_FACTION, in.array());
        assertEquals(72, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_WATCHED_FACTION_INDEX));
    }

    /**
     * TP-SL08-027 — CMSG_SET_FACTION_INACTIVE Stormwind list 19; next login
     * SMSG_INITIALIZE_FACTIONS slot 19 has FACTION_FLAG_INACTIVE.
     */
    @Test
    void tpSl08SetFactionInactive() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "RepHide", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        long guid = client.session().player().guid;
        client.clear();
        WowBuffer in = new WowBuffer(5);
        in.putU32(19);
        in.putU8(1);
        client.handle(world, Opcodes.CMSG_SET_FACTION_INACTIVE, in.array());
        client.session().logout(world, true);
        WowClientDouble relog = new WowClientDouble();
        relog.connect(ACC);
        relog.login(world, guid);
        WowBuffer fac = new WowBuffer(relog.payload(Opcodes.SMSG_INITIALIZE_FACTIONS));
        assertEquals(0x80, fac.getU32());
        for (int i = 0; i < 19; i++) {
            fac.getU8();
            fac.getU32();
        }
        int flags = fac.getU8();
        assertEquals(0x20, flags & 0x20);
        assertEquals(0x01, flags & 0x01);
    }

    /**
     * TP-SL08-027 — CMSG_SET_FACTION_ATWAR Booty Bay list 1; next login
     * SMSG_INITIALIZE_FACTIONS slot 1 has FACTION_FLAG_AT_WAR.
     */
    @Test
    void tpSl08SetFactionAtWar() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "RepWar", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        long guid = client.session().player().guid;
        client.clear();
        WowBuffer in = new WowBuffer(5);
        in.putU32(1);
        in.putU8(1);
        client.handle(world, Opcodes.CMSG_SET_FACTION_ATWAR, in.array());
        client.session().logout(world, true);
        WowClientDouble relog = new WowClientDouble();
        relog.connect(ACC);
        relog.login(world, guid);
        WowBuffer fac = new WowBuffer(relog.payload(Opcodes.SMSG_INITIALIZE_FACTIONS));
        assertEquals(0x80, fac.getU32());
        fac.getU8();
        fac.getU32();
        int flags = fac.getU8();
        assertEquals(0x02, flags & 0x02);
    }

    /**
     * TP-SL08-028 — Vendor buy success sends the same CREATE item + inventory slot
     * VALUES + PLAYER_FIELD_COINAGE as loot autostore (not only ITEM_PUSH_RESULT).
     */
    @Test
    void tpSl08VendorBuyCreatesItemAndCoinageValues() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Shopper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setMoney(50);
        Creature vendor = world.objectMgr.spawnCreature(Content.NPC_CORINA_STEELE, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(vendor);
        java.util.Set<Integer> before = new java.util.HashSet<>(p.items.keySet());
        client.clear();
        client.buyItem(world, vendor.guid, Content.ITEM_WORN_SHORTSWORD, 1);
        Item bought = p.items.entrySet().stream()
                .filter(e -> !before.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
        assertTrue(client.sawCreateObject(UpdateBuilder.itemGuid(bought)));
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + bought.slot * 2;
        long slotGuid = Integer.toUnsignedLong(client.valuesField(p.guid, field))
                | ((long) client.valuesField(p.guid, field + 1) << 32);
        assertEquals(UpdateBuilder.itemGuid(bought), slotGuid);
        assertEquals(15, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(15, p.money);
        assertTrue(client.saw(Opcodes.SMSG_ITEM_PUSH_RESULT));
    }

    /** QuestDef.h DIALOG_STATUS_AVAILABLE — yellow exclamation. */
    private static final int DIALOG_STATUS_AVAILABLE = 6;
}
