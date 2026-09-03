package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.InventoryHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL20-* from spec/03-protocol/packets/inventory.md */
class Slice20P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl20SellBuybackSlot74() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Vendor");
        Player p = client.session().player();
        p.money = 10;
        Item it = new Item(world.nextItemGuid(), 25);
        it.slot = 32;
        it.durability = 10;
        p.items.put((int) it.guid, it);
        client.clear();
        WowBuffer sell = new WowBuffer(17);
        sell.putU64(0);
        sell.putU64(it.guid);
        sell.putU8(0);
        client.handle(world, Opcodes.CMSG_SELL_ITEM, sell.array());
        assertEquals(it, p.buyback.get(InventoryHandler.BUYBACK_SLOT_START));
        assertEquals(1, p.getInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1));
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        WowBuffer back = new WowBuffer(12);
        back.putU64(0);
        back.putU32(InventoryHandler.BUYBACK_SLOT_START);
        client.handle(world, Opcodes.CMSG_BUYBACK_ITEM, back.array());
        assertTrue(p.items.containsKey((int) it.guid));
    }

    @Test
    void tpSl20SocketBonusAndMetaReject() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Gemmer");
        Player p = client.session().player();
        Item gear = new Item(world.nextItemGuid(), 25);
        gear.slot = 32;
        p.items.put((int) gear.guid, gear);
        Item gem = new Item(world.nextItemGuid(), 23094);
        gem.slot = 33;
        p.items.put((int) gem.guid, gem);
        client.clear();
        WowBuffer sock = new WowBuffer(32);
        sock.putU64(gear.guid);
        sock.putU64(gem.guid);
        sock.putU64(0);
        sock.putU64(0);
        client.handle(world, Opcodes.CMSG_SOCKET_GEMS, sock.array());
        assertEquals(1, gear.enchant);
        Item metaGear = new Item(world.nextItemGuid(), 25);
        metaGear.slot = 34;
        p.items.put((int) metaGear.guid, metaGear);
        Item meta = new Item(world.nextItemGuid(), InventoryHandler.META_GEM_SKYFIRE);
        meta.slot = 35;
        p.items.put((int) meta.guid, meta);
        WowBuffer bad = new WowBuffer(32);
        bad.putU64(metaGear.guid);
        bad.putU64(meta.guid);
        bad.putU64(0);
        bad.putU64(0);
        client.handle(world, Opcodes.CMSG_SOCKET_GEMS, bad.array());
        assertEquals(0, metaGear.enchant);
    }

    @Test
    void tpSl20RepairDeductsGold() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Smith");
        Player p = client.session().player();
        p.money = 10;
        Item it = new Item(world.nextItemGuid(), 25);
        it.slot = 32;
        it.durability = 10;
        p.items.put((int) it.guid, it);
        client.clear();
        WowBuffer repair = new WowBuffer(17);
        repair.putU64(0);
        repair.putU64(0);
        repair.putU8(0);
        client.handle(world, Opcodes.CMSG_REPAIR_ITEM, repair.array());
        assertEquals(100, it.durability);
        assertEquals(9, p.money);
        assertEquals(9, p.getInt(UpdateFields.PLAYER_FIELD_COINAGE));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
