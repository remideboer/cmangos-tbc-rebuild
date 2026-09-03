package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

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
