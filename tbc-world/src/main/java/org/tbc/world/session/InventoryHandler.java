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
}
