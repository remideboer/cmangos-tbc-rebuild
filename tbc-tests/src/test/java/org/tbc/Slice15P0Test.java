package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.loot.GroupLoot;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL15-* from packet files, one criterion per method. */
class Slice15P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl15LootNeedVsGreed() {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        WowClientDouble b = new WowClientDouble();
        a.connect(ACC_A);
        b.connect(ACC_B);
        Player pa = world.characters.create(ACC_A.id(), "Need", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player pb = world.characters.create(ACC_B.id(), "Greed", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, pa.guid);
        b.login(world, pb.guid);
        a.groupInvite(world, "Greed");
        b.groupAccept(world);

        Creature corpse = find(world, 6);
        pa.relocate(corpse.x, corpse.y, corpse.z, corpse.o);
        pb.relocate(corpse.x, corpse.y, corpse.z, corpse.o);
        corpse.lootable = true;
        corpse.taggedBy = pa.guid;

        a.clear();
        b.clear();
        WowBuffer method = new WowBuffer(16);
        method.putU32(GroupLoot.METHOD_NEED_BEFORE_GREED);
        method.putU64(pa.guid);
        method.putU32(2);
        a.handle(world, Opcodes.CMSG_LOOT_METHOD, method.array());
        byte[] list = lastPayload(a, Opcodes.SMSG_GROUP_LIST);
        assertEquals(0, list[0] & 0xFF, "party (not raid) type");
        assertEquals(GroupLoot.METHOD_NEED_BEFORE_GREED, lootMethodFromList(list));

        a.clear();
        b.clear();
        a.loot(world, corpse.guid);

        byte[] startA = lastPayload(a, Opcodes.SMSG_LOOT_START_ROLL);
        byte[] startB = lastPayload(b, Opcodes.SMSG_LOOT_START_ROLL);
        assertStartRoll(startA, corpse.guid);
        assertStartRoll(startB, corpse.guid);

        WowBuffer need = roll(corpse.guid, 0, GroupLoot.ROLL_NEED);
        WowBuffer greed = roll(corpse.guid, 0, GroupLoot.ROLL_GREED);
        a.handle(world, Opcodes.CMSG_LOOT_ROLL, need.array());
        b.handle(world, Opcodes.CMSG_LOOT_ROLL, greed.array());

        byte[] won = lastPayload(a, Opcodes.SMSG_LOOT_ROLL_WON);
        WowBuffer w = new WowBuffer(won);
        assertEquals(corpse.guid, w.getU64());
        assertEquals(0, w.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, w.getU32());
        w.getU32();
        w.getU32();
        assertEquals(pa.guid, w.getU64());
        w.getU8();
        assertEquals(GroupLoot.ROLL_NEED, w.getU8());
        assertTrue(b.saw(Opcodes.SMSG_LOOT_ROLL_WON));
    }

    @Test
    void tpSl15RaidConvertReadyCheck() {
        Pair g = loginTwo("Raider", "Mate");
        g.a.clear();
        g.b.clear();
        g.a.handle(g.world, Opcodes.CMSG_GROUP_RAID_CONVERT, new byte[0]);
        assertEquals(1, lastPayload(g.a, Opcodes.SMSG_GROUP_LIST)[0] & 0xFF);
        assertEquals(1, lastPayload(g.b, Opcodes.SMSG_GROUP_LIST)[0] & 0xFF);
        g.a.clear();
        g.b.clear();
        g.a.handle(g.world, Opcodes.MSG_RAID_READY_CHECK, new byte[0]);
        long requester = g.a.session().player().guid;
        assertEquals(requester, WowClientDouble.u64le(lastPayload(g.a, Opcodes.MSG_RAID_READY_CHECK), 0));
        assertEquals(requester, WowClientDouble.u64le(lastPayload(g.b, Opcodes.MSG_RAID_READY_CHECK), 0));
    }

    private static Pair loginTwo(String aName, String bName) {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        WowClientDouble b = new WowClientDouble();
        a.connect(ACC_A);
        b.connect(ACC_B);
        Player pa = world.characters.create(ACC_A.id(), aName, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player pb = world.characters.create(ACC_B.id(), bName, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, pa.guid);
        b.login(world, pb.guid);
        a.groupInvite(world, bName);
        b.groupAccept(world);
        return new Pair(world, a, b);
    }

    private static void assertStartRoll(byte[] payload, long lootGuid) {
        WowBuffer s = new WowBuffer(payload);
        assertEquals(lootGuid, s.getU64());
        assertEquals(0, s.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, s.getU32());
        s.getU32();
        s.getU32();
        assertEquals(GroupLoot.TIMEOUT_MS, s.getU32());
        assertEquals(GroupLoot.VOTE_MASK, s.getU8());
    }

    private static WowBuffer roll(long lootGuid, int slot, int type) {
        WowBuffer b = new WowBuffer(13);
        b.putU64(lootGuid);
        b.putU32(slot);
        b.putU8(type);
        return b;
    }

    /** SMSG_GROUP_LIST loot method byte after leader guid when there is at least one other member. */
    private static int lootMethodFromList(byte[] payload) {
        WowBuffer b = new WowBuffer(payload);
        b.getU8();
        b.getU8();
        b.getU8();
        b.getU8();
        b.getU64();
        int others = b.getU32();
        for (int i = 0; i < others; i++) {
            b.getCString();
            b.getU64();
            b.getU8();
            b.getU8();
            b.getU8();
        }
        b.getU64();
        return b.getU8();
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        throw new AssertionError("no creature entry " + entry);
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }

    private record Pair(World world, WowClientDouble a, WowClientDouble b) {}
}
