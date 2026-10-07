package org.tbc.world.content;

import static org.tbc.world.content.Content.BACKPACK_END;
import static org.tbc.world.content.Content.BACKPACK_START;
import static org.tbc.world.content.Content.EQUIP_ERR_NOT_ENOUGH_MONEY;
import static org.tbc.world.content.Content.UNIT_NPC_FLAG_VENDOR;
import static org.tbc.world.content.Content.creature;
import static org.tbc.world.content.Content.encodeEquipErr;
import static org.tbc.world.content.Content.encodePush;
import static org.tbc.world.content.Content.outOfRange;
import static org.tbc.world.content.Content.slotOccupied;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

/** NPC vendor: CMSG_LIST_INVENTORY / CMSG_BUY_ITEM(_IN_SLOT) and the SMSG_LIST_INVENTORY encoding (split out of Content). */
final class Vendor {

    /** Quest-log hook after a purchase lands in the bags (Content.itemAddedQuestCheck). */
    interface ItemAddedHook {
        void itemAdded(Player p, GameMap map, int entry, int count, BiConsumer<Integer, byte[]> send);
    }

    private final ObjectMgr mgr;
    private final ItemAddedHook onItemAdded;

    Vendor(ObjectMgr mgr, ItemAddedHook onItemAdded) {
        this.mgr = mgr;
        this.onItemAdded = onItemAdded;
    }

    void listInventory(Player p, GameMap map, WowBuffer in, BiConsumer<Integer, byte[]> send) {
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

    void buy(Player p, GameMap map, WowBuffer in, boolean inSlot, LongSupplier nextItemGuid,
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
        List<Integer> stock = mgr.itemsForVendor(c.entry);
        if (stock.isEmpty() || !stock.contains(itemId)) {
            return;
        }
        ObjectMgr.ItemTemplate t = mgr.item(itemId);
        if (t == null) {
            return;
        }
        int price = t.buyPrice * count;
        if (p.money < price) {
            send.accept(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE, encodeEquipErr(EQUIP_ERR_NOT_ENOUGH_MONEY));
            return;
        }
        LongSupplier guids = nextItemGuid;
        if (guids == null) {
            return;
        }
        // Prefer merge into existing stacks (CMaNGOS CanStore); requestedSlot only when empty non-stack.
        List<ObjectMgr.StoredItem> stored;
        if (inSlot && requestedSlot >= BACKPACK_START && requestedSlot < BACKPACK_END
                && !slotOccupied(p, requestedSlot) && t.stackable <= 1) {
            Item it = new Item(guids.getAsLong(), itemId);
            it.ownerGuid = Guid.low(p.guid);
            it.bag = 0;
            it.slot = requestedSlot;
            it.count = count;
            it.displayId = t.displayId;
            it.quality = t.quality;
            ObjectMgr.applyWeaponProto(it, t);
            p.items.put(Guid.low(it.guid), it);
            p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + requestedSlot * 2,
                    Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
            stored = List.of(new ObjectMgr.StoredItem(it, count, true));
        } else {
            stored = mgr.storeNewItem(p, itemId, count, guids);
        }
        if (stored.isEmpty()) {
            return;
        }
        p.setMoney(p.money - price);
        p.dirty = true;
        int total = 0;
        for (Item x : p.items.values()) {
            if (x.entry == itemId) {
                total += x.count;
            }
        }
        boolean anyCreated = false;
        for (ObjectMgr.StoredItem s : stored) {
            if (s.created()) {
                anyCreated = true;
                var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(s.item(), p.guid));
                send.accept(created.opcode(), created.payload());
                int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + s.item().slot * 2;
                var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
                send.accept(inv.opcode(), inv.payload());
            } else {
                var stack = UpdateBuilder.maybeCompress(
                        UpdateBuilder.valuesItem(s.item(), UpdateFields.ITEM_FIELD_STACK_COUNT));
                send.accept(stack.opcode(), stack.payload());
            }
        }
        var coin = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
        send.accept(coin.opcode(), coin.payload());
        int vendorSlot = stock.indexOf(itemId) + 1;
        WowBuffer bought = new WowBuffer(20);
        bought.putU64(c.guid);
        bought.putU32(vendorSlot);
        bought.putU32(0xFFFFFFFF);
        bought.putU32(count);
        send.accept(Opcodes.SMSG_BUY_ITEM, bought.array());
        Item pushItem = stored.get(0).item();
        // storeNewItem appends creates after merges, so the last entry is the new slot when anyCreated.
        int pushSlot = anyCreated ? stored.get(stored.size() - 1).item().slot : 0xFFFFFFFF;
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodePush(p, pushItem, count, 1, total, pushSlot));
        onItemAdded.itemAdded(p, map, itemId, count, send);
    }

    byte[] encodeVendorList(Creature c) {
        List<Integer> stock = mgr.itemsForVendor(c.entry);
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
            ObjectMgr.ItemTemplate t = mgr.item(itemId);
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
}
