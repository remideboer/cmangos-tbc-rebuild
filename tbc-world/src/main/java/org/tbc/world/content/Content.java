package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.AuctionHandler;
import org.tbc.world.session.InventoryHandler;
import org.tbc.world.session.TaxiHandler;
import org.tbc.world.session.TrainerHandler;

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
    public static final int QUEST_A_THREAT_WITHIN = 783;
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
        if (inSlot) {
            if (in.remaining() >= 9) {
                in.getU64();
                in.getU8();
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
        int slot = nextBackpackSlot(p);
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
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
    }

    public void completeQuest(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
        if (in.remaining() < 12) {
            return;
        }
        long guid = in.getU64();
        int questId = in.getU32();
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
        p.setMoney(p.money + q.rewMoney());
        p.questLogId[slot] = 0;
        p.questLogState[slot] = 0;
        writeLogField(p, slot);
        send.accept(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE, encodeQuestComplete(q));
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
            boolean used = false;
            for (Item it : p.items.values()) {
                if (it.bag == 0 && it.slot == slot) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return slot;
            }
        }
        return -1;
    }

    static void writeLogField(Player p, int slot) {
        int base = UpdateFields.PLAYER_QUEST_LOG_1_1 + slot * 4;
        p.setInt(base, p.questLogId[slot]);
        p.setInt(base + 1, p.questLogState[slot]);
        p.setInt(base + 2, 0);
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

    static byte[] encodeQuestComplete(ObjectMgr.QuestTemplate q) {
        WowBuffer b = new WowBuffer(24);
        b.putU32(q.id());
        b.putU32(0x03);
        b.putU32(0);
        b.putU32(q.rewMoney());
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    static byte[] u32(int v) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(v);
        return b.array();
    }
}
