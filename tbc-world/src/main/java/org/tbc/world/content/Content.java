package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;

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
    public static final int NPC_AUCTIONEER_CHILTON = 8670;
    public static final int NPC_OLIVIA_BURNSIDE = 2455;
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
    public static final int ITEM_WORN_SHORTSWORD = 25;
    public static final int SPELL_BATTLE_SHOUT = 6673;
    public static final int TRAINER_SPELL_BATTLE_SHOUT_COST = 200;
    public static final int TAXI_STORMWIND = 2;
    public static final int TAXI_IRONFORGE = 6;
    public static final int ERR_TAXIOK = 0;
    public static final int ERR_TAXINOTVISITED = 6;
    public static final int ZONE_ELWYNN = 12;
    public static final int WEATHER_STATE_FINE = 0;
    public static final int WEATHER_INSTANT_SMOOTH = 0;
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
        send.accept(Opcodes.SMSG_GOSSIP_MESSAGE, encodeGossip(c));
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

    byte[] encodeGossip(Creature c) {
        List<Integer> quests = mgr.questGivers.getOrDefault(c.entry, List.of());
        WowBuffer b = new WowBuffer(64);
        b.putU64(c.guid);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
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
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(1);
        b.putU32(0);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(it.slot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(count);
        b.putU32(count);
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
