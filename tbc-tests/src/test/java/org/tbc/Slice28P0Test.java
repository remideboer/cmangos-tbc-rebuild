package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL28-* from quest.md / loot.md / misc-player.md / lfg.md */
class Slice28P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl28PushQuestResult() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Leader");
        WowClientDouble b = login(world, ACC_B, "Share");
        a.groupInvite(world, "Share");
        b.groupAccept(world);
        a.clear();
        WowBuffer push = new WowBuffer(4);
        push.putU32(783);
        a.handle(world, Opcodes.CMSG_PUSHQUESTTOPARTY, push.array());
        byte[] res = lastPayload(a, Opcodes.MSG_QUEST_PUSH_RESULT);
        assertEquals(a.session().player().guid, WowClientDouble.u64le(res, 0));
        assertTrue(b.saw(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
    }

    @Test
    void tpSl28MasterLootGive() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Leader");
        WowClientDouble b = login(world, ACC_B, "Share");
        a.groupInvite(world, "Share");
        b.groupAccept(world);
        Player mate = b.session().player();
        int before = mate.items.size();
        b.clear();
        WowBuffer give = new WowBuffer(17);
        give.putU64(1);
        give.putU8(0);
        give.putU64(mate.guid);
        a.handle(world, Opcodes.CMSG_LOOT_MASTER_GIVE, give.array());
        assertTrue(mate.items.size() > before);
        byte[] push = lastPayload(b, Opcodes.SMSG_ITEM_PUSH_RESULT);
        assertEquals(mate.guid, WowClientDouble.u64le(push, 0));
        assertEquals(25, WowClientDouble.u32le(push, 8 + 4 + 4 + 4 + 1 + 4));
    }

    @Test
    void tpSl28GmTicketHasText() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Leader");
        a.clear();
        WowBuffer ticket = new WowBuffer(16);
        ticket.putCString("stuck");
        a.handle(world, Opcodes.CMSG_GMTICKET_CREATE, ticket.array());
        a.handle(world, Opcodes.CMSG_GMTICKET_GETTICKET, new byte[0]);
        byte[] t = lastPayload(a, Opcodes.SMSG_GMTICKET_GETTICKET);
        assertEquals(0x06, WowClientDouble.u32le(t, 0));
    }

    @Test
    void tpSl28LfgAccept() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Leader");
        a.clear();
        a.handle(world, Opcodes.CMSG_ACCEPT_LFG_MATCH, new byte[0]);
        byte[] u = lastPayload(a, Opcodes.SMSG_LFG_UPDATE);
        assertEquals(1, u[0] & 0xFF);
    }

    private static WowClientDouble login(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
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
