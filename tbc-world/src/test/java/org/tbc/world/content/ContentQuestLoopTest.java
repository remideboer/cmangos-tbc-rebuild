package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.zip.Inflater;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dialog marks, every objective slot, and who may take or turn in a quest. */
class ContentQuestLoopTest {
    private ObjectMgr mgr;
    private Content content;
    private GameMap map;
    private Player p;
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();

    @BeforeEach
    void setUp() {
        mgr = new ObjectMgr();
        mgr.load(null, null);
        content = new Content(mgr);
        map = new GameMap(0, 0);
        p = new Player();
        p.guid = 1;
        p.race = 1;
        p.level = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
    }

    @Test
    void dialogStatusWhenTakenShouldShowIncompleteThenReward() {
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        content.acceptQuest(p, map, quest(mcbride.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        content.questGiverStatusQuery(p, map, u64(mcbride.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_INCOMPLETE, statusByte());

        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.questGiverStatusQuery(p, map, u64(willem.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());

        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        content.questGiverStatusQuery(p, map, u64(mcbride.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_REWARD, statusByte());
        content.questGiverStatusQuery(p, map, u64(willem.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
    }

    @Test
    void dialogStatusWhenLevelRacePrevOrRewardedShouldStayNone() {
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY);
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0));
        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
        p.level = 2;
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());

        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
        p.race = 2;
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());

        p.race = 0;
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());

        p.race = 1;
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, Content.QUEST_A_THREAT_WITHIN));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
        p.rewardedQuests.add(Content.QUEST_A_THREAT_WITHIN);
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());

        p.rewardedQuests.clear();
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -Content.QUEST_KOBOLD_CAMP_CLEANUP));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
        p.questLogId[0] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());

        p.rewardedQuests.add(Content.QUEST_REST_AND_RELAXATION);
        p.questLogId[0] = 0;
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
    }

    @Test
    void killedMonsterCreditWhenSecondCreatureStillNeededShouldNotComplete() {
        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, Content.NPC_KOBOLD_VERMIN, 1, Content.NPC_MARSHAL_DUGHAN, 1,
                0, 0, 0, 0, 1, 0, 0));
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        content.acceptQuest(p, map, quest(mcbride.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        content.killedMonsterCredit(p, spawn(Content.NPC_KOBOLD_VERMIN), this::capture);
        assertEquals(0, p.questLogState[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        ops.clear();
        content.killedMonsterCredit(p, spawn(Content.NPC_CORINA_STEELE), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        content.killedMonsterCredit(p, spawn(Content.NPC_MARSHAL_DUGHAN), this::capture);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        WowBuffer add = new WowBuffer(last.get(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
        assertEquals(Content.QUEST_KOBOLD_CAMP_CLEANUP, add.getU32());
        assertEquals(Content.NPC_MARSHAL_DUGHAN, add.getU32());
        assertEquals(1, add.getU32());
        assertEquals(1, add.getU32());
    }

    @Test
    void itemAddedQuestCheckWhenSecondItemStillNeededShouldNotComplete() {
        mgr.quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, questTemplate(
                Content.QUEST_BROTHERHOOD_OF_THIEVES, 0, 0, 0, 0,
                Content.ITEM_RED_BURLAP_BANDANA, 1, Content.ITEM_WORN_SHORTSWORD, 1, 1, 0, 0));
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        content.itemAddedQuestCheck(p, Content.ITEM_RED_BURLAP_BANDANA, 1, this::capture);
        assertEquals(0, p.questLogState[0]);
        content.itemAddedQuestCheck(p, Content.ITEM_WORN_SHORTSWORD, 1, this::capture);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertEquals(1, p.questLogItemCount[0][1]);
    }

    @Test
    void completeQuestWhenObjectivesShortShouldNotReward() {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        content.completeQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), 1, this::capture);
        content.requestReward(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
        assertFalse(ops.contains(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        assertFalse(p.rewardedQuests.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES));
        p.questLogItemCount[0][0] = 12;
        content.completeQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), 1, this::capture);
        assertTrue(p.rewardedQuests.contains(Content.QUEST_BROTHERHOOD_OF_THIEVES));
        assertEquals(0, p.questLogId[0]);
    }

    @Test
    void gossipWhenCannotTakeShouldOmitQuestAndKeepTurnIn() {
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        content.gossipHello(p, map, u64(mcbride.guid), this::capture);
        assertTrue(gossipQuestIds().contains(Content.QUEST_KOBOLD_CAMP_CLEANUP));
        assertFalse(gossipQuestIds().contains(Content.QUEST_A_THREAT_WITHIN));
        ops.clear();
        last.clear();
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.gossipHello(p, map, u64(willem.guid), this::capture);
        assertTrue(gossipQuestIds().contains(Content.QUEST_A_THREAT_WITHIN));
        assertFalse(gossipQuestIds().contains(Content.QUEST_BROTHERHOOD_OF_THIEVES));

        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        last.clear();
        content.gossipHello(p, map, u64(willem.guid), this::capture);
        assertFalse(gossipQuestIds().contains(Content.QUEST_A_THREAT_WITHIN));

        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        ops.clear();
        last.clear();
        content.gossipHello(p, map, u64(willem.guid), this::capture);
        assertTrue(gossipQuestIds().contains(Content.QUEST_BROTHERHOOD_OF_THIEVES));

        ops.clear();
        last.clear();
        content.gossipHello(p, map, u64(mcbride.guid), this::capture);
        assertTrue(gossipQuestIds().contains(Content.QUEST_A_THREAT_WITHIN));
        assertTrue(gossipQuestIds().contains(Content.QUEST_KOBOLD_CAMP_CLEANUP));
    }

    @Test
    void gossipQuestRowWhenAvailableShouldUseDialogStatusIcon() {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.gossipHello(p, map, u64(willem.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, gossipQuestIcon(Content.QUEST_A_THREAT_WITHIN));
    }

    @Test
    void gossipQuestRowWhenTakenShouldUseIncompleteOrRewardIcon() {
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        content.acceptQuest(p, map, quest(mcbride.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        ops.clear();
        last.clear();
        content.gossipHello(p, map, u64(mcbride.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_INCOMPLETE, gossipQuestIcon(Content.QUEST_KOBOLD_CAMP_CLEANUP));

        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        last.clear();
        content.gossipHello(p, map, u64(mcbride.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_REWARD, gossipQuestIcon(Content.QUEST_A_THREAT_WITHIN));
    }

    @Test
    void queryQuestWhenRewardsExistShouldListChoiceAndRewardItems() {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.queryQuest(p, map, quest(willem.guid, Content.QUEST_BROTHERHOOD_OF_THIEVES), this::capture);
        WowBuffer details = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        details.getU64();
        assertEquals(Content.QUEST_BROTHERHOOD_OF_THIEVES, details.getU32());
        details.getCString();
        details.getCString();
        details.getCString();
        assertEquals(1, details.getU32());
        assertEquals(0, details.getU32());
        assertEquals(2, details.getU32());
        assertEquals(Content.ITEM_MILITIA_DAGGER, details.getU32());
        assertEquals(1, details.getU32());
        details.getU32();
        assertEquals(Content.ITEM_MILITIA_HAMMER, details.getU32());
        assertEquals(1, details.getU32());

        ops.clear();
        last.clear();
        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY);
        content.queryQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        WowBuffer reward = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        reward.getU64();
        reward.getU32();
        reward.getCString();
        reward.getCString();
        reward.getCString();
        reward.getU32();
        reward.getU32();
        assertEquals(0, reward.getU32());
        assertEquals(1, reward.getU32());
        assertEquals(Content.ITEM_REFRESHING_SPRING_WATER, reward.getU32());
        assertEquals(5, reward.getU32());
    }

    @Test
    void acceptQuestWhenTakenShouldSendQuestLogId() throws Exception {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, questLogIdOnWire());
    }

    @Test
    void objectivesMetWhenCountIsZeroOrAlreadyFilledShouldPassThatSlot() {
        ObjectMgr.QuestTemplate zeroNeed = questTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                Content.NPC_KOBOLD_VERMIN, 0, 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 0, 0, 0, 1, 0, 0);
        assertTrue(content.objectivesMet(p, 0, zeroNeed));
        ObjectMgr.QuestTemplate filled = questTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                Content.NPC_KOBOLD_VERMIN, 1, 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 1, 0, 0, 1, 0, 0);
        p.questLogCounts[0][0] = 1;
        p.questLogItemCount[0][0] = 1;
        assertTrue(content.objectivesMet(p, 0, filled));
        p.questLogCounts[0][0] = 0;
        assertFalse(content.objectivesMet(p, 0, filled));
        p.questLogCounts[0][0] = 1;
        p.questLogItemCount[0][0] = 0;
        assertFalse(content.objectivesMet(p, 0, filled));
        assertTrue(content.objectivesMet(p, 0, mgr.quests.get(Content.QUEST_A_THREAT_WITHIN)));
    }

    private int statusByte() {
        return last.get(Opcodes.SMSG_QUESTGIVER_STATUS)[8] & 0xFF;
    }

    private int gossipQuestIcon(int questId) {
        WowBuffer b = gossipMenu();
        int n = b.getU32();
        for (int i = 0; i < n; i++) {
            int id = b.getU32();
            int icon = b.getU32();
            b.getU32();
            b.getCString();
            if (id == questId) {
                return icon;
            }
        }
        return -1;
    }

    private int questLogIdOnWire() throws Exception {
        byte[] raw = last.get(Opcodes.SMSG_UPDATE_OBJECT);
        if (raw == null) {
            raw = inflate(last.get(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        }
        WowBuffer b = new WowBuffer(raw);
        b.getU32();
        b.getU8();
        assertEquals(UpdateBuilder.UPDATETYPE_VALUES, b.getU8());
        b.getPackedGuid();
        int nblocks = b.getU8() & 0xFF;
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int field = UpdateFields.PLAYER_QUEST_LOG_1_1;
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) == 0) {
                continue;
            }
            int value = b.getU32();
            if (i == field) {
                return value;
            }
        }
        return 0;
    }

    private static byte[] inflate(byte[] compressed) throws Exception {
        int size = compressed[0] & 0xFF
                | ((compressed[1] & 0xFF) << 8)
                | ((compressed[2] & 0xFF) << 16)
                | ((compressed[3] & 0xFF) << 24);
        Inflater inf = new Inflater();
        inf.setInput(compressed, 4, compressed.length - 4);
        byte[] out = new byte[size];
        inf.inflate(out);
        inf.end();
        return out;
    }

    private WowBuffer gossipMenu() {
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_GOSSIP_MESSAGE));
        b.getU64();
        b.getU32();
        b.getU32();
        int items = b.getU32();
        for (int i = 0; i < items; i++) {
            b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        return b;
    }

    private List<Integer> gossipQuestIds() {
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_GOSSIP_MESSAGE));
        b.getU64();
        b.getU32();
        b.getU32();
        int items = b.getU32();
        for (int i = 0; i < items; i++) {
            b.getU32();
            b.getU8();
            b.getU8();
            b.getU32();
            b.getCString();
            b.getCString();
        }
        int n = b.getU32();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(b.getU32());
            b.getU32();
            b.getU32();
            b.getCString();
        }
        return ids;
    }

    private Creature spawn(int entry) {
        Creature c = mgr.spawnCreature(entry, 0, 0, 0, 0, 0, null);
        map.add(c);
        return c;
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
    }

    private static WowBuffer u64(long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        return b;
    }

    private static WowBuffer quest(long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        return b;
    }

    private static ObjectMgr.QuestTemplate questTemplate(int id, int creature1, int creatureCount1, int creature2,
                                                         int creatureCount2, int item1, int itemCount1, int item2,
                                                         int itemCount2, int minLevel, int races, int prevQuestId) {
        return new ObjectMgr.QuestTemplate(id, "Quest", minLevel, 0, 0, "details", "objectives",
                creature1, creatureCount1, item1, itemCount1, 1, 0, 0, 0, 0, 0, 0, 0,
                creature2, creatureCount2, 0, 0, 0, 0,
                item2, itemCount2, 0, 0, 0, 0, prevQuestId, races);
    }

    @Test
    void gameObjectQuestgiverWhenUsedShouldOfferAndAccept() {
        GameObject poster = go(Content.GO_ICE_STONE);
        mgr.goQuestGivers.put(Content.GO_ICE_STONE, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        content.questGiverStatusQuery(p, map, u64(poster.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_AVAILABLE, statusByte());
        content.useGameObject(p, map, poster, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        content.acceptQuest(p, map, quest(poster.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertEquals(Content.QUEST_REST_AND_RELAXATION, p.questLogId[0]);
    }

    @Test
    void useGameObjectWhenObjectiveEntryShouldCreditNegativeId() {
        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, -Content.GO_ICE_BLOCK, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[0] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        GameObject block = go(Content.GO_ICE_BLOCK);
        content.useGameObject(p, map, block, this::capture);
        assertEquals(1, p.questLogCounts[0][0]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
    }

    @Test
    void exploreWhenAreaTriggerMatchesShouldCompleteEventObjective() {
        mgr.quests.put(Content.QUEST_A_THREAT_WITHIN, questTemplate(
                Content.QUEST_A_THREAT_WITHIN, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_A_THREAT_WITHIN, ObjectMgr.QuestExtras.explore());
        mgr.areaTriggerQuests.put(45, Content.QUEST_A_THREAT_WITHIN);
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        content.exploreAreaTrigger(p, 45, this::capture);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
    }

    @Test
    void spellCastWhenRequiredShouldCreditObjective() {
        mgr.quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, questTemplate(
                Content.QUEST_BROTHERHOOD_OF_THIEVES, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, ObjectMgr.QuestExtras.spell(635));
        p.questLogId[0] = Content.QUEST_BROTHERHOOD_OF_THIEVES;
        content.spellCastCredit(p, 635, this::capture);
        assertEquals(1, p.questLogCounts[0][0]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
    }

    @Test
    void acceptWhenReputationTooLowShouldRefuseAndRewardShouldGrantStanding() {
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY);
        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_REST_AND_RELAXATION, ObjectMgr.QuestExtras.reputation(72, 100, 72, 50));
        content.questGiverStatusQuery(p, map, u64(farley.guid), this::capture);
        assertEquals(Content.DIALOG_STATUS_NONE, statusByte());
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertEquals(0, p.questLogId[0]);
        p.modifyReputation(72, 50);
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertEquals(Content.QUEST_REST_AND_RELAXATION, p.questLogId[0]);
        p.questLogState[0] = Content.QUEST_STATE_COMPLETE;
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), 1, this::capture);
        assertEquals(150, p.reputationStanding(72));
    }

    @Test
    void escortWhenPlayerReachesPointShouldComplete() {
        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        mgr.questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN)));
        mgr.quests.put(Content.QUEST_A_THREAT_WITHIN, new ObjectMgr.QuestTemplate(
                Content.QUEST_A_THREAT_WITHIN, "Escort", 1, Content.QUEST_TYPE_ESCORT));
        mgr.questExtras.put(Content.QUEST_A_THREAT_WITHIN, ObjectMgr.QuestExtras.point(0, 10f, 0f));
        content.acceptQuest(p, map, quest(willem.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(p.guid, willem.followTarget);
        p.relocate(10, 0, 0, 0);
        content.tickEscort(p, map, this::capture);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
    }

    @Test
    void timerWhenExpiredShouldFailAndCompletedQuestShouldStay() {
        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, ObjectMgr.QuestExtras.limit(60));
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        mgr.questGivers.put(Content.NPC_MARSHAL_MCBRIDE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        content.acceptQuest(p, map, quest(mcbride.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        long expiry = p.questExpiry[0];
        content.failExpired(p, expiry - 1, this::capture);
        assertEquals(0, p.questLogState[0]);
        content.failExpired(p, expiry, this::capture);
        assertEquals(Content.QUEST_STATE_FAIL, p.questLogState[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_FAILEDTIMER));
        p.questLogState[0] = Content.QUEST_STATE_COMPLETE;
        content.failExpired(p, expiry + 60_000, this::capture);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
    }

    @Test
    void dailyWhenRewardedShouldRefuseUntilReset() {
        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY);
        mgr.questGivers.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_REST_AND_RELAXATION, ObjectMgr.QuestExtras.daily());
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        p.questLogState[0] = Content.QUEST_STATE_COMPLETE;
        content.completeQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), 1, this::capture);
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertFalse(containsQuest(Content.QUEST_REST_AND_RELAXATION));
        content.resetDailies(p);
        content.acceptQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertTrue(containsQuest(Content.QUEST_REST_AND_RELAXATION));
    }

    @Test
    void objectiveCreditWhenTheConditionDoesNotMatchShouldLeaveTheLog() {
        content.questGiverStatusQuery(p, map, u64(999), this::capture);
        content.acceptQuest(p, map, quest(999, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertFalse(content.useGameObject(p, map, null, this::capture));
        GameObject far = go(Content.GO_ICE_STONE);
        far.relocate(100, 0, 0, 0);
        assertFalse(content.useGameObject(p, map, far, this::capture));
        content.acceptQuest(p, map, quest(far.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);

        GameObject plain = go(Content.GO_ICE_BLOCK);
        mgr.goQuestGivers.put(Content.GO_ICE_BLOCK, new ArrayList<>());
        assertFalse(content.useGameObject(p, map, plain, this::capture));
        mgr.goQuestGivers.put(Content.GO_ICE_BLOCK, new ArrayList<>(List.of(99, Content.QUEST_REST_AND_RELAXATION)));
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 80, 0, 0));
        assertFalse(content.useGameObject(p, map, plain, this::capture));
        content.acceptQuest(p, map, quest(plain.guid, 7), this::capture);

        content.exploreAreaTrigger(p, 99, this::capture);
        mgr.areaTriggerQuests.put(45, Content.QUEST_A_THREAT_WITHIN);
        content.exploreAreaTrigger(p, 45, this::capture);
        p.questLogId[0] = Content.QUEST_A_THREAT_WITHIN;
        content.exploreAreaTrigger(p, 45, this::capture);
        mgr.questExtras.put(Content.QUEST_A_THREAT_WITHIN, ObjectMgr.QuestExtras.spell(1));
        content.exploreAreaTrigger(p, 45, this::capture);
        mgr.questExtras.put(Content.QUEST_A_THREAT_WITHIN, ObjectMgr.QuestExtras.explore());
        p.questLogCounts[0][0] = 1;
        content.exploreAreaTrigger(p, 45, this::capture);
        p.questLogCounts[0][0] = 0;
        assertFalse(content.objectivesMet(p, 0, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, -Content.GO_ICE_BLOCK, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0)));

        mgr.quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, questTemplate(
                Content.QUEST_BROTHERHOOD_OF_THIEVES, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[1] = Content.QUEST_BROTHERHOOD_OF_THIEVES;
        content.spellCastCredit(p, 635, this::capture);
        mgr.questExtras.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, ObjectMgr.QuestExtras.spell(635));
        p.questLogState[1] = Content.QUEST_STATE_COMPLETE;
        content.spellCastCredit(p, 635, this::capture);
        p.questLogState[1] = 0;
        p.questLogCounts[1][0] = 1;
        content.spellCastCredit(p, 635, this::capture);
        assertTrue(content.objectivesMet(p, 1, mgr.quests.get(Content.QUEST_BROTHERHOOD_OF_THIEVES)));

        Creature willem = spawn(Content.NPC_DEPUTY_WILLEM);
        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, new ObjectMgr.QuestTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, "Escort", 1, 0));
        p.questLogId[2] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        content.tickEscort(p, map, this::capture);
        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, new ObjectMgr.QuestTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, "Escort", 1, Content.QUEST_TYPE_ESCORT));
        mgr.questExtras.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, ObjectMgr.QuestExtras.point(0, 10f, 0f));
        content.tickEscort(p, map, this::capture);
        p.relocate(10, 0, 0, 0);
        p.questLogCounts[2][0] = 1;
        content.tickEscort(p, map, this::capture);

        p.questLogId[3] = Content.QUEST_REST_AND_RELAXATION;
        content.failExpired(p, 1, this::capture);
        mgr.quests.put(99, null);
        mgr.questExtras.put(99, ObjectMgr.QuestExtras.spell(635));
        p.questLogId[4] = 99;
        content.spellCastCredit(p, 635, this::capture);
        content.tickEscort(p, map, this::capture);

        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, -Content.GO_ICE_BLOCK, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[0] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        p.questLogState[0] = 0;
        p.questLogCounts[0][0] = 0;
        GameObject block = go(Content.GO_ICE_BLOCK);
        content.useGameObject(p, map, block, this::capture);
        assertEquals(0, p.questLogState[0]);
        p.questLogCounts[0][0] = 2;
        content.useGameObject(p, map, block, this::capture);
        assertEquals(0, willem.followTarget);
    }

    @Test
    void objectiveBranchesWhenExtrasAndCreditsVaryShouldCoverPaths() {
        mgr.quests.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, questTemplate(
                Content.QUEST_BROTHERHOOD_OF_THIEVES, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_BROTHERHOOD_OF_THIEVES, ObjectMgr.QuestExtras.spell(635));
        p.questLogId[0] = Content.QUEST_BROTHERHOOD_OF_THIEVES;
        assertFalse(content.objectivesMet(p, 0, mgr.quests.get(Content.QUEST_BROTHERHOOD_OF_THIEVES)));

        mgr.quests.put(Content.QUEST_A_THREAT_WITHIN, new ObjectMgr.QuestTemplate(
                Content.QUEST_A_THREAT_WITHIN, "Escort", 1, Content.QUEST_TYPE_ESCORT));
        mgr.questExtras.put(Content.QUEST_A_THREAT_WITHIN, ObjectMgr.QuestExtras.point(0, 50f, 50f));
        p.questLogId[1] = Content.QUEST_A_THREAT_WITHIN;
        assertFalse(content.objectivesMet(p, 1, mgr.quests.get(Content.QUEST_A_THREAT_WITHIN)));

        mgr.areaTriggerQuests.put(77, Content.QUEST_REST_AND_RELAXATION);
        content.exploreAreaTrigger(p, 77, this::capture);

        p.questLogId[2] = Content.QUEST_REST_AND_RELAXATION;
        p.questLogState[2] = Content.QUEST_STATE_COMPLETE;
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, new ObjectMgr.QuestTemplate(
                Content.QUEST_REST_AND_RELAXATION, "Escort", 1, Content.QUEST_TYPE_ESCORT));
        mgr.questExtras.put(Content.QUEST_REST_AND_RELAXATION, ObjectMgr.QuestExtras.point(0, 0f, 0f));
        content.tickEscort(p, map, this::capture);
        p.questLogState[2] = 0;
        mgr.questExtras.remove(Content.QUEST_REST_AND_RELAXATION);
        content.tickEscort(p, map, this::capture);

        GameObject stone = go(Content.GO_ICE_STONE);
        mgr.goQuestGivers.put(Content.GO_ICE_STONE, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        content.acceptQuest(p, map, quest(stone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);

        mgr.quests.put(Content.QUEST_KOBOLD_CAMP_CLEANUP, questTemplate(
                Content.QUEST_KOBOLD_CAMP_CLEANUP, -Content.GO_ICE_BLOCK, 3, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[3] = Content.QUEST_KOBOLD_CAMP_CLEANUP;
        p.questLogState[3] = Content.QUEST_STATE_COMPLETE;
        GameObject block = go(Content.GO_ICE_BLOCK);
        content.useGameObject(p, map, block, this::capture);
        p.questLogState[3] = 0;
        p.questLogCounts[3][0] = 0;
        content.useGameObject(p, map, block, this::capture);
        assertEquals(1, p.questLogCounts[3][0]);
        assertEquals(0, p.questLogState[3]);
        content.useGameObject(p, map, block, this::capture);
        assertEquals(2, p.questLogCounts[3][0]);

        mgr.goQuestInvolved.put(Content.GO_ICE_STONE, new ArrayList<>(List.of(Content.QUEST_KOBOLD_CAMP_CLEANUP)));
        p.questLogCounts[3][0] = 3;
        p.questLogState[3] = Content.QUEST_STATE_COMPLETE;
        content.requestReward(p, map, quest(stone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD));
        content.completeQuest(p, map, quest(stone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), 1, this::capture);
        assertTrue(p.rewardedQuests.contains(Content.QUEST_KOBOLD_CAMP_CLEANUP));
        content.queryQuest(p, map, quest(stone.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);

        Creature farley = spawn(Content.NPC_INNKEEPER_FARLEY);
        mgr.questInvolved.put(Content.NPC_INNKEEPER_FARLEY, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        content.requestReward(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        content.queryQuest(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        farley.relocate(100, 0, 0, 0);
        content.requestReward(p, map, quest(farley.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertFalse(content.goInvolves(Content.GO_ICE_BLOCK, Content.QUEST_REST_AND_RELAXATION));
        mgr.goQuestInvolved.put(Content.GO_ICE_BLOCK, new ArrayList<>(List.of(1)));
        assertFalse(content.goInvolves(Content.GO_ICE_BLOCK, Content.QUEST_REST_AND_RELAXATION));
        assertTrue(content.goInvolves(Content.GO_ICE_STONE, Content.QUEST_KOBOLD_CAMP_CLEANUP));

        p.questLogId[4] = 4242;
        content.useGameObject(p, map, block, this::capture);
        GameObject farStone = go(Content.GO_ICE_STONE);
        farStone.relocate(200, 0, 0, 0);
        content.requestReward(p, map, quest(farStone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        content.completeQuest(p, map, quest(farStone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), 1, this::capture);
        content.queryQuest(p, map, quest(9999, Content.QUEST_REST_AND_RELAXATION), this::capture);

        mgr.areaTriggerQuests.put(88, 8888);
        p.questLogId[5] = 8888;
        content.exploreAreaTrigger(p, 88, this::capture);
        mgr.goQuestGivers.put(Content.GO_ICE_STONE, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        mgr.goQuestInvolved.remove(Content.GO_ICE_STONE);
        content.queryQuest(p, map, quest(stone.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        content.requestReward(p, map, quest(stone.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        assertFalse(content.goGives(Content.GO_ICE_STONE, Content.QUEST_KOBOLD_CAMP_CLEANUP));
        mgr.quests.put(Content.QUEST_REST_AND_RELAXATION, questTemplate(
                Content.QUEST_REST_AND_RELAXATION, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        mgr.questExtras.put(Content.QUEST_REST_AND_RELAXATION, ObjectMgr.QuestExtras.limit(0));
        assertTrue(content.objectivesMet(p, 0, mgr.quests.get(Content.QUEST_REST_AND_RELAXATION)));
        assertFalse(content.goGives(404, 1));
        assertTrue(content.objectivesMet(p, 0, questTemplate(
                1, 0, 5, 0, 0, 0, 3, 0, 0, 1, 0, 0)));
        mgr.quests.put(55, questTemplate(55, -Content.GO_ICE_BLOCK, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[6] = 55;
        p.questLogState[6] = 0;
        p.questLogCounts[6][0] = 0;
        content.useGameObject(p, map, stone, this::capture);
        assertEquals(0, p.questLogCounts[6][0]);
        mgr.goQuestGivers.remove(Content.GO_ICE_STONE);
        mgr.goQuestInvolved.put(Content.GO_ICE_STONE, new ArrayList<>(List.of(Content.QUEST_REST_AND_RELAXATION)));
        content.queryQuest(p, map, quest(stone.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        content.queryQuest(p, map, quest(stone.guid, Content.QUEST_KOBOLD_CAMP_CLEANUP), this::capture);
        content.requestReward(p, map, quest(stone.guid, Content.QUEST_REST_AND_RELAXATION), this::capture);
        mgr.quests.put(56, questTemplate(56, -Content.GO_ICE_BLOCK, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0));
        p.questLogId[7] = 56;
        p.questLogState[7] = 0;
        p.questLogCounts[7][0] = 2;
        content.useGameObject(p, map, block, this::capture);
        assertEquals(2, p.questLogCounts[7][0]);
    }

    private boolean containsQuest(int questId) {
        for (int id : p.questLogId) {
            if (id == questId) {
                return true;
            }
        }
        return false;
    }

    private GameObject go(int entry) {
        GameObject go = new GameObject();
        go.guid = map.gameObjects.size() + 50;
        go.entry = entry;
        go.relocate(0, 0, 0, 0);
        map.add(go);
        return go;
    }
}
