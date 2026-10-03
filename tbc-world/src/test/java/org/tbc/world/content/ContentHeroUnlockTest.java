package org.tbc.world.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.classless.HeroClassUnlock;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Branch coverage for Hero warrior unlock (Content is JaCoCo-gated). */
class ContentHeroUnlockTest {
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
        p.level = 1;
        p.race = 1;
        p.relocate(0, 0, 0, 0);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        map.add(p);
        ops.clear();
        last.clear();
    }

    @Test
    void canTakeWhenOrdinaryClassShouldRefuseHeroUnlockQuest() {
        p.clazz = Player.CLASS_WARRIOR;
        ObjectMgr.QuestTemplate q = mgr.quests.get(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertFalse(content.canTake(p, q));
    }

    @Test
    void canTakeWhenHeroShouldAllowHeroUnlockQuest() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        ObjectMgr.QuestTemplate q = mgr.quests.get(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertTrue(content.canTake(p, q));
        assertTrue(content.canTake(p, mgr.quests.get(Content.QUEST_A_THREAT_WITHIN)));
    }

    @Test
    void acceptQuestWhenWarriorShouldRefuseHeroUnlock() {
        p.clazz = Player.CLASS_WARRIOR;
        Creature trainer = spawnTrainer();
        WowBuffer in = new WowBuffer(12);
        in.putU64(trainer.guid);
        in.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        content.acceptQuest(p, map, in, this::capture);
        assertEquals(0, p.questLogId[0]);
    }

    @Test
    void acceptQuestWhenHeroShouldTakeUnlock() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature trainer = spawnTrainer();
        WowBuffer in = new WowBuffer(12);
        in.putU64(trainer.guid);
        in.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        content.acceptQuest(p, map, in, this::capture);
        assertEquals(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON, p.questLogId[0]);
    }

    @Test
    void creatureHitCreditWhenQuestHasNoHitObjectiveShouldIgnore() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature mcbride = spawn(Content.NPC_MARSHAL_MCBRIDE);
        WowBuffer in = new WowBuffer(12);
        in.putU64(mcbride.guid);
        in.putU32(Content.QUEST_KOBOLD_CAMP_CLEANUP);
        content.acceptQuest(p, map, in, this::capture);
        Creature wyrm = spawnWyrm();
        content.creatureHitCredit(p, map, wyrm, this::capture);
        assertEquals(0, p.questLogCounts[0][1]);
    }

    @Test
    void creatureHitCreditWhenNullVictimShouldNoOp() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        content.creatureHitCredit(p, map, null, this::capture);
        assertEquals(0, p.questLogCounts[0][1]);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_ADD_KILL));
    }

    @Test
    void creatureHitCreditWhenWrongOrCompleteShouldIgnore() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        Creature kobold = mgr.spawnCreature(6, 0, 0, 0, 0, 0, null);
        map.add(kobold);
        content.creatureHitCredit(p, map, kobold, this::capture);
        assertEquals(0, p.questLogCounts[0][1]);

        Creature wyrm = spawnWyrm();
        p.questLogState[0] = Content.QUEST_STATE_COMPLETE;
        content.creatureHitCredit(p, map, wyrm, this::capture);
        assertEquals(0, p.questLogCounts[0][1]);

        p.questLogState[0] = 0;
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        content.creatureHitCredit(p, map, wyrm, this::capture);
        assertEquals(HeroClassUnlock.REQUIRED_HITS, p.questLogCounts[0][1]);
    }

    @Test
    void creatureHitCreditWhenFiveHitsAndKillShouldComplete() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        p.questLogCounts[0][0] = 1;
        Creature wyrm = spawnWyrm();
        for (int i = 1; i <= 4; i++) {
            content.creatureHitCredit(p, map, wyrm, this::capture);
            assertEquals(i, p.questLogCounts[0][1]);
            assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        }
        content.creatureHitCredit(p, map, wyrm, this::capture);
        assertEquals(5, p.questLogCounts[0][1]);
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
    }

    /** Trainer still on the client past VISIBILITY must get yellow ? when hits and the kill are done. */
    @Test
    void creatureHitCreditWhenTrainerSeenBeyondVisibilityShouldSendRewardStatus() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        Creature trainer = spawnTrainer();
        WowBuffer in = new WowBuffer(12);
        in.putU64(trainer.guid);
        in.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        content.acceptQuest(p, map, in, this::capture);
        float ox = trainer.x;
        float oy = trainer.y;
        trainer.relocate((float) GameMap.VISIBILITY + 20f, 0f, 0f, 0f);
        map.reindex(trainer, ox, oy);
        WorldSession session = new WorldSession(new PacketSink() {
            @Override
            public void send(int opcode, byte[] payload) {
            }

            @Override
            public void close() {
            }
        }, 1);
        session.markSeen(trainer.guid);
        p.session = session;
        p.questLogCounts[0][0] = 1;
        Creature wyrm = spawnWyrm();
        ops.clear();
        last.clear();
        for (int i = 0; i < HeroClassUnlock.REQUIRED_HITS; i++) {
            content.creatureHitCredit(p, map, wyrm, this::capture);
        }
        assertEquals(Content.QUEST_STATE_COMPLETE, p.questLogState[0]);
        WowBuffer multi = new WowBuffer(last.get(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE));
        int n = multi.getU32();
        boolean sawReward = false;
        for (int i = 0; i < n; i++) {
            long guid = multi.getU64();
            int status = multi.getU8() & 0xFF;
            if (guid == trainer.guid) {
                assertEquals(Content.DIALOG_STATUS_REWARD, status);
                sawReward = true;
            }
        }
        assertTrue(sawReward);
    }

    @Test
    void creatureHitCreditWhenTemplateMissingShouldNotComplete() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        mgr.quests.remove(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        Creature wyrm = spawnWyrm();
        content.creatureHitCredit(p, map, wyrm, this::capture);
        assertEquals(1, p.questLogCounts[0][1]);
        assertFalse(ops.contains(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
    }

    @Test
    void objectivesMetWhenHitsShortShouldBeFalse() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        p.questLogCounts[0][0] = 1;
        ObjectMgr.QuestTemplate q = mgr.quests.get(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertFalse(content.objectivesMet(p, 0, q));
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        assertTrue(content.objectivesMet(p, 0, q));
    }

    @Test
    void completeQuestWhenDeadHeroShouldRefuse() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        p.questLogCounts[0][0] = 1;
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        Creature trainer = spawnTrainer();
        p.setHealth(0);
        WowBuffer in = choose(trainer.guid);
        content.completeQuest(p, map, in, () -> 1L, this::capture);
        assertFalse(p.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertFalse(p.spells.contains(SpellEngine.HEROIC_STRIKE));
    }

    @Test
    void completeQuestWhenAliveShouldLearnRewSpellOnce() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        p.questLogCounts[0][0] = 1;
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        Creature trainer = spawnTrainer();
        WowBuffer in = choose(trainer.guid);
        content.completeQuest(p, map, in, () -> 1L, this::capture);
        assertTrue(p.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertTrue(p.spells.contains(SpellEngine.HEROIC_STRIKE));
        assertTrue(ops.contains(Opcodes.SMSG_LEARNED_SPELL));
    }

    @Test
    void completeQuestWhenRewSpellAlreadyKnownShouldStayIdempotent() {
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        takeQuest();
        p.questLogCounts[0][0] = 1;
        p.questLogCounts[0][1] = HeroClassUnlock.REQUIRED_HITS;
        p.spells.add(SpellEngine.HEROIC_STRIKE);
        Creature trainer = spawnTrainer();
        ops.clear();
        last.clear();
        content.completeQuest(p, map, choose(trainer.guid), () -> 1L, this::capture);
        assertTrue(p.rewardedQuests.contains(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertFalse(ops.contains(Opcodes.SMSG_LEARNED_SPELL));
        assertTrue(p.spells.contains(SpellEngine.HEROIC_STRIKE));
    }

    private void takeQuest() {
        Creature trainer = spawnTrainer();
        WowBuffer in = new WowBuffer(12);
        in.putU64(trainer.guid);
        in.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        content.acceptQuest(p, map, in, this::capture);
        ops.clear();
        last.clear();
    }

    private Creature spawnTrainer() {
        return spawn(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
    }

    private Creature spawnWyrm() {
        return spawn(HeroClassUnlock.CREATURE_MANA_WYRM);
    }

    private Creature spawn(int entry) {
        Creature c = mgr.spawnCreature(entry, 0, 0, 0, 0, 0, null);
        map.add(c);
        return c;
    }

    private WowBuffer choose(long guid) {
        WowBuffer in = new WowBuffer(16);
        in.putU64(guid);
        in.putU32(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        in.putU32(0);
        return in;
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
    }
}
