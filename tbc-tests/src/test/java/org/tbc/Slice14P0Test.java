package org.tbc;

import org.tbc.bdd.WowClientDouble;
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

import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL14-* from packet files, one criterion per method. */
class Slice14P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl14SwapInvItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Swapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item first = new Item(world.nextItemGuid(), 25);
        first.slot = src;
        p.items.put((int) first.guid, first);
        int dst = p.firstFreeBagSlot();
        Item second = new Item(world.nextItemGuid(), 159);
        second.slot = dst;
        p.items.put((int) first.guid, first);
        p.items.put((int) second.guid, second);
        p.setGuid(invSlotField(first.slot), UpdateBuilder.itemGuid(first));
        p.setGuid(invSlotField(second.slot), UpdateBuilder.itemGuid(second));
        int srcSlot = first.slot;
        int dstSlot = second.slot;

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(srcSlot);
        swap.putU8(dstSlot);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        long atSrc = guidAt(update, invSlotField(srcSlot));
        long atDst = guidAt(update, invSlotField(dstSlot));
        assertEquals(UpdateBuilder.itemGuid(second), atSrc);
        assertEquals(UpdateBuilder.itemGuid(first), atDst);
    }

    @Test
    void tpSl14DestroyItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Destroyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int slot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.slot = slot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(slot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(slot);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(slot)));
    }

    @Test
    void tpSl14SplitItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Splitter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item stack = new Item(world.nextItemGuid(), 25);
        stack.slot = src;
        stack.count = 2;
        p.items.put((int) stack.guid, stack);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(stack));
        int dst = p.firstFreeBagSlot();

        client.clear();
        WowBuffer split = new WowBuffer(5);
        split.putU8(0);
        split.putU8(src);
        split.putU8(0);
        split.putU8(dst);
        split.putU8(1);
        client.handle(world, Opcodes.CMSG_SPLIT_ITEM, split.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        Item splitOff = p.itemAt(0, dst);
        assertNotNull(splitOff);
        assertEquals(UpdateBuilder.itemGuid(splitOff), guidAt(update, invSlotField(dst)));
    }

    @Test
    void tpSl14TrainerBuySpell() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Trainee", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(Content.TRAINER_SPELL_BATTLE_SHOUT_COST);
        client.clear();
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());

        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        WowBuffer ok = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertEquals(trainer.guid, ok.getU64());
        assertEquals(Content.SPELL_BATTLE_SHOUT, ok.getU32());
        assertEquals(Content.SPELL_BATTLE_SHOUT, WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_LEARNED_SPELL), 0));
        assertEquals(0, p.money);
    }

    @Test
    void tpSl14ActivateTaxi() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Flyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.relocate(master.x, master.y, master.z, master.o);
        p.learnTaxi(Content.TAXI_STORMWIND);
        p.learnTaxi(Content.TAXI_IRONFORGE);
        client.clear();
        WowBuffer taxi = new WowBuffer(16);
        taxi.putU64(master.guid);
        taxi.putU32(Content.TAXI_STORMWIND);
        taxi.putU32(Content.TAXI_IRONFORGE);
        client.handle(world, Opcodes.CMSG_ACTIVATETAXI, taxi.array());

        assertFalse(client.saw(Opcodes.SMSG_NEW_TAXI_PATH));
        assertTrue(client.saw(Opcodes.SMSG_ACTIVATETAXIREPLY));
        assertEquals(Content.ERR_TAXIOK, WowClientDouble.u32le(client.payload(Opcodes.SMSG_ACTIVATETAXIREPLY), 0));
        WowBuffer move = new WowBuffer(lastPayload(client, Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(p.guid, move.getPackedGuid());
    }

    @Test
    void tpSl14Weather() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rain", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        ObjectMgr.ZoneWeather row = world.objectMgr.weather.get(p.zoneId);
        assertNotNull(row);
        byte[] payload = lastPayload(client, Opcodes.SMSG_WEATHER);
        assertEquals(row.state(), WowClientDouble.u32le(payload, 0));
        assertEquals(row.grade(), WowClientDouble.floatle(payload, 4), 0.001f);
        assertEquals(Content.WEATHER_INSTANT_SMOOTH, payload[8] & 0xFF);
    }

    private static int invSlotField(int slot) {
        return UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + slot * 2;
    }

    private static byte[] lastValuesUpdate(WowClientDouble client) throws Exception {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            int op = client.opcodes.get(i);
            if (op == Opcodes.SMSG_UPDATE_OBJECT) {
                return client.payloads.get(i);
            }
            if (op == Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT) {
                return inflate(client.payloads.get(i));
            }
        }
        throw new AssertionError("no SMSG_UPDATE_OBJECT after inventory mutate");
    }

    private static byte[] inflate(byte[] compressed) throws Exception {
        int size = WowClientDouble.u32le(compressed, 0);
        Inflater inf = new Inflater();
        inf.setInput(compressed, 4, compressed.length - 4);
        byte[] out = new byte[size];
        inf.inflate(out);
        inf.end();
        return out;
    }

    private static long guidAt(byte[] payload, int field) {
        WowBuffer b = new WowBuffer(payload);
        b.getU32();
        b.getU8();
        assertEquals(UpdateBuilder.UPDATETYPE_VALUES, b.getU8());
        b.getPackedGuid();
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int written = 0;
        long low = 0;
        long high = 0;
        boolean sawLow = false;
        boolean sawHigh = false;
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) == 0) {
                continue;
            }
            int v = b.getU32();
            written++;
            if (i == field) {
                low = v & 0xFFFFFFFFL;
                sawLow = true;
            }
            if (i == field + 1) {
                high = v & 0xFFFFFFFFL;
                sawHigh = true;
            }
        }
        assertTrue(written > 0);
        assertTrue(sawLow && sawHigh);
        return low | (high << 32);
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
