package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vendor sell: money, clear bag slot, buyback. */
class InventoryHandlerSellTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void sellItemWhenVendorAcceptsShouldPayClearSlotAndBuyback() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        Creature vendor = spawnVendor(world, p);
        Item it = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        int slot = it.slot;
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD);
        assertNotNull(t);
        assertTrue(t.sellPrice > 0);
        p.setMoney(0);
        sink.ops.clear();
        sink.last.clear();
        InventoryHandler.sellItem(sink.session, world, sell(vendor.guid, UpdateBuilder.itemGuid(it), 0));
        assertEquals(t.sellPrice, p.money);
        assertNull(p.items.get((int) it.guid));
        assertEquals(0, p.getInt(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + slot * 2));
        assertTrue(p.buyback.containsKey(InventoryHandler.BUYBACK_SLOT_START));
        assertEquals(t.sellPrice, p.getInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1));
        assertTrue(sink.ops.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertFalse(sink.ops.contains(Opcodes.SMSG_SELL_ITEM));
    }

    @Test
    void sellItemWhenNoSellPriceShouldSendError() {
        World world = World.inMemory();
        Sink sink = login(world);
        Player p = sink.session.player();
        Creature vendor = spawnVendor(world, p);
        Item it = give(world, p, Content.ITEM_WORN_SHORTSWORD);
        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).sellPrice = 0;
        sink.ops.clear();
        sink.last.clear();
        InventoryHandler.sellItem(sink.session, world, sell(vendor.guid, UpdateBuilder.itemGuid(it), 1));
        assertTrue(sink.ops.contains(Opcodes.SMSG_SELL_ITEM));
        assertTrue(p.items.containsKey((int) it.guid));
        assertEquals(0, p.money);
    }

    private static Creature spawnVendor(World world, Player p) {
        Creature vendor = world.objectMgr.spawnCreature(
                Content.NPC_CORINA_STEELE, 0, p.x, p.y, p.z, p.o, null);
        vendor.npcFlags |= Content.UNIT_NPC_FLAG_VENDOR;
        world.map(p.mapId, p.instanceId).add(vendor);
        return vendor;
    }

    private static Item give(World world, Player p, int entry) {
        Item it = new Item(world.nextItemGuid(), entry);
        it.slot = p.firstFreeBagSlot();
        it.bag = 0;
        it.count = 1;
        p.items.put((int) it.guid, it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2, UpdateBuilder.itemGuid(it));
        return it;
    }

    private static WowBuffer sell(long vendor, long item, int count) {
        WowBuffer b = new WowBuffer(17);
        b.putU64(vendor);
        b.putU64(item);
        b.putU8(count);
        return b;
    }

    private static Sink login(World world) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), "Seller", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
