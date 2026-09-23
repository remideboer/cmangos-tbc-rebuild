package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
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
    void objectivesMetWhenCountIsZeroOrAlreadyFilledShouldPassThatSlot() {
        ObjectMgr.QuestTemplate zeroNeed = questTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                Content.NPC_KOBOLD_VERMIN, 0, 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 0, 0, 0, 1, 0, 0);
        assertTrue(Content.objectivesMet(p, 0, zeroNeed));
        ObjectMgr.QuestTemplate filled = questTemplate(Content.QUEST_KOBOLD_CAMP_CLEANUP,
                Content.NPC_KOBOLD_VERMIN, 1, 0, 0, Content.ITEM_RED_BURLAP_BANDANA, 1, 0, 0, 1, 0, 0);
        p.questLogCounts[0][0] = 1;
        p.questLogItemCount[0][0] = 1;
        assertTrue(Content.objectivesMet(p, 0, filled));
        p.questLogCounts[0][0] = 0;
        assertFalse(Content.objectivesMet(p, 0, filled));
        p.questLogCounts[0][0] = 1;
        p.questLogItemCount[0][0] = 0;
        assertFalse(Content.objectivesMet(p, 0, filled));
        assertTrue(Content.objectivesMet(p, 0, mgr.quests.get(Content.QUEST_A_THREAT_WITHIN)));
    }

    private int statusByte() {
        return last.get(Opcodes.SMSG_QUESTGIVER_STATUS)[8] & 0xFF;
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
}
