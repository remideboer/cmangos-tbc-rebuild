package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

/** Bag 0 swap. Layout: spec/03-protocol/packets/inventory.md */
public final class InventoryHandler {
    private InventoryHandler() {}

    public static void swapInvItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int src = in.getU8();
        int dst = in.getU8();
        if (src == dst) {
            return;
        }
        Item a = p.itemAt(0, src);
        Item b = p.itemAt(0, dst);
        if (a != null) {
            a.slot = dst;
        }
        if (b != null) {
            b.slot = src;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + src * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        p.setGuid(srcField, b == null ? 0 : UpdateBuilder.itemGuid(b));
        p.setGuid(dstField, a == null ? 0 : UpdateBuilder.itemGuid(a));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** dstbag, dstslot, srcbag, srcslot. Same pos: ignore. inventory.md */
    public static void swapItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int dstBag = in.getU8();
        int dstSlot = in.getU8();
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        if (srcBag == dstBag && srcSlot == dstSlot) {
            return;
        }
        Item a = p.itemAt(srcBag, srcSlot);
        Item b = p.itemAt(dstBag, dstSlot);
        if (a != null) {
            a.bag = dstBag;
            a.slot = dstSlot;
        }
        if (b != null) {
            b.bag = srcBag;
            b.slot = srcSlot;
        }
        if (srcBag != 0 || dstBag != 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dstSlot * 2;
        p.setGuid(srcField, b == null ? 0 : UpdateBuilder.itemGuid(b));
        p.setGuid(dstField, a == null ? 0 : UpdateBuilder.itemGuid(a));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** bag, slot, count (0 = whole stack). Layout: spec/03-protocol/packets/inventory.md */
    public static void destroyItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 3) {
            return;
        }
        int bag = in.getU8();
        int slot = in.getU8();
        in.getU8();
        Item it = p.itemAt(bag, slot);
        if (it == null) {
            return;
        }
        p.items.remove((int) it.guid);
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        p.setGuid(field, 0);
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot, dstbag, dstslot, count. count 0 or same pos: ignore. inventory.md */
    public static void splitItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 5) {
            return;
        }
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        int dstBag = in.getU8();
        int dstSlot = in.getU8();
        int count = in.getU8();
        if (count == 0 || srcBag != 0 || dstBag != 0 || srcSlot == dstSlot) {
            return;
        }
        Item src = p.itemAt(srcBag, srcSlot);
        if (src == null || src.count <= count || p.itemAt(dstBag, dstSlot) != null) {
            return;
        }
        src.count -= count;
        Item split = new Item(world.nextItemGuid(), src.entry);
        split.ownerGuid = Guid.low(p.guid);
        split.bag = dstBag;
        split.slot = dstSlot;
        split.count = count;
        p.items.put((int) split.guid, split);
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dstSlot * 2;
        p.setGuid(dstField, UpdateBuilder.itemGuid(split));
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(split, p.guid));
        s.send(created.opcode(), created.payload());
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** guid raw banker. Layout: spec/03-protocol/packets/inventory.md */
    public static void bankerActivate(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_BANKER) == 0) {
            return;
        }
        WowBuffer shown = new WowBuffer(8);
        shown.putU64(guid);
        s.send(Opcodes.SMSG_SHOW_BANK, shown.array());
    }

    /** srcbag, srcslot. Always inventory → bank. inventory.md */
    public static void autobankItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        if (srcBag != 0 || srcSlot >= Player.BANK_SLOT_ITEM_START) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        int dst = p.firstFreeBankSlot();
        if (it == null || dst < 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        it.slot = dst;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot. Bank pos → inventory; else → bank. inventory.md */
    public static void autostoreBankItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        if (srcBag != 0) {
            return;
        }
        int dst;
        if (srcSlot >= Player.BANK_SLOT_ITEM_START && srcSlot < Player.BANK_SLOT_ITEM_END) {
            dst = p.firstFreeBagSlot();
        } else {
            dst = p.firstFreeBankSlot();
        }
        Item it = p.itemAt(srcBag, srcSlot);
        if (it == null || dst < 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        it.slot = dst;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot. CanEquipItem then equip or swap. inventory.md */
    public static void autoequipItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        if (srcBag != 0) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        if (it == null) {
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        int invType = t != null ? t.inventoryType : it.inventoryType;
        int dest = world.objectMgr.destEquipSlot(p, invType);
        if (dest < 0 || dest == srcSlot) {
            return;
        }
        Item occupied = p.itemAt(0, dest);
        it.slot = dest;
        if (occupied != null) {
            occupied.slot = srcSlot;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dest * 2;
        p.setGuid(srcField, occupied == null ? 0 : UpdateBuilder.itemGuid(occupied));
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
        if (dest >= Player.INVENTORY_SLOT_BAG_START && dest < Player.INVENTORY_SLOT_BAG_END) {
            WowBuffer opened = new WowBuffer(8);
            opened.putU64(UpdateBuilder.itemGuid(it));
            s.send(Opcodes.SMSG_OPEN_CONTAINER, opened.array());
        }
    }

    /** srcbag, srcslot, dstbag. Store into a free slot of dstbag. inventory.md */
    public static void autostoreBagItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 3) {
            return;
        }
        int srcBag = in.getU8();
        int srcSlot = in.getU8();
        int dstBag = in.getU8();
        if (srcBag != 0 || dstBag != 0) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        int dest = p.firstFreeBagSlot();
        if (it == null || dest < 0 || dest == srcSlot) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dest * 2;
        it.slot = dest;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** item uint32; 0 = clear. PLAYER_AMMO_ID. inventory.md */
    public static void setAmmo(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int item = in.getU32();
        p.setInt(UpdateFields.PLAYER_AMMO_ID, item);
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_AMMO_ID));
        s.send(pkt.opcode(), pkt.payload());
    }

    public static final int BUYBACK_SLOT_START = 74;
    public static final int BONUS_ENCHANTMENT_SLOT = 5;
    public static final int ENCHANT_SLOT_FIELDS = 3;
    public static final int META_GEM_SKYFIRE = 25890;

    public static void sellItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() >= 8) {
            in.getU64();
        }
        long item = in.remaining() >= 8 ? in.getU64() : 0;
        Item it = p.items.remove((int) item);
        if (it == null) {
            return;
        }
        it.slot = BUYBACK_SLOT_START;
        p.buyback.put(BUYBACK_SLOT_START, it);
        p.setInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, 1);
        p.setGuid(UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                p, UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1,
                UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1,
                UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1 + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    public static void buybackItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() >= 8) {
            in.getU64();
        }
        int slot = in.remaining() >= 4 ? in.getU32() : BUYBACK_SLOT_START;
        Item it = p.buyback.remove(slot);
        if (it == null) {
            return;
        }
        int bag = p.firstFreeBagSlot();
        it.slot = bag < 0 ? 23 : bag;
        p.items.put((int) it.guid, it);
        p.setGuid(UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1, 0);
        p.setInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, 0);
        int inv = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        p.setGuid(inv, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                p, UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, inv, inv + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    public static void repairItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() >= 8) {
            in.getU64();
        }
        if (in.remaining() >= 8) {
            in.getU64();
        }
        p.money = Math.max(0, p.money - 1);
        p.setInt(UpdateFields.PLAYER_FIELD_COINAGE, p.money);
        for (Item it : p.items.values()) {
            it.durability = 100;
        }
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
        s.send(pkt.opcode(), pkt.payload());
    }

    public static void socketGems(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long itemGuid = in.remaining() >= 8 ? in.getU64() : 0;
        long gem0 = in.remaining() >= 8 ? in.getU64() : 0;
        if (in.remaining() >= 8) {
            in.getU64();
        }
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Item it = p.items.get((int) itemGuid);
        if (it == null) {
            return;
        }
        Item gem = gem0 != 0 ? p.items.get((int) gem0) : null;
        if (gem != null && gem.entry == META_GEM_SKYFIRE) {
            return;
        }
        if (gem != null) {
            p.items.remove((int) gem.guid);
        }
        it.enchant = 1;
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        s.send(pkt.opcode(), pkt.payload());
    }
}
