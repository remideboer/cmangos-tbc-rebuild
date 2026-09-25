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

    /**
     * TP-SL28-005 — HandleGMTicketUpdateTextOpcode.
     * Open ticket then CMSG_GMTICKET_UPDATETEXT → SMSG_GMTICKET_UPDATETEXT
     * GMTICKET_RESPONSE_UPDATE_SUCCESS 4; GETTICKET text is the new message.
     */
    @Test
    void tpSl28GmTicketUpdateTextSuccess() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "TicketUp");
        a.clear();
        WowBuffer create = new WowBuffer(16);
        create.putCString("stuck");
        a.handle(world, Opcodes.CMSG_GMTICKET_CREATE, create.array());
        a.clear();
        WowBuffer update = new WowBuffer(24);
        update.putCString("still stuck in northshire");
        a.handle(world, Opcodes.CMSG_GMTICKET_UPDATETEXT, update.array());
        byte[] res = lastPayload(a, Opcodes.SMSG_GMTICKET_UPDATETEXT);
        assertEquals(4, WowClientDouble.u32le(res, 0));
        a.clear();
        a.handle(world, Opcodes.CMSG_GMTICKET_GETTICKET, new byte[0]);
        byte[] t = lastPayload(a, Opcodes.SMSG_GMTICKET_GETTICKET);
        assertEquals(0x06, WowClientDouble.u32le(t, 0));
        WowBuffer body = new WowBuffer(t);
        body.getU32();
        assertEquals("still stuck in northshire", body.getCString());
    }

    /**
     * TP-SL28-005 — HandleGMTicketDeleteTicketOpcode.
     * Open ticket then CMSG_GMTICKET_DELETETICKET → SMSG_GMTICKET_DELETETICKET
     * GMTICKET_RESPONSE_TICKET_DELETED 9; GETTICKET then DEFAULT 0x0A.
     */
    @Test
    void tpSl28GmTicketDeleteClearsOpenTicket() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "TicketDel");
        a.clear();
        WowBuffer create = new WowBuffer(16);
        create.putCString("stuck");
        a.handle(world, Opcodes.CMSG_GMTICKET_CREATE, create.array());
        a.clear();
        a.handle(world, Opcodes.CMSG_GMTICKET_DELETETICKET, new byte[0]);
        byte[] del = lastPayload(a, Opcodes.SMSG_GMTICKET_DELETETICKET);
        assertEquals(9, WowClientDouble.u32le(del, 0));
        a.clear();
        a.handle(world, Opcodes.CMSG_GMTICKET_GETTICKET, new byte[0]);
        byte[] t = lastPayload(a, Opcodes.SMSG_GMTICKET_GETTICKET);
        assertEquals(0x0A, WowClientDouble.u32le(t, 0));
        assertEquals(4, t.length);
    }

    /**
     * TP-SL28-005 — HandleGMTicketSystemStatusOpcode.
     * CMSG_GMTICKET_SYSTEMSTATUS → SMSG_GMTICKET_SYSTEMSTATUS u32 1 (queue enabled).
     */
    @Test
    void tpSl28GmTicketSystemStatusEnabled() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "TicketSys");
        a.clear();
        a.handle(world, Opcodes.CMSG_GMTICKET_SYSTEMSTATUS, new byte[0]);
        byte[] st = lastPayload(a, Opcodes.SMSG_GMTICKET_SYSTEMSTATUS);
        assertEquals(1, WowClientDouble.u32le(st, 0));
    }

    /**
     * TP-SL28-006 — CMSG_GMTICKET_GETTICKET with no open ticket is GMTICKET_STATUS_DEFAULT 0x0A
     * (not HASTEXT 0x06), so the client does not show “you have an open ticket”.
     */
    @Test
    void tpSl28GmTicketGetWhenNoneShouldReturnDefaultStatus() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "NoTicket");
        a.clear();
        a.handle(world, Opcodes.CMSG_GMTICKET_GETTICKET, new byte[0]);
        byte[] t = lastPayload(a, Opcodes.SMSG_GMTICKET_GETTICKET);
        assertEquals(0x0A, WowClientDouble.u32le(t, 0));
        assertEquals(4, t.length);
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
