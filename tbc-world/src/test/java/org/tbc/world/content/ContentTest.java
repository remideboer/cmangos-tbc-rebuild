package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTest {
    private ObjectMgr mgr;
    private Content content;
    private GameMap map;
    private Player p;
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();
    private long nextItem = 1;

    @BeforeEach
    void setUp() {
        mgr = new ObjectMgr();
        mgr.load(null, null);
        content = new Content(mgr);
        map = new GameMap(0, 0);
        p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        ops.clear();
        last.clear();
        nextItem = 1;
    }

    @Test
    void gossipVendorListsBrowseGoods() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        WowBuffer b = gossipBody();
        assertEquals(vendor.guid, b.getU64());
        assertEquals(0, b.getU32());
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, b.getU32());
        int items = b.getU32();
        assertTrue(items >= 1);
        boolean vendorIcon = false;
        for (int i = 0; i < items; i++) {
            assertEquals(i, b.getU32());
            if (b.getU8() == Content.GOSSIP_ICON_VENDOR) {
                vendorIcon = true;
            }
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        assertTrue(vendorIcon);
        assertEquals(0, b.getU32());
    }

    @Test
    void gossipQuestGiverListsQuestRows() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        content.gossipHello(p, map, u64(giver.guid), this::capture);
        WowBuffer b = gossipBody();
        b.getU64();
        assertEquals(0, b.getU32());
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, b.getU32());
        int items = b.getU32();
        for (int i = 0; i < items; i++) {
            b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        assertTrue(b.getU32() >= 1);
    }

    @Test
    void questGiverStatusQueryWhenOfferedShouldSendAvailable() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        content.questGiverStatusQuery(p, map, new WowBuffer(3), this::capture);
        content.questGiverStatusQuery(p, map, u64(99), this::capture);
        assertTrue(ops.isEmpty());
        content.questGiverStatusQuery(p, map, u64(giver.guid), this::capture);
        WowBuffer st = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS));
        assertEquals(giver.guid, st.getU64());
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, st.getU8());
        ops.clear();
        p.relocate(20, 0, 0, 0);
        content.questGiverStatusQuery(p, map, u64(giver.guid), this::capture);
        st = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS));
        st.getU64();
        assertEquals(Content.DIALOG_STATUS_NONE, st.getU8());
        p.relocate(0, 0, 0, 0);
        ops.clear();
        Creature kobold = spawn(6, 0, 0);
        content.questGiverStatusQuery(p, map, u64(kobold.guid), this::capture);
        st = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS));
        st.getU64();
        assertEquals(Content.DIALOG_STATUS_NONE, st.getU8());
        ops.clear();
        mgr.questGivers.put(6, new ArrayList<>(List.of(99999)));
        content.questGiverStatusQuery(p, map, u64(kobold.guid), this::capture);
        st = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS));
        st.getU64();
        assertEquals(Content.DIALOG_STATUS_NONE, st.getU8());
    }

    @Test
    void questGiverStatusMultipleWhenVisibleShouldListQuestGivers() {
        content.questGiverStatusMultiple(p, map, this::capture);
        WowBuffer empty = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        assertEquals(0, empty.getU32());
        ops.clear();
        last.clear();
        Creature kobold = spawn(6, 0, 0);
        content.questGiverStatusMultiple(p, map, this::capture);
        WowBuffer none = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        assertEquals(0, none.getU32());
        ops.clear();
        last.clear();
        Creature far = spawn(Content.NPC_DEPUTY_WILLEM, 200, 0);
        content.questGiverStatusMultiple(p, map, this::capture);
        WowBuffer oor = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        assertEquals(0, oor.getU32());
        map.remove(far);
        ops.clear();
        last.clear();
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        content.questGiverStatusMultiple(p, map, this::capture);
        WowBuffer st = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        assertEquals(1, st.getU32());
        long guid = st.getU64();
        assertEquals(giver.guid, guid);
        assertTrue(guid != kobold.guid);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, st.getU8());
    }

    @Test
    void killedMonsterCreditWhenKillQuestShouldCountAndComplete() {
        content.killedMonsterCredit(p, null, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        Creature garrick = spawn(103, 0, 0);
        content.killedMonsterCredit(p, garrick, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        p.questLogId[0] = 404;
        content.killedMonsterCredit(p, spawn(6, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        content.killedMonsterCredit(p, spawn(6, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        p.questLogId[0] = 0;
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.acceptQuest(p, map, quest(mcbride.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        ops.clear();
        last.clear();
        Creature kobold = spawn(6, 0, 0);
        content.killedMonsterCredit(p, kobold, this::capture);
        WowBuffer add = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, add.getU32());
        assertEquals(6, add.getU32());
        assertEquals(1, add.getU32());
        assertEquals(10, add.getU32());
        assertEquals(kobold.guid, add.getU64());
        content.killedMonsterCredit(p, garrick, this::capture);
        WowBuffer still = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        still.getU32();
        still.getU32();
        assertEquals(1, still.getU32());
        ops.clear();
        last.clear();
        for (int i = 2; i <= 10; i++) {
            content.killedMonsterCredit(p, spawn(6, 0, 0), this::capture);
        }
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        WowBuffer done = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, done.getU32());
        ops.clear();
        last.clear();
        content.killedMonsterCredit(p, spawn(6, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        p.questLogId[1] = 8;
        mgr.quests.put(8, new ObjectMgr.QuestTemplate(8, "Zero", 1, 0, 0, "", "", 6, 0));
        ops.clear();
        content.killedMonsterCredit(p, spawn(6, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
    }

    @Test
    void itemAddedQuestCheckWhenCollectQuestShouldCountAndComplete() {
        content.itemAddedQuestCheck(p, 0, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        p.questLogId[0] = 404;
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        p.questLogId[0] = 0;
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        ops.clear();
        last.clear();
        content.itemAddedQuestCheck(p, Content.ITEM_WORN_SHORTSWORD, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        WowBuffer add = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        assertEquals(Content.ITEM_RED_BURLAP_BANDANA, add.getU32());
        assertEquals(1, add.getU32());
        ops.clear();
        last.clear();
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 20, this::capture);
        add = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        assertEquals(Content.ITEM_RED_BURLAP_BANDANA, add.getU32());
        assertEquals(11, add.getU32());
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        WowBuffer done = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertEquals(Content.QUEST_BROTHERHOOD_OF_THIEVES, done.getU32());
        ops.clear();
        last.clear();
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
        p.questLogId[1] = 9;
        mgr.quests.put(9, new ObjectMgr.QuestTemplate(9, "NoItems", 1, 0, 0, "", "", 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 0));
        ops.clear();
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM));
    }

    @Test
    void rewardQuestWhenXpItemAndMaxLevelShouldMatchSendQuestReward() {
        p.race = 1;
        p.clazz = 1;
        p.initStatsForLevel(mgr.levelStats);
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        last.clear();
        content.completeQuest(p, map, quest(mcbride.guid, Content.QUEST_A_THREAT_WITHIN), nextItem++, this::capture);
        WowBuffer done = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertEquals(Content.QUEST_A_THREAT_WITHIN, done.getU32());
        assertEquals(0x03, done.getU32());
        assertEquals(40, done.getU32());
        assertEquals(0, done.getU32());
        assertEquals(0, done.getU32());
        assertEquals(0, done.getU32());

        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        p.level = Player.MAX_LEVEL;
        int moneyBefore = p.money;
        ops.clear();
        last.clear();
        content.completeQuest(p, map, quest(mcbride.guid, Content.QUEST_A_THREAT_WITHIN), nextItem++, this::capture);
        done = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        done.getU32();
        done.getU32();
        assertEquals(0, done.getU32());
        assertEquals(24, done.getU32());
        assertEquals(moneyBefore + 24, p.money);

        p.level = 1;
        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION, 99)));
        mgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION, 99)));
        mgr.quests.put(99, new ObjectMgr.QuestTemplate(99, "ZeroRewCount", 1, 0, 0, "", "", 0, 0, 0, 0, 1, 0,
                Content.ITEM_WORN_SHORTSWORD, 0));
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.acceptQuest(p, map, quest(farley.guid, 99), this::capture);
        ops.clear();
        last.clear();
        content.completeQuest(p, map, quest(farley.guid, 99), nextItem++, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        ops.clear();
        last.clear();
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), nextItem++, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        ops.clear();
        last.clear();
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), 0, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        ops.clear();
        last.clear();
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        ObjectMgr.ItemTemplate water = new ObjectMgr.ItemTemplate();
        water.entry = Content.ITEM_REFRESHING_SPRING_WATER;
        water.displayId = 180;
        mgr.items.put(Content.ITEM_REFRESHING_SPRING_WATER, water);
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), nextItem++, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        done = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertEquals(Content.QUEST_REST_AND_RELAXATION, done.getU32());
        done.getU32();
        done.getU32();
        done.getU32();
        done.getU32();
        assertEquals(1, done.getU32());
        assertEquals(Content.ITEM_REFRESHING_SPRING_WATER, done.getU32());
        assertEquals(5, done.getU32());
        fillBackpack();
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        ops.clear();
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), nextItem++, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
    }

    @Test
    void rewardQuestWhenChoiceIndexOneShouldStoreOnlyThatChoiceItem() {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        mgr.questInvolved.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES)));
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        ops.clear();
        last.clear();
        content.completeQuest(p, map, chooseReward(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES, 1),
                nextItem++, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertTrue(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_MILITIA_HAMMER && it.count == 1));
        assertFalse(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_MILITIA_DAGGER));
        ops.clear();
        last.clear();
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        content.completeQuest(p, map, chooseReward(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES, 6),
                nextItem++, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        content.completeQuest(p, map, chooseReward(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES, 2),
                nextItem++, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertEquals(1, p.items.values().stream().filter(it -> it.entry == Content.ITEM_MILITIA_HAMMER).count());
        assertFalse(p.items.values().stream().anyMatch(it -> it.entry == Content.ITEM_MILITIA_DAGGER));
    }

    @Test
    void requestRewardWhenQuestInLogShouldSendOfferReward() {
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        last.clear();
        content.requestReward(p, map, quest(mcbride.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        WowBuffer offer = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        assertEquals(mcbride.guid, offer.getU64());
        assertEquals(Content.QUEST_A_THREAT_WITHIN, offer.getU32());
        assertEquals("A Threat Within", offer.getCString());
        assertEquals("", offer.getCString());
        assertEquals(1, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());

        mgr.quests.remove(Content.QUEST_A_THREAT_WITHIN);
        ops.clear();
        content.requestReward(p, map, quest(mcbride.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        mgr.quests.put(Content.QUEST_A_THREAT_WITHIN, new ObjectMgr.QuestTemplate(
                Content.QUEST_A_THREAT_WITHIN, "A Threat Within", 1, 0, 0, "Speak with Marshal McBride.",
                "Speak with Marshal McBride.", 0, 0, 0, 0, 1, 24, 0, 0));

        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        ops.clear();
        last.clear();
        content.requestReward(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        offer = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        offer.getU64();
        offer.getU32();
        offer.getCString();
        offer.getCString();
        offer.getU32();
        offer.getU32();
        offer.getU32();
        assertEquals(2, offer.getU32());
        assertEquals(Content.ITEM_MILITIA_DAGGER, offer.getU32());
        assertEquals(1, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(Content.ITEM_MILITIA_HAMMER, offer.getU32());
        assertEquals(1, offer.getU32());
        assertEquals(0, offer.getU32());
        assertEquals(0, offer.getU32());

        mgr.quests.put(98, new ObjectMgr.QuestTemplate(98, "OneChoice", 1, 0, 0, "", "", 0, 0, 0, 0, 1, 0, 0, 0,
                Content.ITEM_MILITIA_DAGGER, 1, 0, 0));
        mgr.questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(
                Content.QUEST_A_THREAT_WITHIN, Content.QUEST_BROTHERHOOD_OF_THIEVES, 98)));
        mgr.questInvolved.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_BROTHERHOOD_OF_THIEVES, 98)));
        content.acceptQuest(p, map, quest(willem.guid, 98), this::capture);
        ops.clear();
        last.clear();
        content.requestReward(p, map, quest(willem.guid, 98), this::capture);
        offer = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        offer.getU64();
        offer.getU32();
        offer.getCString();
        offer.getCString();
        offer.getU32();
        offer.getU32();
        offer.getU32();
        assertEquals(1, offer.getU32());
        assertEquals(Content.ITEM_MILITIA_DAGGER, offer.getU32());

        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        ObjectMgr.ItemTemplate water = new ObjectMgr.ItemTemplate();
        water.entry = Content.ITEM_REFRESHING_SPRING_WATER;
        water.displayId = 180;
        mgr.items.put(Content.ITEM_REFRESHING_SPRING_WATER, water);
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        ops.clear();
        last.clear();
        content.requestReward(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        offer = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        offer.getU64();
        offer.getU32();
        offer.getCString();
        offer.getCString();
        offer.getU32();
        offer.getU32();
        offer.getU32();
        assertEquals(0, offer.getU32());
        assertEquals(1, offer.getU32());
        assertEquals(Content.ITEM_REFRESHING_SPRING_WATER, offer.getU32());
        assertEquals(5, offer.getU32());
        assertEquals(180, offer.getU32());
    }

    @Test
    void gossipMobHasEmptyMenu() {
        Creature kobold = spawn(6, 0, 0);
        content.gossipHello(p, map, u64(kobold.guid), this::capture);
        WowBuffer b = gossipBody();
        b.getU64();
        assertEquals(0, b.getU32());
        assertEquals(Content.DEFAULT_GOSSIP_MESSAGE, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
    }

    @Test
    void gossipVendorWithQuestListsBoth() {
        mgr.questGivers.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        WowBuffer b = gossipBody();
        b.getU64();
        b.getU32();
        b.getU32();
        int items = b.getU32();
        assertTrue(items >= 1);
        for (int i = 0; i < items; i++) {
            b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        assertTrue(b.getU32() >= 1);
    }

    @Test
    void gossipSelectVendorOpensShop() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(vendor.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_LIST_INVENTORY));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_LIST_INVENTORY));
        assertEquals(vendor.guid, b.getU64());
        assertTrue((b.getU8() & 0xFF) >= 1);
    }

    @Test
    void gossipSelectTrainerOpensList() {
        p.clazz = 1;
        Creature trainer = spawn(Content.NPC_LLANE_BESHERE, 0, 0);
        content.gossipHello(p, map, u64(trainer.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(trainer.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_TRAINER_LIST));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_TRAINER_LIST));
        assertEquals(trainer.guid, b.getU64());
        b.getU32();
        assertTrue(b.getU32() >= 1);
        assertEquals(Content.SPELL_BATTLE_SHOUT, b.getU32());
    }

    @Test
    void gossipSelectBankerOpensBank() {
        Creature banker = spawn(Content.NPC_OLIVIA_BURNSIDE, 0, 0);
        content.gossipHello(p, map, u64(banker.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(banker.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SHOW_BANK));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_SHOW_BANK));
        assertEquals(banker.guid, b.getU64());
    }

    @Test
    void gossipSelectTaxiOpensFlightMap() {
        p.team = 469;
        Creature master = spawn(Content.NPC_DUNGAR_LONGDRINK, 0, 0);
        content.gossipHello(p, map, u64(master.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(master.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SHOWTAXINODES));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_SHOWTAXINODES));
        assertEquals(1, b.getU32());
        assertEquals(master.guid, b.getU64());
        assertEquals(Content.TAXI_STORMWIND, b.getU32());
    }

    @Test
    void gossipSelectInnkeeperConfirmsBind() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        int[] choice = gossipMenuAndListByIcon(Content.GOSSIP_ICON_INTERACT_2);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, choice[0], choice[1]), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        assertTrue(ops.contains(Opcodes.SMSG_BINDER_CONFIRM));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_BINDER_CONFIRM));
        assertEquals(inn.guid, b.getU64());
    }

    @Test
    void gossipSelectNestedOpensSubmenu() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        WowBuffer hello = gossipBody();
        hello.getU64();
        int menuId = hello.getU32();
        hello.getU32();
        int gossipItemCount = hello.getU32();
        int nested = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = hello.getU32();
            hello.getU8();
            hello.getU8();
            hello.getU32();
            String text = hello.getCString();
            hello.getCString();
            if (Content.GOSSIP_FARLEY_INN_INFO.equals(text)) {
                nested = index;
            }
        }
        assertTrue(nested >= 0);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, menuId, nested), this::capture);
        WowBuffer b = gossipBody();
        assertEquals(inn.guid, b.getU64());
        assertEquals(Content.GOSSIP_MENU_FARLEY_INN_INFO, b.getU32());
        assertEquals(Content.GOSSIP_TEXT_FARLEY_INN_INFO, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.remaining());
    }

    @Test
    void gossipSelectGossipStayLeavesMenu() {
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 99, 0, "Stay",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0));
        mgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        WowBuffer hello = gossipBody();
        hello.getU64();
        int menuId = hello.getU32();
        hello.getU32();
        int gossipItemCount = hello.getU32();
        int stay = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = hello.getU32();
            hello.getU8();
            hello.getU8();
            hello.getU32();
            String text = hello.getCString();
            hello.getCString();
            if ("Stay".equals(text)) {
                stay = index;
            }
        }
        assertTrue(stay >= 0);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, menuId, stay), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
    }

    @Test
    void gossipSelectWhenActionMenuNegativeShouldCloseGossip() {
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 98, 0, "Goodbye",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", -1));
        mgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        WowBuffer hello = gossipBody();
        hello.getU64();
        int menuId = hello.getU32();
        hello.getU32();
        int gossipItemCount = hello.getU32();
        int goodbye = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = hello.getU32();
            hello.getU8();
            hello.getU8();
            hello.getU32();
            String text = hello.getCString();
            hello.getCString();
            if ("Goodbye".equals(text)) {
                goodbye = index;
            }
        }
        assertTrue(goodbye >= 0);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, menuId, goodbye), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        assertEquals(0, last.get(Opcodes.SMSG_GOSSIP_COMPLETE).length);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    @Test
    void gossipSelectWhenActionPoiShouldSendGossipPoi() {
        ObjectMgr.PointOfInterest poi = ObjectMgr.lionsPrideInnPoi();
        mgr.pointsOfInterest.put(poi.entry(), poi);
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 97, 0, poi.iconName(),
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0, poi.entry()));
        mgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        WowBuffer hello = gossipBody();
        hello.getU64();
        int menuId = hello.getU32();
        hello.getU32();
        int gossipItemCount = hello.getU32();
        int chosen = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = hello.getU32();
            hello.getU8();
            hello.getU8();
            hello.getU32();
            String text = hello.getCString();
            hello.getCString();
            if (poi.iconName().equals(text)) {
                chosen = index;
            }
        }
        assertTrue(chosen >= 0);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, menuId, chosen), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_POI));
        assertEquals(0, ops.indexOf(Opcodes.SMSG_GOSSIP_POI));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_GOSSIP_POI));
        assertEquals(poi.flags(), b.getU32());
        assertEquals(poi.x(), b.getFloat(), 0.0001f);
        assertEquals(poi.y(), b.getFloat(), 0.0001f);
        assertEquals(poi.icon(), b.getU32());
        assertEquals(poi.data(), b.getU32());
        assertEquals(poi.iconName(), b.getCString());
        assertEquals(0, b.remaining());
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
    }

    @Test
    void gossipSelectWhenActionPoiMissingShouldSkipPacket() {
        List<ObjectMgr.GossipMenuItem> rows = new ArrayList<>(
                mgr.gossipOptions.get(Content.GOSSIP_MENU_FARLEY));
        rows.add(new ObjectMgr.GossipMenuItem(Content.GOSSIP_MENU_FARLEY, 96, 0, "Missing POI",
                Content.GOSSIP_OPTION_GOSSIP, Content.UNIT_NPC_FLAG_GOSSIP, 0, 0, "", 0, 99));
        mgr.gossipOptions.put(Content.GOSSIP_MENU_FARLEY, rows);
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        WowBuffer hello = gossipBody();
        hello.getU64();
        int menuId = hello.getU32();
        hello.getU32();
        int gossipItemCount = hello.getU32();
        int chosen = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = hello.getU32();
            hello.getU8();
            hello.getU8();
            hello.getU32();
            String text = hello.getCString();
            hello.getCString();
            if ("Missing POI".equals(text)) {
                chosen = index;
            }
        }
        assertTrue(chosen >= 0);
        ops.clear();
        content.gossipSelect(p, map, select(inn.guid, menuId, chosen), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_POI));
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
    }

    @Test
    void gossipSelectAuctioneerOpensHello() {
        Creature auctioneer = spawn(Content.NPC_AUCTIONEER_CHILTON, 0, 0);
        content.gossipHello(p, map, u64(auctioneer.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(auctioneer.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.MSG_AUCTION_HELLO));
        WowBuffer b = new WowBuffer(last.get(Opcodes.MSG_AUCTION_HELLO));
        assertEquals(auctioneer.guid, b.getU64());
        assertEquals(Content.AUCTION_HOUSE_HUMAN, b.getU32());
    }

    @Test
    void gossipSelectSpiritHealerWhenGhostShouldConfirm() {
        Creature healer = spiritHealer();
        p.setHealth(1);
        p.setGhost(true);
        content.gossipHello(p, map, u64(healer.guid), this::capture);
        ops.clear();
        last.clear();
        content.gossipSelect(p, map, select(healer.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM));
        assertEquals(healer.guid, b.getU64());
    }

    @Test
    void gossipSelectSpiritHealerWhenCorpseShouldConfirm() {
        Creature healer = spiritHealer();
        p.setHealth(0);
        content.gossipHello(p, map, u64(healer.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(healer.guid, 0, 0), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM));
    }

    @Test
    void gossipSelectSpiritHealerWhenAliveShouldStaySilent() {
        Creature healer = spiritHealer();
        p.setHealth(1);
        p.setGhost(true);
        content.gossipHello(p, map, u64(healer.guid), this::capture);
        p.setGhost(false);
        ops.clear();
        content.gossipSelect(p, map, select(healer.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_SPIRIT_HEALER_CONFIRM));
    }

    @Test
    void gossipHelloSpiritHealerWhenAliveShouldOmitOption() {
        Creature healer = spiritHealer();
        p.setHealth(50);
        content.gossipHello(p, map, u64(healer.guid), this::capture);
        WowBuffer b = gossipBody();
        b.getU64();
        b.getU32();
        b.getU32();
        assertEquals(0, b.getU32());
    }

    @Test
    void gossipHelloWhenGhostAtVendorShouldStaySilent() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setGhost(true);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        p.setGhost(false);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    @Test
    void gossipSelectIgnoresBadInput() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        Creature kobold = spawn(6, 0, 0);
        Creature banker = spawn(Content.NPC_OLIVIA_BURNSIDE, 0, 0);
        content.gossipSelect(p, map, new WowBuffer(3), this::capture);
        content.gossipSelect(p, map, select(vendor.guid, 0, 0), this::capture);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(0, 0, 0), this::capture);
        content.gossipSelect(p, map, select(99, 0, 0), this::capture);
        p.relocate(20, 0, 0, 0);
        content.gossipSelect(p, map, select(vendor.guid, 0, 0), this::capture);
        p.relocate(0, 0, 0, 0);
        content.gossipSelect(p, map, select(vendor.guid, 99, 0), this::capture);
        content.gossipSelect(p, map, select(vendor.guid, 0, 99), this::capture);
        content.gossipSelect(p, map, select(kobold.guid, 0, 0), this::capture);
        content.gossipHello(p, map, u64(banker.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(kobold.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_LIST_INVENTORY));
        assertFalse(ops.contains(Opcodes.SMSG_SHOW_BANK));
        Creature taxi = spawn(Content.NPC_DUNGAR_LONGDRINK, 0, 0);
        content.gossipHello(p, map, u64(taxi.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(kobold.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_SHOWTAXINODES));
        Creature auctioneer = spawn(Content.NPC_AUCTIONEER_CHILTON, 0, 0);
        content.gossipHello(p, map, u64(auctioneer.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(kobold.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.MSG_AUCTION_HELLO));
        assertFalse(ops.contains(Opcodes.SMSG_SHOWTAXINODES));
        assertFalse(ops.contains(Opcodes.SMSG_SHOW_BANK));
        assertFalse(ops.contains(Opcodes.SMSG_LIST_INVENTORY));
        assertFalse(ops.contains(Opcodes.SMSG_TRAINER_LIST));
        assertFalse(ops.contains(Opcodes.SMSG_BINDER_CONFIRM));
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        content.gossipHello(p, map, u64(inn.guid), this::capture);
        int[] innChoice = gossipMenuAndListByIcon(Content.GOSSIP_ICON_INTERACT_2);
        ops.clear();
        content.gossipSelect(p, map, select(kobold.guid, innChoice[0], innChoice[1]), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_BINDER_CONFIRM));
        p.clazz = 1;
        Creature trainer = spawn(Content.NPC_LLANE_BESHERE, 0, 0);
        content.gossipHello(p, map, u64(trainer.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(kobold.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_TRAINER_LIST));
        mgr.creatures.put(9002, new ObjectMgr.CreatureTemplate(9002, "Petitioner", 0, 12, 100, 1,
                262144, "", "", 0));
        Creature petitioner = spawn(9002, 0, 0);
        content.gossipHello(p, map, u64(petitioner.guid), this::capture);
        ops.clear();
        content.gossipSelect(p, map, select(petitioner.guid, 0, 0), this::capture);
        assertFalse(ops.contains(Opcodes.MSG_AUCTION_HELLO));
    }

    @Test
    void vendorBuyPushAndGold() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setMoney(70);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        content.listInventory(p, map, u64(vendor.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_LIST_INVENTORY));
        ops.clear();
        content.buy(p, map, buy(vendor.guid, Content.ITEM_WORN_SHORTSWORD, 1), false, nextItem++, this::capture);
        assertEquals(35, p.money);
        assertEquals(1, p.items.size());
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        ops.clear();
        content.buy(p, map, buyInSlot(vendor.guid, Content.ITEM_WORN_SHORTSWORD, p.guid, 23, 1), true, nextItem++, this::capture);
        assertEquals(0, p.money);
        assertEquals(2, p.items.size());
    }

    @Test
    void vendorBuyInSlotWhenFreeOrPastBackpackEndShouldHonorOrFallBack() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setMoney(200);
        content.buy(p, map, buy(vendor.guid, Content.ITEM_WORN_SHORTSWORD, 1), false, nextItem++, this::capture);
        assertEquals(1, p.items.size());
        assertEquals(Content.BACKPACK_START, p.items.values().iterator().next().slot);
        ops.clear();
        content.buy(p, map, buyInSlot(vendor.guid, Content.ITEM_WORN_SHORTSWORD, p.guid, 24, 1), true, nextItem++, this::capture);
        assertEquals(2, p.items.size());
        assertTrue(p.items.values().stream().anyMatch(it -> it.slot == 24));
        content.buy(p, map, buyInSlot(vendor.guid, Content.ITEM_WORN_SHORTSWORD, p.guid, Content.BACKPACK_END, 1),
                true, nextItem++, this::capture);
        assertEquals(3, p.items.size());
        assertTrue(p.items.values().stream().anyMatch(it -> it.slot == 25));
    }

    @Test
    void questAcceptQueryComplete() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.gossipHello(p, map, u64(giver.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        content.queryQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        content.queryQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, p.questLogId[0]);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        ops.clear();
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        assertEquals(Content.QUEST_BROTHERHOOD_OF_THIEVES, p.questLogId[1]);
        ops.clear();
        content.questGiverStatusQuery(p, map, u64(giver.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, last.get(Opcodes.SMSG_QUESTGIVER_STATUS)[8] & 0xFF);
        ops.clear();
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        ops.clear();
        content.completeQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), nextItem++, this::capture);
        assertEquals(0, p.questLogId[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
    }

    @Test
    void removeQuestWhenAcceptedShouldClearSlotAndSendUpdate() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, p.questLogId[0]);
        ops.clear();
        content.removeQuest(p, slot(0), this::capture);
        assertEquals(0, p.questLogId[0]);
        assertTrue(sentUpdate());
    }

    @Test
    void removeQuestWhenEmptySlotShouldStillSendUpdate() {
        content.removeQuest(p, slot(0), this::capture);
        assertEquals(0, p.questLogId[0]);
        assertTrue(sentUpdate());
    }

    @Test
    void ignoresTruncationRangeAndWrongNpc() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        Creature kobold = spawn(6, 0, 0);
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.gossipHello(p, map, new WowBuffer(3), this::capture);
        content.gossipSelect(p, map, new WowBuffer(3), this::capture);
        content.listInventory(p, map, new WowBuffer(3), this::capture);
        content.buy(p, map, new WowBuffer(8), false, nextItem++, this::capture);
        content.queryQuest(p, map, new WowBuffer(8), this::capture);
        content.requestReward(p, map, new WowBuffer(8), this::capture);
        content.acceptQuest(p, map, new WowBuffer(8), this::capture);
        content.removeQuest(p, new WowBuffer(0), this::capture);
        content.removeQuest(p, slot(Content.MAX_QUEST_LOG_SIZE), this::capture);
        content.completeQuest(p, map, new WowBuffer(8), nextItem++, this::capture);
        content.gossipHello(p, map, u64(0), this::capture);
        content.gossipHello(p, map, u64(99), this::capture);
        content.listInventory(p, map, u64(0), this::capture);
        content.listInventory(p, map, u64(99), this::capture);
        content.queryQuest(p, map, quest(99, 783), this::capture);
        content.acceptQuest(p, map, quest(99, 783), this::capture);
        content.completeQuest(p, map, quest(99, 783), nextItem++, this::capture);
        content.requestReward(p, map, quest(99, 783), this::capture);
        content.requestReward(p, map, quest(turn.guid, 783), this::capture);
        p.relocate(20, 0, 0, 0);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        content.listInventory(p, map, u64(vendor.guid), this::capture);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        content.queryQuest(p, map, quest(giver.guid, 783), this::capture);
        content.acceptQuest(p, map, quest(giver.guid, 783), this::capture);
        content.completeQuest(p, map, quest(turn.guid, 783), nextItem++, this::capture);
        content.requestReward(p, map, quest(turn.guid, 783), this::capture);
        p.relocate(0, 0, 0, 0);
        content.listInventory(p, map, u64(kobold.guid), this::capture);
        content.buy(p, map, buy(kobold.guid, 25, 1), false, nextItem++, this::capture);
        content.buy(p, map, buy(99, 25, 1), false, nextItem++, this::capture);
        content.queryQuest(p, map, quest(kobold.guid, 783), this::capture);
        content.acceptQuest(p, map, quest(turn.guid, 783), this::capture);
        content.completeQuest(p, map, quest(giver.guid, 783), nextItem++, this::capture);
        content.requestReward(p, map, quest(giver.guid, 783), this::capture);
        content.completeQuest(p, map, quest(turn.guid, 783), nextItem++, this::capture);
        assertTrue(ops.isEmpty());
        assertTrue(p.items.isEmpty());
    }

    @Test
    void buyFailuresAndSlotSkip() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setMoney(50);
        content.buy(p, map, buy(vendor.guid, 26, 1), false, nextItem++, this::capture);
        mgr.vendorItems.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(25, 99999)));
        content.buy(p, map, buy(vendor.guid, 99999, 1), false, nextItem++, this::capture);
        p.setMoney(0);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        assertEquals(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE, ops.get(ops.size() - 1).intValue());
        assertEquals(Content.EQUIP_ERR_NOT_ENOUGH_MONEY, last.get(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE)[0] & 0xFF);
        p.setMoney(100);
        fillBackpack();
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        assertEquals(16, p.items.size());
        p.items.clear();
        Item otherBag = new Item(50, 25);
        otherBag.bag = 1;
        otherBag.slot = 23;
        p.items.put(50, otherBag);
        ops.clear();
        content.buy(p, map, buyExact12(vendor.guid, 25), false, nextItem++, this::capture);
        assertEquals(2, p.items.size());
        ops.clear();
        p.setMoney(100);
        content.buy(p, map, buyCount(vendor.guid, 25, 0), false, nextItem++, this::capture);
        content.buy(p, map, buyShortInSlot(vendor.guid, 25), true, nextItem++, this::capture);
        mgr.vendorItems.remove(Content.NPC_CORINA_STEELE);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
    }

    @Test
    void questLogFullMissingTemplateAndEncoders() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        Creature emptyVendor = vendor(9001);
        Creature gossipOnly = spawn(Content.NPC_MARSHAL_DUGHAN, 0, 0);
        mgr.questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN, 1)));
        content.acceptQuest(p, map, quest(giver.guid, 1), this::capture);
        for (int i = 0; i < 25; i++) {
            p.questLogId[i] = 100 + i;
        }
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTLOG_FULL));
        p.questLogId[24] = Content.QUEST_A_THREAT_WITHIN;
        mgr.quests.remove(Content.QUEST_A_THREAT_WITHIN);
        content.completeQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), nextItem++, this::capture);
        content.queryQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        mgr.questGivers.put(Content.NPC_MARSHAL_DUGHAN, new ArrayList<>(List.of(404)));
        content.gossipHello(p, map, u64(gossipOnly.guid), this::capture);
        content.listInventory(p, map, u64(emptyVendor.guid), this::capture);
        mgr.vendorItems.put(9001, new ArrayList<>(List.of(99999)));
        content.listInventory(p, map, u64(emptyVendor.guid), this::capture);
        assertTrue(content.gives(Content.NPC_DEPUTY_WILLEM, Content.QUEST_A_THREAT_WITHIN)
                || content.involves(Content.NPC_MARSHAL_MCBRIDE, Content.QUEST_A_THREAT_WITHIN));
        assertTrue(content.offersOrInvolves(Content.NPC_DEPUTY_WILLEM, Content.QUEST_A_THREAT_WITHIN));
        assertTrue(content.offersOrInvolves(Content.NPC_MARSHAL_MCBRIDE, Content.QUEST_A_THREAT_WITHIN));
        assertFalse(content.offersOrInvolves(6, Content.QUEST_A_THREAT_WITHIN));
        assertFalse(content.gives(6, 783));
        assertFalse(content.gives(Content.NPC_DEPUTY_WILLEM, 404));
        assertFalse(content.involves(6, 783));
        assertFalse(content.involves(Content.NPC_MARSHAL_MCBRIDE, 404));
        assertEquals(-1, Content.slotOf(p, 1));
        assertEquals(24, Content.slotOf(p, Content.QUEST_A_THREAT_WITHIN));
        assertEquals(-1, Content.freeSlot(p));
        p.questLogId[0] = 0;
        assertEquals(0, Content.freeSlot(p));
        assertTrue(Content.outOfRange(p, farCreature()));
        assertFalse(Content.outOfRange(p, emptyVendor));
        assertNull(Content.creature(map, 0));
    }

    private boolean sentUpdate() {
        return ops.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT);
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
    }

    private WowBuffer gossipBody() {
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        return new WowBuffer(last.get(Opcodes.SMSG_GOSSIP_MESSAGE));
    }

    private int[] gossipMenuAndListByIcon(int icon) {
        WowBuffer b = gossipBody();
        b.getU64();
        int menuId = b.getU32();
        b.getU32();
        int gossipItemCount = b.getU32();
        int listId = -1;
        for (int i = 0; i < gossipItemCount; i++) {
            int index = b.getU32();
            if (b.getU8() == icon) {
                listId = index;
            }
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        assertTrue(listId >= 0);
        return new int[] { menuId, listId };
    }

    private Creature spawn(int entry, float x, float y) {
        Creature c = mgr.spawnCreature(entry, 0, x, y, 0, 0, null);
        map.add(c);
        return c;
    }

    private Creature spiritHealer() {
        mgr.creatures.put(6491, new ObjectMgr.CreatureTemplate(6491, "Spirit Healer", 0, 35, 100, 60,
                Content.UNIT_NPC_FLAG_GOSSIP | Content.UNIT_NPC_FLAG_SPIRITHEALER, "", "", 0));
        return spawn(6491, 0, 0);
    }

    private Creature vendor(int entry) {
        mgr.creatures.put(entry, new ObjectMgr.CreatureTemplate(entry, "Vendor", 0, 12, 100, 1,
                Content.UNIT_NPC_FLAG_VENDOR, "", "", 0));
        return spawn(entry, 0, 0);
    }

    private Creature farCreature() {
        Creature c = new Creature();
        c.guid = 8;
        c.relocate(40, 0, 0, 0);
        return c;
    }

    private void fillBackpack() {
        for (int slot = Content.BACKPACK_START; slot < Content.BACKPACK_END; slot++) {
            Item it = new Item(100 + slot, 25);
            it.bag = 0;
            it.slot = slot;
            p.items.put(Guid.low(it.guid), it);
        }
    }

    private static WowBuffer u64(long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        return b;
    }

    private static WowBuffer select(long guid, int menuId, int listId) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU32(menuId);
        b.putU32(listId);
        return b;
    }

    private static WowBuffer buy(long vendor, int itemId, int count) {
        WowBuffer b = new WowBuffer(14);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU8(count);
        b.putU8(1);
        return b;
    }

    private static WowBuffer buyCount(long vendor, int itemId, int count) {
        return buy(vendor, itemId, count);
    }

    private static WowBuffer buyExact12(long vendor, int itemId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(vendor);
        b.putU32(itemId);
        return b;
    }

    private static WowBuffer buyShortInSlot(long vendor, int itemId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(vendor);
        b.putU32(itemId);
        return b;
    }

    private static WowBuffer buyInSlot(long vendor, int itemId, long bagGuid, int bagSlot, int count) {
        WowBuffer b = new WowBuffer(22);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU64(bagGuid);
        b.putU8(bagSlot);
        b.putU8(count);
        return b;
    }

    private static WowBuffer slot(int slot) {
        WowBuffer b = new WowBuffer(1);
        b.putU8(slot);
        return b;
    }

    private static WowBuffer quest(long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        return b;
    }

    private static WowBuffer chooseReward(long guid, int questId, int reward) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU32(questId);
        b.putU32(reward);
        return b;
    }
}
