package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL16-* from packet files, one criterion per method. */
class Slice16P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final Set<Integer> ARENA_MAPS = Set.of(559, 562, 572);

    @Test
    void tpSl16ArenaPortNewWorld() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Pvper");
        client.clear();
        client.handle(world, Opcodes.CMSG_BATTLEMASTER_JOIN_ARENA, new byte[16]);
        byte[] st = lastPayload(client, Opcodes.SMSG_BATTLEFIELD_STATUS);
        assertEquals(2, WowClientDouble.u32le(st, 17));
        client.clear();
        client.battlefieldPort(world, 1);
        int map = WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_NEW_WORLD), 0);
        assertTrue(ARENA_MAPS.contains(map), "map " + map);
    }

    @Test
    void tpSl16HellfireTowerWorldStates() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Pvper");
        Player p = client.session().player();
        world.teleport(p, 530, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, 2480, 1));
        assertTrue(hasWorldState(client, 2476, 1));
        assertTrue(hasWorldState(client, 2478, 1));
    }

    @Test
    void tpSl16LfgListIncludesPlayer() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Lfg");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.CMSG_SET_LOOKING_FOR_GROUP, new byte[8]);
        WowBuffer q = new WowBuffer(12);
        q.putU32(1);
        q.putU32(1);
        q.putU32(0);
        client.handle(world, Opcodes.MSG_LOOKING_FOR_GROUP, q.array());
        WowBuffer list = new WowBuffer(lastPayload(client, Opcodes.MSG_LOOKING_FOR_GROUP));
        list.getU32();
        list.getU32();
        assertEquals(1, list.getU32());
        assertEquals(1, list.getU32());
        assertEquals(p.guid, list.getPackedGuid());
    }

    @Test
    void tpSl16WsgFlagWorldState() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Flag");
        Player p = client.session().player();
        world.teleport(p, 489, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, 1545, 1) || hasWorldState(client, 1546, 1));
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == 23333 || a.spellId() == 23335));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static boolean hasWorldState(WowClientDouble client, int field, int value) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_UPDATE_WORLD_STATE) {
                continue;
            }
            byte[] p = client.payloads.get(i);
            if (WowClientDouble.u32le(p, 0) == field && WowClientDouble.u32le(p, 4) == value) {
                return true;
            }
        }
        return false;
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
