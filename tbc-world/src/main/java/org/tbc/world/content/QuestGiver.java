package org.tbc.world.content;

import static org.tbc.world.content.Content.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.ReputationMgr;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

/**
 * Quest giver: CMSG_QUESTGIVER_* dialog, quest log slots, objective credit and rewards
 * (CMaNGOS QuestHandler.cpp / Player.cpp quest methods). Split out of Content; Content is the facade.
 */
final class QuestGiver {

    private final ObjectMgr mgr;

    QuestGiver(ObjectMgr mgr) {
        this.mgr = mgr;
    }
    public void queryQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        long giverGuid = resolveQuestGiverGuid(p, map, guid, questId, true);
        if (giverGuid == 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, encodeDetails(giverGuid, q));
    }

    /** HandleQuestgiverRequestRewardOpcode → PlayerMenu::SendQuestGiverOfferReward. */
    public void requestReward(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        long giverGuid = resolveQuestGiverGuid(p, map, guid, questId, false);
        if (giverGuid == 0) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null || !readyToTurnIn(p, slot, q)) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD, encodeOfferReward(giverGuid, q));
    }

    /**
     * CMSG_QUESTGIVER_STATUS_QUERY. GossipDef.cpp SendQuestGiverStatus; QuestHandler.cpp getDialogStatus.
     * Missing questgiver: no packet. Found: raw guid + uint8 status.
     */
    public void questGiverStatusQuery(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null) {
            GameObject go = map.gameObjects.get(guid);
            if (go == null) {
                return;
            }
            sendQuestGiverStatus(p, go.guid, goDialogStatus(p, go.entry), send);
            return;
        }
        sendQuestGiverStatus(p, c, send);
    }

    /** GossipDef.cpp SendQuestGiverStatus. Visible questgivers, not only those inside interact range. */
    public void sendQuestGiverStatus(Player p, Creature c, BiConsumer<Integer, byte[]> send) {
        WowBuffer out = new WowBuffer(9);
        out.putU64(c.guid);
        out.putU8(dialogStatus(p, c));
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS, out.array());
    }

    private void sendQuestGiverStatus(Player p, long guid, int status, BiConsumer<Integer, byte[]> send) {
        WowBuffer out = new WowBuffer(9);
        out.putU64(guid);
        out.putU8(status);
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS, out.array());
    }

    /**
     * CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY. Player.cpp SendQuestGiverStatusMultiple:
     * questgivers inside visibility, plus any questgiver the client already has
     * ({@code m_clientGUIDs} / session seen). Raw guid + uint8 status; count prefix.
     * Recv ignored. A game-object questgiver answers CMSG_QUESTGIVER_STATUS_QUERY.
     */
    public void questGiverStatusMultiple(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        Map<Long, Creature> found = new LinkedHashMap<>();
        for (Creature c : map.nearbyCreatures(p, GameMap.VISIBILITY)) {
            if ((c.npcFlags & UNIT_NPC_FLAG_QUESTGIVER) != 0) {
                found.put(c.guid, c);
            }
        }
        if (p.session != null) {
            for (long guid : p.session.seenGuids()) {
                Creature c = creature(map, guid);
                if (c != null && (c.npcFlags & UNIT_NPC_FLAG_QUESTGIVER) != 0) {
                    found.putIfAbsent(c.guid, c);
                }
            }
        }
        WowBuffer out = new WowBuffer(4 + found.size() * 9);
        out.putU32(found.size());
        for (Creature c : found.values()) {
            out.putU64(c.guid);
            out.putU8(dialogStatus(p, c));
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE, out.array());
    }

    /**
     * QuestHandler.cpp getDialogStatus. Higher QuestDef.h value wins
     * (reward 8, available 6, incomplete 3).
     */
    int dialogStatus(Player p, Creature c) {
        int best = DIALOG_STATUS_NONE;
        best = raiseStatus(p, c.entry, mgr.questInvolved.get(c.entry), true, best);
        best = raiseStatus(p, c.entry, mgr.questGivers.get(c.entry), false, best);
        return best;
    }

    private int goDialogStatus(Player p, int entry) {
        int best = DIALOG_STATUS_NONE;
        best = raiseStatus(p, entry, mgr.goQuestInvolved.get(entry), true, best);
        best = raiseStatus(p, entry, mgr.goQuestGivers.get(entry), false, best);
        return best;
    }

    private int raiseStatus(Player p, int entry, List<Integer> quests, boolean involved, int best) {
        if (quests == null) {
            return best;
        }
        for (int questId : quests) {
            int status = relationStatus(p, entry, questId, involved);
            if (status > best) {
                best = status;
            }
        }
        return best;
    }

    private int relationStatus(Player p, int entry, int questId, boolean involved) {
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null) {
            return DIALOG_STATUS_NONE;
        }
        int slot = slotOf(p, questId);
        if (slot >= 0) {
            if (readyToTurnIn(p, slot, q)) {
                return involved ? DIALOG_STATUS_REWARD : DIALOG_STATUS_NONE;
            }
            return DIALOG_STATUS_INCOMPLETE;
        }
        if (!involved && canTake(p, q)) {
            return DIALOG_STATUS_AVAILABLE;
        }
        return DIALOG_STATUS_NONE;
    }

    /** Player::CanTakeQuest for level, race, previous quest, and already rewarded. */
    boolean canTake(Player p, ObjectMgr.QuestTemplate q) {
        if (p.rewardedQuests.contains(q.id())) {
            return false;
        }
        if (p.level < q.minLevel()) {
            return false;
        }
        if (!raceMatches(p, q.requiredRaces())) {
            return false;
        }
        if (!repAndDailyAllow(p, q)) {
            return false;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(q.id())
                && !org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p)) {
            return false;
        }
        return prevSatisfied(p, q.prevQuestId());
    }

    private boolean repAndDailyAllow(Player p, ObjectMgr.QuestTemplate q) {
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(q.id());
        if (extra != null && extra.isDaily() && p.dailyQuestDone.contains(q.id())) {
            return false;
        }
        if (extra != null && extra.reqRepFaction() > 0 && p.reputationStanding(extra.reqRepFaction()) < extra.reqRepValue()) {
            return false;
        }
        return true;
    }

    static boolean raceMatches(Player p, int requiredRaces) {
        if (requiredRaces == 0) {
            return true;
        }
        int bit = p.race <= 0 ? 0 : 1 << (p.race - 1);
        return (requiredRaces & bit) != 0;
    }

    static boolean prevSatisfied(Player p, int prevQuestId) {
        if (prevQuestId == 0) {
            return true;
        }
        if (prevQuestId > 0) {
            return p.rewardedQuests.contains(prevQuestId);
        }
        return slotOf(p, -prevQuestId) >= 0;
    }

    /**
     * CMaNGOS getDialogStatus uses QUEST_STATUS_COMPLETE, not a live objective recount.
     * Spell/event complete can set the log bit before counters catch up.
     */
    boolean readyToTurnIn(Player p, int slot, ObjectMgr.QuestTemplate q) {
        return p.questLogState[slot] == QUEST_STATE_COMPLETE || objectivesMet(p, slot, q);
    }

    /** Player::CompleteQuest — log bit, QUESTUPDATE_COMPLETE, refresh nearby ? / !. */
    private void markQuestObjectivesComplete(Player p, GameMap map, int slot, int questId,
                                             BiConsumer<Integer, byte[]> send) {
        p.questLogState[slot] = QUEST_STATE_COMPLETE;
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
        sendLogUpdate(p, slot, send);
        questGiverStatusMultiple(p, map, send);
    }

    /** Creature, item, game-object, spell, and explore objectives. */
    boolean objectivesMet(Player p, int slot, ObjectMgr.QuestTemplate q) {
        for (int i = 0; i < 4; i++) {
            if (q.reqCreatureOrGOCount(i) > 0) {
                if (q.reqCreatureOrGOId(i) != 0) {
                    if (p.questLogCounts[slot][i] < q.reqCreatureOrGOCount(i)) {
                        return false;
                    }
                }
            }
            if (q.reqItemCount(i) > 0) {
                if (q.reqItemId(i) > 0) {
                    if (p.questLogItemCount[slot][i] < q.reqItemCount(i)) {
                        return false;
                    }
                }
            }
        }
        ObjectMgr.CreatureHitObjective hits = mgr.questCreatureHits.get(q.id());
        if (hits != null && p.questLogCounts[slot][1] < hits.count()) {
            return false;
        }
        ObjectMgr.EmoteNearNpcObjective emote = mgr.questEmoteNearNpc.get(q.id());
        if (emote != null && p.questLogCounts[slot][org.tbc.world.classless.HeroClassUnlock.RALLY_ROAR_COUNT_SLOT] < 1) {
            return false;
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(q.id());
        if (extra == null) {
            return true;
        }
        boolean event = extra.reqSpell1() > 0 || extra.exploreOrEvent() || q.type() == QUEST_TYPE_ESCORT;
        if (!event) {
            return true;
        }
        return p.questLogCounts[slot][0] >= 1;
    }

    public void acceptQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        Creature c = creature(map, guid);
        GameObject go = null;
        int giverEntry;
        if (c != null) {
            if (outOfRange(p, c) || !gives(c.entry, questId)) {
                return;
            }
            giverEntry = c.entry;
        } else {
            go = map.gameObjects.get(guid);
            if (go == null || p.distance2d(go) > INTERACT_RANGE || !goGives(go.entry, questId)) {
                return;
            }
            giverEntry = go.entry;
        }
        ObjectMgr.QuestTemplate taken = mgr.quest(questId);
        if (taken == null || !repAndDailyAllow(p, taken)) {
            return;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(questId)
                && !org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        if (!prevSatisfied(p, taken.prevQuestId())) {
            return;
        }
        if (slotOf(p, questId) >= 0) {
            return;
        }
        int slot = freeSlot(p);
        if (slot < 0) {
            send.accept(Opcodes.SMSG_QUESTLOG_FULL, new byte[0]);
            return;
        }
        clearQuestSlot(p, slot);
        p.questLogId[slot] = questId;
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra != null && extra.limitSeconds() > 0) {
            p.questExpiry[slot] = System.currentTimeMillis() + extra.limitSeconds() * 1000L;
        }
        if (c != null && taken.type() == QUEST_TYPE_ESCORT) {
            c.followTarget = p.guid;
        }
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
        if (objectivesMet(p, slot, taken)) {
            markQuestObjectivesComplete(p, map, slot, questId, send);
        } else if (c != null) {
            sendQuestGiverStatus(p, c, send);
        } else {
            sendQuestGiverStatus(p, go.guid, goDialogStatus(p, giverEntry), send);
        }
    }

    /** HandleQuestLogRemoveQuest → SetQuestSlot(slot, 0). */
    public void removeQuest(Player p, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 1) {
            return;
        }
        int slot = in.getU8();
        if (slot >= MAX_QUEST_LOG_SIZE) {
            return;
        }
        clearQuestSlot(p, slot);
        writeLogField(p, slot);
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base, base + 1, base + 2));
        send.accept(upd.opcode(), upd.payload());
    }

    public void completeQuest(Player p, GameMap map, WowBuffer in, LongSupplier nextItemGuid,
                              BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        int reward = 0;
        if (in.remaining() >= 4) {
            reward = in.getU32();
        }
        if (reward >= QUEST_REWARD_CHOICES_COUNT) {
            return;
        }
        if (resolveQuestGiverGuid(p, map, guid, questId, false) == 0) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null || !readyToTurnIn(p, slot, q)) {
            return;
        }
        if (org.tbc.world.classless.HeroClassUnlock.isHeroOnly(questId) && !p.alive()) {
            return;
        }
        // Player::RewardQuest — DestroyItemCount ReqItemId/ReqItemCount before rewards.
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqItemId(i);
            if (reqId > 0) {
                destroyItemCount(p, reqId, q.reqItemCount(i), send);
            }
        }
        p.questLogState[slot] = QUEST_STATE_COMPLETE;
        send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
        int xp;
        int money = q.rewMoney();
        if (p.level < Player.MAX_LEVEL) {
            xp = QuestXp.xpValue(p.level, q.questLevel(), q.rewMoneyMaxLevel());
            int[] changed = p.giveXp(xp, null);
            if (changed.length > 0) {
                var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, changed));
                send.accept(upd.opcode(), upd.payload());
            }
        } else {
            xp = 0;
            money += q.rewMoneyMaxLevel();
        }
        p.setMoney(p.money + money);
        storeRewardItem(p, q.rewItemId1(), q.rewItemCount1(), nextItemGuid, send);
        storeRewardItem(p, q.rewChoiceItemId(reward), q.rewChoiceItemCount(reward), nextItemGuid, send);
        p.rewardedQuests.add(questId);
        int rewSpell = mgr.questRewSpell.getOrDefault(questId, 0);
        learnQuestRewardSpell(p, rewSpell, send);
        // Hero's First Lesson: Heroic Strike needs Battle Stance 2457 for the stance bar (like a warrior).
        if (questId == org.tbc.world.classless.HeroClassUnlock.QUEST_HEROS_FIRST_LESSON) {
            learnQuestRewardSpell(p, org.tbc.world.spell.SpellEngine.SPELL_BATTLE_STANCE, send);
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra != null && extra.rewRepFaction() > 0) {
            p.modifyReputation(extra.rewRepFaction(), extra.rewRepValue());
            byte[] standingPkt = p.reputations.encodeStandingUpdate(
                    ReputationMgr.listIdForFaction(extra.rewRepFaction()));
            if (standingPkt.length > 0) {
                send.accept(Opcodes.SMSG_SET_FACTION_STANDING, standingPkt);
            }
        }
        if (extra != null && extra.isDaily()) {
            p.dailyQuestDone.add(questId);
        }
        clearQuestSlot(p, slot);
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE, encodeQuestComplete(q, xp, money, 0));
        questGiverStatusMultiple(p, map, send);
    }

    private static void learnQuestRewardSpell(Player p, int spellId, BiConsumer<Integer, byte[]> send) {
        if (spellId <= 0 || p.spells.contains(spellId)) {
            return;
        }
        p.spells.add(spellId);
        WowBuffer learned = new WowBuffer(4);
        learned.putU32(spellId);
        send.accept(Opcodes.SMSG_LEARNED_SPELL, learned.array());
    }

    private void storeRewardItem(Player p, int itemId, int count, LongSupplier nextItemGuid,
                                 BiConsumer<Integer, byte[]> send) {
        if (itemId <= 0 || count <= 0) {
            return;
        }
        int bagSlot = nextBackpackSlot(p);
        if (bagSlot < 0) {
            return;
        }
        long itemGuid = nextItemGuid.getAsLong();
        if (itemGuid == 0) {
            return;
        }
        Item it = new Item(itemGuid, itemId);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = bagSlot;
        it.count = count;
        ObjectMgr.ItemTemplate t = mgr.item(itemId);
        if (t != null) {
            it.displayId = t.displayId;
            it.quality = t.quality;
            ObjectMgr.applyWeaponProto(it, t);
        }
        p.items.put(Guid.low(it.guid), it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + bagSlot * 2, UpdateBuilder.itemGuid(it));
        p.dirty = true;
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        send.accept(created.opcode(), created.payload());
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        send.accept(inv.opcode(), inv.payload());
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, it, it.count));
    }

    /**
     * Player::DestroyItemCount(entry, count, update=true). Walks backpack stacks matching
     * {@code itemId}, reducing or removing until {@code count} is consumed.
     */
    public void destroyItemCount(Player p, int itemId, int count, BiConsumer<Integer, byte[]> send) {
        if (itemId <= 0 || count <= 0) {
            return;
        }
        int left = count;
        List<Item> stacks = new ArrayList<>();
        for (Item it : p.items.values()) {
            if (it.entry == itemId && it.bag == 0) {
                stacks.add(it);
            }
        }
        for (Item it : stacks) {
            if (left <= 0) {
                break;
            }
            if (it.count <= left) {
                left -= it.count;
                p.items.remove(Guid.low(it.guid));
                int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
                p.setGuid(field, 0);
                p.dirty = true;
                var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
                send.accept(inv.opcode(), inv.payload());
                WowBuffer destroy = new WowBuffer(8);
                destroy.putU64(UpdateBuilder.itemGuid(it));
                send.accept(Opcodes.SMSG_DESTROY_OBJECT, destroy.array());
            } else {
                it.count -= left;
                left = 0;
                p.dirty = true;
                var stack = UpdateBuilder.maybeCompress(
                        UpdateBuilder.valuesItem(it, UpdateFields.ITEM_FIELD_STACK_COUNT));
                send.accept(stack.opcode(), stack.payload());
            }
        }
    }

    boolean goGives(int entry, int questId) {
        List<Integer> list = mgr.goQuestGivers.get(entry);
        return list != null && list.contains(questId);
    }

    boolean goInvolves(int entry, int questId) {
        List<Integer> list = mgr.goQuestInvolved.get(entry);
        return list != null && list.contains(questId);
    }

    /** Creature or game-object questgiver in range; {@code offer} allows giver or involved. */
    private long resolveQuestGiverGuid(Player p, GameMap map, long guid, int questId, boolean offer) {
        Creature c = creature(map, guid);
        if (c != null) {
            if (outOfRange(p, c) || !questNpcRelated(c.entry, questId, offer, false)) {
                return 0;
            }
            return c.guid;
        }
        GameObject go = map.gameObjects.get(guid);
        if (go == null || p.distance2d(go) > INTERACT_RANGE
                || !questNpcRelated(go.entry, questId, offer, true)) {
            return 0;
        }
        return go.guid;
    }

    private boolean questNpcRelated(int entry, int questId, boolean offer, boolean gameObject) {
        if (gameObject) {
            if (offer && goGives(entry, questId)) {
                return true;
            }
            return goInvolves(entry, questId);
        }
        if (offer) {
            return offersOrInvolves(entry, questId);
        }
        return involves(entry, questId);
    }

    /** Using a questgiver object opens its quest. Using an objective object credits the negative id. */
    public boolean useGameObject(Player p, GameMap map, GameObject go, BiConsumer<Integer, byte[]> send) {
        if (go == null || p.distance2d(go) > INTERACT_RANGE) {
            return false;
        }
        boolean credited = creditGameObject(p, map, go, send);
        List<Integer> offered = mgr.goQuestGivers.get(go.entry);
        if (offered == null) {
            return credited;
        }
        for (int questId : offered) {
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            if (q != null && canTake(p, q)) {
                send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, encodeDetails(go.guid, q));
                return true;
            }
        }
        return credited;
    }

    public void exploreAreaTrigger(Player p, GameMap map, int triggerId, BiConsumer<Integer, byte[]> send) {
        Integer questId = mgr.areaTriggerQuests.get(triggerId);
        if (questId == null) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quest(questId);
        if (q == null) {
            return;
        }
        ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
        if (extra == null || !extra.exploreOrEvent()) {
            return;
        }
        if (p.questLogCounts[slot][0] > 0) {
            return;
        }
        p.questLogCounts[slot][0] = 1;
        finishObjective(p, map, slot, q, q.id(), send);
    }

    public void spellCastCredit(Player p, GameMap map, int spellId, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
            if (extra == null || extra.reqSpell1() != spellId || p.questLogCounts[slot][0] > 0) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            if (q == null) {
                continue;
            }
            p.questLogCounts[slot][0] = 1;
            finishObjective(p, map, slot, q, spellId, send);
        }
    }

    public void tickEscort(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            ObjectMgr.QuestExtras extra = mgr.questExtras.get(questId);
            if (q == null || q.type() != QUEST_TYPE_ESCORT || extra == null || p.questLogCounts[slot][0] > 0) {
                continue;
            }
            double dx = p.x - extra.pointX();
            double dy = p.y - extra.pointY();
            if (dx * dx + dy * dy > 25) {
                continue;
            }
            p.questLogCounts[slot][0] = 1;
            finishObjective(p, map, slot, q, questId, send);
        }
    }

    public void failExpired(Player p, long nowMs, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questExpiry[slot] == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            if (nowMs < p.questExpiry[slot]) {
                continue;
            }
            p.questLogState[slot] = QUEST_STATE_FAIL;
            WowBuffer fail = new WowBuffer(4);
            fail.putU32(questId);
            send.accept(Opcodes.SMSG_QUESTUPDATE_FAILEDTIMER, fail.array());
        }
    }

    public void resetDailies(Player p) {
        for (int questId : p.dailyQuestDone) {
            p.rewardedQuests.remove(questId);
        }
        p.dailyQuestDone.clear();
    }

    private boolean creditGameObject(Player p, GameMap map, GameObject go, BiConsumer<Integer, byte[]> send) {
        boolean credited = false;
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            if (q == null) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                int req = q.reqCreatureOrGOId(i);
                if (req >= 0 || req != -go.entry) {
                    continue;
                }
                int have = p.questLogCounts[slot][i];
                int need = q.reqCreatureOrGOCount(i);
                if (have >= need) {
                    continue;
                }
                p.questLogCounts[slot][i] = have + 1;
                finishObjective(p, map, slot, q, req, send);
                credited = true;
            }
        }
        return credited;
    }

    private void finishObjective(Player p, GameMap map, int slot, ObjectMgr.QuestTemplate q, int objectiveId,
                                 BiConsumer<Integer, byte[]> send) {
        WowBuffer add = new WowBuffer(24);
        add.putU32(q.id());
        add.putU32(objectiveId);
        add.putU32(p.questLogCounts[slot][0]);
        add.putU32(1);
        add.putU64(0);
        send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
        writeLogField(p, slot);
        sendLogUpdate(p, slot, send);
        if (objectivesMet(p, slot, q)) {
            p.questLogState[slot] = QUEST_STATE_COMPLETE;
            writeLogField(p, slot);
            sendLogUpdate(p, slot, send);
            send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(q.id()));
            questGiverStatusMultiple(p, map, send);
        }
    }

    boolean gives(int entry, int questId) {
        List<Integer> list = mgr.questGivers.get(entry);
        return list != null && list.contains(questId);
    }

    boolean involves(int entry, int questId) {
        List<Integer> list = mgr.questInvolved.get(entry);
        return list != null && list.contains(questId);
    }

    boolean offersOrInvolves(int entry, int questId) {
        return gives(entry, questId) || involves(entry, questId);
    }

    /**
     * Player.cpp KilledMonsterCredit / SendQuestUpdateAddCreatureOrGo.
     * Creature objective slots 1–4. Game-object objectives (negative ids) are later.
     */
    public void killedMonsterCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        if (victim == null) {
            return;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            if (q == null) {
                continue;
            }
            int objective = creditCreature(p, slot, q, victim.entry);
            if (objective < 0) {
                continue;
            }
            int cur = p.questLogCounts[slot][objective];
            int reqCount = q.reqCreatureOrGOCount(objective);
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32(q.reqCreatureOrGOId(objective));
            add.putU32(cur);
            add.putU32(reqCount);
            add.putU64(victim.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            boolean done = objectivesMet(p, slot, q);
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            sendLogUpdate(p, slot, send);
            if (done) {
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /**
     * Landed melee hits on a creature objective (not a kill). Misses and spells do not call this.
     */
    public void creatureHitCredit(Player p, GameMap map, Creature victim, BiConsumer<Integer, byte[]> send) {
        if (victim == null) {
            return;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.CreatureHitObjective hits = mgr.questCreatureHits.get(questId);
            if (hits == null || hits.creatureEntry() != victim.entry) {
                continue;
            }
            int cur = p.questLogCounts[slot][1];
            if (cur >= hits.count()) {
                continue;
            }
            cur++;
            p.questLogCounts[slot][1] = cur;
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32(hits.creatureEntry());
            add.putU32(cur);
            add.putU32(hits.count());
            add.putU64(victim.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            boolean done = q != null && objectivesMet(p, slot, q);
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            sendLogUpdate(p, slot, send);
            if (done) {
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /**
     * CMaNGOS {@code CreatureAI::ReceiveEmote}: Hero /roar near the named trainer.
     * Returns the NPC and spell to cast, or null.
     */
    public ObjectMgr.EmoteNearNpcObjective creditTextEmoteNearNpc(Player p, GameMap map, int textEmote,
                                           BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.EmoteNearNpcObjective obj = mgr.questEmoteNearNpc.get(questId);
            if (obj == null || obj.textEmote() != textEmote) {
                continue;
            }
            int roarSlot = org.tbc.world.classless.HeroClassUnlock.RALLY_ROAR_COUNT_SLOT;
            if (p.questLogCounts[slot][roarSlot] >= 1) {
                continue;
            }
            Creature npc = nearestNpc(p, map, org.tbc.world.classless.HeroStarterTrainers.warriorEntries());
            if (npc == null) {
                return null;
            }
            p.questLogCounts[slot][roarSlot] = 1;
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32((int) npc.entry);
            add.putU32(1);
            add.putU32(1);
            add.putU64(npc.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            boolean done = objectivesMet(p, slot, q);
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            sendLogUpdate(p, slot, send);
            if (done) {
                questGiverStatusMultiple(p, map, send);
            }
            return obj;
        }
        return null;
    }

    private static Creature nearestNpc(Player p, GameMap map, int[] entries) {
        for (Creature c : map.creatures.values()) {
            if (p.distance2d(c) > INTERACT_RANGE) {
                continue;
            }
            for (int entry : entries) {
                if (c.entry == entry) {
                    return c;
                }
            }
        }
        return null;
    }

    /** First matching creature slot that still needs a kill, or -1. */
    private static int creditCreature(Player p, int slot, ObjectMgr.QuestTemplate q, int entry) {
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqCreatureOrGOId(i);
            int reqCount = q.reqCreatureOrGOCount(i);
            if (reqId <= 0 || reqId != entry) {
                continue;
            }
            int cur = p.questLogCounts[slot][i];
            if (cur >= reqCount) {
                continue;
            }
            p.questLogCounts[slot][i] = cur + 1;
            return i;
        }
        return -1;
    }

    /**
     * Player.cpp ItemAddedQuestCheck / SendQuestUpdateAddItem.
     * Item objective slots 1–4. Packet is item u32 + added count u32 — not quest id.
     */
    public void itemAddedQuestCheck(Player p, GameMap map, int entry, int count, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quest(questId);
            if (q == null) {
                continue;
            }
            int[] added = new int[1];
            int objective = creditItem(p, slot, q, entry, count, added);
            if (objective < 0) {
                continue;
            }
            WowBuffer pkt = new WowBuffer(8);
            pkt.putU32(q.reqItemId(objective));
            pkt.putU32(added[0]);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM, pkt.array());
            if (objectivesMet(p, slot, q)) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
                sendLogUpdate(p, slot, send);
                questGiverStatusMultiple(p, map, send);
            }
        }
    }

    /** First matching item slot still short of its count, or -1. added[0] is the clamped amount. */
    private static int creditItem(Player p, int slot, ObjectMgr.QuestTemplate q, int entry, int count, int[] added) {
        for (int i = 0; i < 4; i++) {
            int reqId = q.reqItemId(i);
            int reqCount = q.reqItemCount(i);
            if (reqId <= 0 || reqId != entry) {
                continue;
            }
            int cur = p.questLogItemCount[slot][i];
            if (cur >= reqCount) {
                continue;
            }
            int add = cur + count <= reqCount ? count : reqCount - cur;
            p.questLogItemCount[slot][i] = cur + add;
            added[0] = add;
            return i;
        }
        return -1;
    }

    static void clearQuestSlot(Player p, int slot) {
        p.questLogId[slot] = 0;
        p.questLogState[slot] = 0;
        p.questLogCounts[slot][0] = 0;
        p.questLogCounts[slot][1] = 0;
        p.questLogCounts[slot][2] = 0;
        p.questLogCounts[slot][3] = 0;
        p.questLogItemCount[slot][0] = 0;
        p.questLogItemCount[slot][1] = 0;
        p.questExpiry[slot] = 0;
        p.questLogItemCount[slot][2] = 0;
        p.questLogItemCount[slot][3] = 0;
    }

    private void sendLogUpdate(Player p, int slot, BiConsumer<Integer, byte[]> send) {
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base, base + 1, base + 2));
        send.accept(upd.opcode(), upd.payload());
    }

    /** Mirror questLog* arrays into PLAYER_QUEST_LOG_* before create-self / VALUES. */
    public static void syncQuestLogFields(Player p) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            writeLogField(p, slot);
        }
    }

    static void writeLogField(Player p, int slot) {
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        p.setInt(base, p.questLogId[slot]);
        p.setInt(base + 1, p.questLogState[slot]);
        int packed = (p.questLogCounts[slot][0] & 0xFF)
                | ((p.questLogCounts[slot][1] & 0xFF) << 8)
                | ((p.questLogCounts[slot][2] & 0xFF) << 16)
                | ((p.questLogCounts[slot][3] & 0xFF) << 24);
        p.setInt(base + 2, packed);
        p.setInt(base + 3, 0);
    }

    byte[] encodeOfferReward(long guid, ObjectMgr.QuestTemplate q) {
        int choices = q.rewChoiceItemsCount();
        int items = q.rewItemsCount();
        WowBuffer b = new WowBuffer(64 + q.title().length() + choices * 12 + items * 12);
        b.putU64(guid);
        b.putU32(q.id());
        b.putCString(q.title());
        b.putCString("");
        b.putU32(1);
        b.putU32(0);
        b.putU32(0);
        b.putU32(choices);
        for (int i = 0; i < choices; i++) {
            int id = q.rewChoiceItemId(i);
            b.putU32(id);
            b.putU32(q.rewChoiceItemCount(i));
            b.putU32(displayId(id));
        }
        b.putU32(items);
        if (items > 0) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
            b.putU32(displayId(q.rewItemId1()));
        }
        b.putU32(q.rewMoney());
        b.putU32(0);
        b.putU32(0x08);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    private int displayId(int itemId) {
        ObjectMgr.ItemTemplate t = mgr.item(itemId);
        return t == null ? 0 : t.displayId;
    }

    /** GossipDef.cpp writes GetQuestLevel. A stored 0 falls back to MinLevel. */
    static int shownQuestLevel(ObjectMgr.QuestTemplate q) {
        return q.questLevel() != 0 ? q.questLevel() : q.minLevel();
    }

    byte[] encodeDetails(long guid, ObjectMgr.QuestTemplate q) {
        int choices = q.rewChoiceItemsCount();
        int items = q.rewItemsCount();
        WowBuffer b = new WowBuffer(96 + choices * 12 + items * 12
                + q.title().length() + q.details().length() + q.objectives().length());
        b.putU64(guid);
        b.putU32(q.id());
        b.putCString(q.title());
        b.putCString(q.details());
        b.putCString(q.objectives());
        b.putU32(1);
        b.putU32(0);
        b.putU32(choices);
        for (int i = 0; i < choices; i++) {
            int id = q.rewChoiceItemId(i);
            b.putU32(id);
            b.putU32(q.rewChoiceItemCount(i));
            b.putU32(displayId(id));
        }
        b.putU32(items);
        for (int i = 0; i < items; i++) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
            b.putU32(displayId(q.rewItemId1()));
        }
        b.putU32(q.rewMoney());
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    static byte[] encodeQuestComplete(ObjectMgr.QuestTemplate q, int xp, int money, int honor) {
        int items = q.rewItemId1() > 0 ? 1 : 0;
        WowBuffer b = new WowBuffer(24 + items * 8);
        b.putU32(q.id());
        b.putU32(0x03);
        b.putU32(xp);
        b.putU32(money);
        b.putU32(honor);
        b.putU32(items);
        if (items > 0) {
            b.putU32(q.rewItemId1());
            b.putU32(q.rewItemCount1());
        }
        return b.array();
    }
}
