package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.AuctionHandler;
import org.tbc.world.session.InventoryHandler;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.TrainerHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Gossip, vendor buy, starter quest. Packets: gossip.md, quest.md, inventory-gossip-quest.md. */
public final class Content {
    public static final float INTERACT_RANGE = 5f;
    public static final float GOLDSHIRE_X = -9465f;
    public static final float GOLDSHIRE_Y = 16f;
    public static final float GOLDSHIRE_Z = 57f;
    public static final int UNIT_NPC_FLAG_GOSSIP = 0x1;
    public static final int UNIT_NPC_FLAG_QUESTGIVER = 0x2;
    public static final int UNIT_NPC_FLAG_VENDOR = 0x80;
    public static final int UNIT_NPC_FLAG_TRAINER = 0x10;
    public static final int UNIT_NPC_FLAG_FLIGHTMASTER = 0x2000;
    public static final int UNIT_NPC_FLAG_AUCTIONEER = 0x200000;
    public static final int UNIT_NPC_FLAG_BANKER = 0x00020000;
    public static final int UNIT_NPC_FLAG_INNKEEPER = 0x00010000;
    /** Unit.h UNIT_NPC_FLAG_PETITIONER. */
    public static final int UNIT_NPC_FLAG_PETITIONER = 0x00040000;
    /** Unit.h UNIT_NPC_FLAG_TABARDDESIGNER. */
    public static final int UNIT_NPC_FLAG_TABARDDESIGNER = 0x00080000;
    /** GossipDef.h DEFAULT_GOSSIP_MESSAGE; menu 0 title text. */
    public static final int DEFAULT_GOSSIP_MESSAGE = 0x00FFFFFF;
    /** NPCHandler.h MAX_GOSSIP_TEXT_OPTIONS. */
    public static final int MAX_GOSSIP_TEXT_OPTIONS = 8;
    /** QueryHandler.cpp missing npc_text fill. */
    public static final String DEFAULT_NPC_TEXT = "Greetings $N";
    /** GossipDef.h GOSSIP_ICON_VENDOR; brown bag. */
    public static final int GOSSIP_ICON_VENDOR = 1;
    /** GossipDef.h GOSSIP_ICON_TAXI; flight. */
    public static final int GOSSIP_ICON_TAXI = 2;
    /** GossipDef.h GOSSIP_ICON_INTERACT_2; innkeeper. */
    public static final int GOSSIP_ICON_INTERACT_2 = 5;
    /** GossipDef.h GOSSIP_ICON_TRAINER; book. */
    public static final int GOSSIP_ICON_TRAINER = 3;
    /** GossipDef.h GOSSIP_ICON_MONEY_BAG; banker. */
    public static final int GOSSIP_ICON_MONEY_BAG = 6;
    /** GossipDef.h GOSSIP_OPTION_GOSSIP. */
    public static final int GOSSIP_OPTION_GOSSIP = 1;
    /** GossipDef.h GOSSIP_OPTION_VENDOR. */
    public static final int GOSSIP_OPTION_VENDOR = 3;
    /** GossipDef.h GOSSIP_OPTION_TAXIVENDOR. */
    public static final int GOSSIP_OPTION_TAXIVENDOR = 4;
    /** GossipDef.h GOSSIP_OPTION_TRAINER. */
    public static final int GOSSIP_OPTION_TRAINER = 5;
    /** GossipDef.h GOSSIP_OPTION_BANKER. */
    public static final int GOSSIP_OPTION_BANKER = 9;
    /** GossipDef.h GOSSIP_OPTION_INNKEEPER. */
    public static final int GOSSIP_OPTION_INNKEEPER = 8;
    /** GossipDef.h GOSSIP_OPTION_AUCTIONEER. */
    public static final int GOSSIP_OPTION_AUCTIONEER = 13;
    /** AuctionHouseMgr.cpp GetAuctionHouseEntry faction 12 (human). */
    public static final int AUCTION_HOUSE_HUMAN = 1;
    public static final int NPC_AUCTIONEER_CHILTON = 8670;
    public static final int NPC_OLIVIA_BURNSIDE = 2455;
    /** Stormwind tabard designer (creature_template 5193). */
    public static final int NPC_REBECCA_LAUGHLIN = 5193;
    /** PetitionsHandler.cpp GUILD_CHARTER. */
    public static final int ITEM_GUILD_CHARTER = 5863;
    /** PetitionsHandler.cpp GUILD_CHARTER_COST. */
    public static final int GUILD_CHARTER_COST = 1000;
    /** PetitionsHandler.cpp CHARTER_DISPLAY_ID. */
    public static final int CHARTER_DISPLAY_ID = 16161;
    public static final int GAME_EVENT_MIDSUMMER = 1;
    public static final int NPC_LUMA_SKYMOTHER = 25697;
    public static final int GO_ICE_STONE = 187882;
    public static final int GO_ICE_BLOCK = 188067;
    public static final int AUCTION_LIST_DELAY_MS = 300;
    public static final int EQUIP_ERR_NOT_ENOUGH_MONEY = 29;
    public static final int QUEST_STATE_COMPLETE = 0x1;
    public static final int QUEST_STATE_FAIL = 0x2;
    /** QuestDef.h MAX_QUEST_LOG_SIZE. */
    public static final int MAX_QUEST_LOG_SIZE = 25;
    /** QuestDef.h DIALOG_STATUS_NONE / DIALOG_STATUS_AVAILABLE (yellow !). */
    public static final int DIALOG_STATUS_NONE = 0;
    public static final int DIALOG_STATUS_AVAILABLE = 6;
    public static final int QUEST_A_THREAT_WITHIN = 783;
    /** quest_template 2158 Rest and Relaxation; RewItemId1 159 × 5. */
    public static final int QUEST_REST_AND_RELAXATION = 2158;
    /** quest_template 7 Kobold Camp Cleanup; ReqCreatureOrGOId1 6, ReqCreatureOrGOCount1 10. */
    public static final int QUEST_KOBOLD_CAMP_CLEANUP = 7;
    /** quest_template 18 Brotherhood of Thieves; ReqItemId1 752, ReqItemCount1 12. */
    public static final int QUEST_BROTHERHOOD_OF_THIEVES = 18;
    public static final int NPC_KOBOLD_VERMIN = 6;
    public static final int NPC_CORINA_STEELE = 54;
    public static final int NPC_MARSHAL_MCBRIDE = 197;
    public static final int NPC_MARSHAL_DUGHAN = 240;
    public static final int NPC_DEPUTY_WILLEM = 823;
    public static final int NPC_LLANE_BESHERE = 911;
    public static final int NPC_DUNGAR_LONGDRINK = 352;
    public static final int NPC_INNKEEPER_FARLEY = 295;
    /** creature_template GossipMenuId for Farley 295. */
    public static final int GOSSIP_MENU_FARLEY = 1291;
    /** gossip_menu 1291 text_id. */
    public static final int GOSSIP_TEXT_FARLEY = 820;
    /** gossip_menu_option action_menu_id for Farley inn-info. */
    public static final int GOSSIP_MENU_FARLEY_INN_INFO = 1221;
    /** gossip_menu 1221 text_id. */
    public static final int GOSSIP_TEXT_FARLEY_INN_INFO = 1853;
    /** gossip_menu_option.option_text on Farley menu 1291. */
    public static final String GOSSIP_FARLEY_INN_INFO = "What can I do at an inn?";
    public static final int ITEM_WORN_SHORTSWORD = 25;
    /** tbc-db item_template 821; stat_type1 STAMINA 7 / stat_value1 2, armor 65. */
    public static final int ITEM_RIVERPAW_LEATHER_VEST = 821;
    /** tbc-db item_template 2041; stat_type1 AGILITY 3 / 11, stat_type2 STAMINA 7 / 5, armor 92. */
    public static final int ITEM_TUNIC_OF_WESTFALL = 2041;
    /** tbc-db item_template 3306; stat_type1 STRENGTH 4 / 4, stat_type2 STAMINA 7 / 3, armor 162. */
    public static final int ITEM_BRACKWATER_VEST = 3306;
    /** tbc-db item_template 2981; stat_type1 INTELLECT 5 / 6, stat_type2 SPIRIT 6 / 3, armor 35. */
    public static final int ITEM_SEERS_ROBE = 2981;
    /** tbc-db item_template 10399; STR 4 / AGI 3 / STA 11, armor 92. */
    public static final int ITEM_BLACKENED_DEFIAS_ARMOR = 10399;
    /** tbc-db item_template 16726; STR 13 / STA 21 / INT 16 / SPI 8, armor 657. */
    public static final int ITEM_LIGHTFORGE_BREASTPLATE = 16726;
    /** QuestDef.h QUEST_REWARD_CHOICES_COUNT. */
    public static final int QUEST_REWARD_CHOICES_COUNT = 6;
    /** locales_item 2224 Militia Dagger; quest 18 RewChoiceItemId1. */
    public static final int ITEM_MILITIA_DAGGER = 2224;
    /** locales_item 5580 Militia Hammer; quest 18 RewChoiceItemId2. */
    public static final int ITEM_MILITIA_HAMMER = 5580;
    /** locales_item 159 Refreshing Spring Water; quest 2158 RewItemId1. */
    public static final int ITEM_REFRESHING_SPRING_WATER = 159;
    /** locales_item 752 Red Burlap Bandana; quest 18 ReqItemId1. */
    public static final int ITEM_RED_BURLAP_BANDANA = 752;
    /** locales_item 6948 Hearthstone; item_template spellid_1 8690. */
    public static final int ITEM_HEARTHSTONE = 6948;
    public static final int ITEM_ROUGH_ARROW = 2512;
    public static final int ITEM_SMALL_BROWN_POUCH = 4496;
    /** locales_item 889; PageText 16 is locales_page_text (Stalvan to Crillian). */
    public static final int ITEM_DUSTY_UNSENT_LETTER = 889;
    public static final int PAGE_TEXT_STALVAN_CRILLIAN = 16;
    /** locales_item 6351 Dented Crate / Venture Co. supplies. */
    public static final int ITEM_DENTED_CRATE = 6351;
    /** locales_item 5042 Red Ribboned Wrapping Paper. */
    public static final int ITEM_RED_RIBBONED_WRAPPING_PAPER = 5042;
    /** ItemPrototype.h ITEM_FLAG_IS_WRAPPER; named in inventory.md. */
    public static final int ITEM_FLAG_IS_WRAPPER = 0x00000200;
    /** Item.h ITEM_DYNFLAG_UNLOCKED; locked items after EffectOpenLock. */
    public static final int ITEM_DYNFLAG_UNLOCKED = 0x00000004;
    /** Item.h ITEM_DYNFLAG_WRAPPED; named in inventory.md. */
    public static final int ITEM_DYNFLAG_WRAPPED = 0x00000008;
    public static final int SPELL_BATTLE_SHOUT = 6673;
    public static final int TRAINER_SPELL_BATTLE_SHOUT_COST = 200;
    public static final int TAXI_STORMWIND = 2;
    public static final int TAXI_IRONFORGE = 6;
    public static final int ERR_TAXIOK = 0;
    public static final int ERR_TAXINOTVISITED = 6;
    public static final int ZONE_ELWYNN = 12;
    public static final int WEATHER_STATE_FINE = 0;
    /** Weather.h WEATHER_STATE_LIGHT_RAIN. */
    public static final int WEATHER_STATE_LIGHT_RAIN = 3;
    public static final int WEATHER_INSTANT_SMOOTH = 0;
    /** Player.h BuyBankSlotResult. */
    public static final int ERR_BANKSLOT_FAILED_TOO_MANY = 0;
    public static final int ERR_BANKSLOT_INSUFFICIENT_FUNDS = 1;
    public static final int ERR_BANKSLOT_NOTBANKER = 2;
    public static final int ERR_BANKSLOT_OK = 3;
    /**
     * BankBagSlotPrices.dbc id → copper. Index 0 unused.
     * Slot 1..7: 10s, 1g, 10g, 25g, 50g, 100g, 200g.
     */
    public static final int[] BANK_BAG_SLOT_PRICES = {0, 1000, 10_000, 100_000, 250_000, 500_000, 1_000_000, 2_000_000};
    public static final int BACKPACK_START = 23;
    public static final int BACKPACK_END = 39;

    private final ObjectMgr mgr;

    public Content(ObjectMgr mgr) {
        this.mgr = mgr;
    }

    public void gossipHello(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c));
    }

    public void gossipSelect(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 16) {
            return;
        }
        long guid = in.getU64();
        int menuId = in.getU32();
        int gossipListId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c)) {
            return;
        }
        if (!p.hasGossipOption(menuId, gossipListId)) {
            return;
        }
        int option = p.gossipOptionId(gossipListId);
        if (option == GOSSIP_OPTION_VENDOR) {
            if ((c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_LIST_INVENTORY, encodeVendorList(c));
        } else if (option == GOSSIP_OPTION_TRAINER) {
            TrainerHandler.sendList(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_BANKER) {
            InventoryHandler.sendShowBank(c, send);
        } else if (option == GOSSIP_OPTION_TAXIVENDOR) {
            TaxiHandler.sendMenu(p, c, mgr, send);
        } else if (option == GOSSIP_OPTION_INNKEEPER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_INNKEEPER) == 0) {
                return;
            }
            send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            send.accept(Opcodes.SMSG_BINDER_CONFIRM, encodeBinderConfirm(c));
        } else if (option == GOSSIP_OPTION_AUCTIONEER) {
            if ((c.npcFlags & UNIT_NPC_FLAG_AUCTIONEER) == 0) {
                return;
            }
            AuctionHandler.sendHello(c, send);
        } else if (option == GOSSIP_OPTION_GOSSIP) {
            int poiId = p.gossipActionPoi(gossipListId);
            if (poiId != 0) {
                ObjectMgr.PointOfInterest poi = mgr.pointsOfInterest.get(poiId);
                if (poi != null) {
                    send.accept(Opcodes.SMSG_GOSSIP_POI, encodeGossipPoi(poi));
                }
            }
            int next = p.gossipActionMenu(gossipListId);
            if (next > 0) {
                send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(p, c, next));
            } else if (next < 0) {
                send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
            }
        }
    }

    public void listInventory(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || (c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
            return;
        }
        send.accept(Opcodes.SMSG_LIST_INVENTORY, encodeVendorList(c));
    }

    public void buy(Player p, GameMap map, WowBuffer in, boolean inSlot, long nextItemGuid,
                    BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long vendor = in.getU64();
        int itemId = in.getU32();
        int requestedSlot = -1;
        if (inSlot) {
            if (in.remaining() >= 9) {
                in.getU64();
                requestedSlot = in.getU8() & 0xFF;
            }
        }
        int count = in.remaining() > 0 ? Math.max(1, in.getU8()) : 1;
        Creature c = creature(map, vendor);
        if (c == null || outOfRange(p, c) || (c.npcFlags & UNIT_NPC_FLAG_VENDOR) == 0) {
            return;
        }
        List<Integer> stock = mgr.vendorItems.get(c.entry);
        if (stock == null || !stock.contains(itemId)) {
            return;
        }
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        if (t == null) {
            return;
        }
        int price = t.buyPrice * count;
        if (p.money < price) {
            send.accept(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE, encodeEquipErr(EQUIP_ERR_NOT_ENOUGH_MONEY));
            return;
        }
        int slot = requestedSlot;
        if (slot < BACKPACK_START || slot >= BACKPACK_END || slotOccupied(p, slot)) {
            slot = nextBackpackSlot(p);
        }
        if (slot < 0) {
            return;
        }
        p.setMoney(p.money - price);
        Item it = new Item(nextItemGuid, itemId);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = slot;
        it.count = count;
        it.displayId = t.displayId;
        it.inventoryType = t.inventoryType;
        it.quality = t.quality;
        p.items.put(Guid.low(it.guid), it);
        p.dirty = true;
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, it, count));
        itemAddedQuestCheck(p, itemId, count, send);
    }

    public void queryQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || !offersOrInvolves(c.entry, questId)) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, encodeDetails(c.guid, q));
    }

    /** HandleQuestgiverRequestRewardOpcode → PlayerMenu::SendQuestGiverOfferReward. */
    public void requestReward(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || !involves(c.entry, questId)) {
            return;
        }
        if (slotOf(p, questId) < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return;
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_OFFER_REWARD, encodeOfferReward(c.guid, q));
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
            return;
        }
        int status = outOfRange(p, c) ? DIALOG_STATUS_NONE : dialogStatus(p, c);
        WowBuffer out = new WowBuffer(9);
        out.putU64(guid);
        out.putU8(status);
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS, out.array());
    }

    /**
     * CMSG_QUESTGIVER_STATUS_MULTIPLE_QUERY. Player.cpp SendQuestGiverStatusMultiple:
     * visible creatures with UNIT_NPC_FLAG_QUESTGIVER; raw guid + uint8 status; count prefix.
     * Recv ignored. Game-object questgivers are later.
     */
    public void questGiverStatusMultiple(Player p, GameMap map, BiConsumer<Integer, byte[]> send) {
        List<Creature> found = new ArrayList<>();
        for (Creature c : map.nearbyCreatures(p, GameMap.VISIBILITY)) {
            if ((c.npcFlags & UNIT_NPC_FLAG_QUESTGIVER) == 0) {
                continue;
            }
            found.add(c);
        }
        WowBuffer out = new WowBuffer(4 + found.size() * 9);
        out.putU32(found.size());
        for (Creature c : found) {
            out.putU64(c.guid);
            out.putU8(dialogStatus(p, c));
        }
        send.accept(Opcodes.SMSG_QUESTGIVER_STATUS_MULTIPLE, out.array());
    }

    /** Quest-giver markings only (QUEST_STATUS_NONE + CanSeeStartQuest stand-in: template exists, not in log). */
    int dialogStatus(Player p, Creature c) {
        List<Integer> offered = mgr.questGivers.getOrDefault(c.entry, List.of());
        for (int questId : offered) {
            if (mgr.quests.get(questId) == null) {
                continue;
            }
            if (slotOf(p, questId) < 0) {
                return DIALOG_STATUS_AVAILABLE;
            }
        }
        return DIALOG_STATUS_NONE;
    }

    public void acceptQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || !gives(c.entry, questId)) {
            return;
        }
        if (mgr.quests.get(questId) == null) {
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
        p.questLogId[slot] = questId;
        p.questLogState[slot] = 0;
        p.questLogCounts[slot][0] = 0;
        p.questLogCounts[slot][1] = 0;
        p.questLogCounts[slot][2] = 0;
        p.questLogCounts[slot][3] = 0;
        p.questLogItemCount[slot] = 0;
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
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
        p.questLogId[slot] = 0;
        p.questLogState[slot] = 0;
        p.questLogCounts[slot][0] = 0;
        p.questLogCounts[slot][1] = 0;
        p.questLogCounts[slot][2] = 0;
        p.questLogCounts[slot][3] = 0;
        p.questLogItemCount[slot] = 0;
        writeLogField(p, slot);
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base, base + 1, base + 2));
        send.accept(upd.opcode(), upd.payload());
    }

    public void completeQuest(Player p, GameMap map, WowBuffer in, long nextItemGuid,
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
        Creature c = creature(map, guid);
        if (c == null || outOfRange(p, c) || !involves(c.entry, questId)) {
            return;
        }
        int slot = slotOf(p, questId);
        if (slot < 0) {
            return;
        }
        ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
        if (q == null) {
            return;
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
        p.questLogId[slot] = 0;
        p.questLogState[slot] = 0;
        p.questLogCounts[slot][0] = 0;
        p.questLogCounts[slot][1] = 0;
        p.questLogCounts[slot][2] = 0;
        p.questLogCounts[slot][3] = 0;
        p.questLogItemCount[slot] = 0;
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE, encodeQuestComplete(q, xp, money, 0));
    }

    private void storeRewardItem(Player p, int itemId, int count, long nextItemGuid,
                                 BiConsumer<Integer, byte[]> send) {
        if (itemId <= 0 || count <= 0 || nextItemGuid == 0) {
            return;
        }
        int bagSlot = nextBackpackSlot(p);
        if (bagSlot < 0) {
            return;
        }
        Item it = new Item(nextItemGuid, itemId);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = bagSlot;
        it.count = count;
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        if (t != null) {
            it.displayId = t.displayId;
            it.inventoryType = t.inventoryType;
            it.quality = t.quality;
        }
        p.items.put(Guid.low(it.guid), it);
        p.dirty = true;
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, it, it.count));
    }

    public static boolean outOfRange(Player p, Creature c) {
        return p.distance2d(c) > INTERACT_RANGE;
    }

    public static Creature creature(GameMap map, long guid) {
        return guid == 0 ? null : map.creatures.get(guid);
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

    static int slotOf(Player p, int questId) {
        for (int i = 0; i < 25; i++) {
            if (p.questLogId[i] == questId) {
                return i;
            }
        }
        return -1;
    }

    static int freeSlot(Player p) {
        for (int i = 0; i < 25; i++) {
            if (p.questLogId[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    static int nextBackpackSlot(Player p) {
        for (int slot = BACKPACK_START; slot < BACKPACK_END; slot++) {
            if (!slotOccupied(p, slot)) {
                return slot;
            }
        }
        return -1;
    }

    static boolean slotOccupied(Player p, int slot) {
        for (Item it : p.items.values()) {
            if (it.bag == 0 && it.slot == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Player.cpp KilledMonsterCredit / SendQuestUpdateAddCreatureOrGo.
     * Creature req id 1 only (v1); GO objectives later.
     */
    public void killedMonsterCredit(Player p, Creature victim, BiConsumer<Integer, byte[]> send) {
        if (victim == null) {
            return;
        }
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            int reqId = q.reqCreatureOrGOId1();
            int reqCount = q.reqCreatureOrGOCount1();
            if (reqId <= 0 || reqId != victim.entry) {
                continue;
            }
            int cur = p.questLogCounts[slot][0];
            if (cur >= reqCount) {
                continue;
            }
            cur++;
            p.questLogCounts[slot][0] = cur;
            WowBuffer add = new WowBuffer(24);
            add.putU32(questId);
            add.putU32(reqId);
            add.putU32(cur);
            add.putU32(reqCount);
            add.putU64(victim.guid);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_KILL, add.array());
            writeLogField(p, slot);
            int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
            boolean done = cur >= reqCount;
            if (done) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
            }
            var upd = done
                    ? UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base + 1, base + 2))
                    : UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base + 2));
            send.accept(upd.opcode(), upd.payload());
        }
    }

    /**
     * Player.cpp ItemAddedQuestCheck / SendQuestUpdateAddItem.
     * Item req id 1 only (v1). Packet is item u32 + added count u32 — not quest id.
     */
    public void itemAddedQuestCheck(Player p, int entry, int count, BiConsumer<Integer, byte[]> send) {
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int questId = p.questLogId[slot];
            if (questId == 0 || p.questLogState[slot] == QUEST_STATE_COMPLETE) {
                continue;
            }
            ObjectMgr.QuestTemplate q = mgr.quests.get(questId);
            if (q == null) {
                continue;
            }
            int reqId = q.reqItemId1();
            int reqCount = q.reqItemCount1();
            if (reqId <= 0 || reqId != entry) {
                continue;
            }
            int cur = p.questLogItemCount[slot];
            if (cur >= reqCount) {
                continue;
            }
            int add = cur + count <= reqCount ? count : reqCount - cur;
            p.questLogItemCount[slot] = cur + add;
            WowBuffer pkt = new WowBuffer(8);
            pkt.putU32(reqId);
            pkt.putU32(add);
            send.accept(Opcodes.SMSG_QUESTUPDATE_ADD_ITEM, pkt.array());
            if (p.questLogItemCount[slot] >= reqCount) {
                p.questLogState[slot] = QUEST_STATE_COMPLETE;
                writeLogField(p, slot);
                send.accept(Opcodes.SMSG_QUESTUPDATE_COMPLETE, u32(questId));
                int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
                var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, base + 1));
                send.accept(upd.opcode(), upd.payload());
            }
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

    byte[] encodeGossip(Player p, Creature c) {
        return encodeGossip(p, c, mgr.gossipMenuId(c.entry));
    }

    byte[] encodeGossip(Player p, Creature c, int menuId) {
        List<ObjectMgr.GossipMenuItem> items = mgr.gossipOptionsFor(p, c, menuId);
        int[] optionIds = new int[items.size()];
        int[] actionMenus = new int[items.size()];
        int[] actionPois = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            optionIds[i] = items.get(i).optionId();
            actionMenus[i] = items.get(i).actionMenu();
            actionPois[i] = items.get(i).actionPoi();
        }
        p.prepareGossipMenu(menuId, optionIds, actionMenus, actionPois);
        List<Integer> quests = mgr.questGivers.getOrDefault(c.entry, List.of());
        WowBuffer b = new WowBuffer(64);
        b.putU64(c.guid);
        b.putU32(menuId);
        b.putU32(mgr.gossipTextId(menuId));
        b.putU32(items.size());
        int index = 0;
        for (ObjectMgr.GossipMenuItem it : items) {
            b.putU32(index++);
            b.putU8(it.icon());
            b.putU8(it.coded());
            b.putU32(it.boxMoney());
            b.putCString(it.text());
            b.putCString(it.boxText());
        }
        b.putU32(quests.size());
        for (int id : quests) {
            ObjectMgr.QuestTemplate q = mgr.quests.get(id);
            b.putU32(id);
            b.putU32(0);
            b.putU32(q == null ? 1 : q.minLevel());
            b.putCString(q == null ? "" : q.title());
        }
        return b.array();
    }

    static byte[] encodeGossipPoi(ObjectMgr.PointOfInterest poi) {
        WowBuffer b = new WowBuffer(24 + poi.iconName().length());
        b.putU32(poi.flags());
        b.putFloat(poi.x());
        b.putFloat(poi.y());
        b.putU32(poi.icon());
        b.putU32(poi.data());
        b.putCString(poi.iconName());
        return b.array();
    }

    static byte[] encodeBinderConfirm(Creature c) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(c.guid);
        return b.array();
    }

    byte[] encodeVendorList(Creature c) {
        List<Integer> stock = mgr.vendorItems.getOrDefault(c.entry, List.of());
        WowBuffer b = new WowBuffer(32 + stock.size() * 32);
        b.putU64(c.guid);
        if (stock.isEmpty()) {
            b.putU8(0);
            b.putU8(0);
            return b.array();
        }
        b.putU8(stock.size());
        int slot = 1;
        for (int itemId : stock) {
            ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
            b.putU32(slot++);
            b.putU32(itemId);
            b.putU32(t == null ? 0 : t.displayId);
            b.putU32(0xFFFFFFFF);
            b.putU32(t == null ? 0 : t.buyPrice);
            b.putU32(t == null ? 0 : t.maxDurability);
            b.putU32(1);
            b.putU32(0);
        }
        return b.array();
    }

    static byte[] encodePush(Player p, Item it, int count) {
        return encodePush(p, it, count, 1, count);
    }

    public static byte[] encodeLootPush(Player p, Item it, int inventoryTotal) {
        return encodePush(p, it, it.count, 0, inventoryTotal);
    }

    static byte[] encodePush(Player p, Item it, int count, int received, int inventoryTotal) {
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(received);
        b.putU32(0);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(it.slot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(count);
        b.putU32(inventoryTotal);
        return b.array();
    }

    static byte[] encodeEquipErr(int result) {
        WowBuffer b = new WowBuffer(18);
        b.putU8(result);
        b.putU64(0);
        b.putU64(0);
        b.putU8(0);
        return b.array();
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
        ObjectMgr.ItemTemplate t = mgr.items.get(itemId);
        return t == null ? 0 : t.displayId;
    }

    static byte[] encodeDetails(long guid, ObjectMgr.QuestTemplate q) {
        WowBuffer b = new WowBuffer(64);
        b.putU64(guid);
        b.putU32(q.id());
        b.putCString(q.title());
        b.putCString(q.details());
        b.putCString(q.objectives());
        b.putU32(1);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
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

    static byte[] u32(int v) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(v);
        return b.array();
    }
}
