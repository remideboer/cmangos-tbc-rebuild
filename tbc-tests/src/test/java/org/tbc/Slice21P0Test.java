package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.GuildHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL21-* from guild.md / group.md */
class Slice21P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl21GuildQueryResponse() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Guilded");
        Player p = client.session().player();
        p.guildId = 1;
        p.guildName = "Plates";
        client.clear();
        WowBuffer q = new WowBuffer(4);
        q.putU32(1);
        client.handle(world, Opcodes.CMSG_GUILD_QUERY, q.array());
        WowBuffer g = new WowBuffer(lastPayload(client, Opcodes.SMSG_GUILD_QUERY_RESPONSE));
        assertEquals(1, g.getU32());
        assertEquals("Plates", g.getCString());
    }

    @Test
    void tpSl21GuildBankTabPermissions() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Guilded");
        Player p = client.session().player();
        p.guildId = 1;
        p.money = 200000;
        client.clear();
        client.handle(world, Opcodes.CMSG_GUILD_BANK_BUY_TAB, new byte[8]);
        assertEquals(200000 - GuildHandler.TAB_PRICE, p.money);
        byte[] perm = lastPayload(client, Opcodes.MSG_GUILD_PERMISSIONS);
        assertEquals(1, perm[12] & 0xFF);
        assertTrue(perm.length >= 13 + GuildHandler.GUILD_BANK_MAX_TABS * 8);
    }

    @Test
    void tpSl21RollAndPing() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Guilded");
        Player p = client.session().player();
        client.clear();
        WowBuffer roll = new WowBuffer(8);
        roll.putU32(1);
        roll.putU32(100);
        client.handle(world, Opcodes.MSG_RANDOM_ROLL, roll.array());
        byte[] r = lastPayload(client, Opcodes.MSG_RANDOM_ROLL);
        assertEquals(1, WowClientDouble.u32le(r, 0));
        assertEquals(100, WowClientDouble.u32le(r, 4));
        int rolled = WowClientDouble.u32le(r, 8);
        assertTrue(rolled >= 1 && rolled <= 100);
        assertEquals(p.guid, WowClientDouble.u64le(r, 12));
        WowBuffer ping = new WowBuffer(8);
        ping.putFloat(1.5f);
        ping.putFloat(2.5f);
        client.handle(world, Opcodes.MSG_MINIMAP_PING, ping.array());
        byte[] pingP = lastPayload(client, Opcodes.MSG_MINIMAP_PING);
        assertEquals(p.guid, WowClientDouble.u64le(pingP, 0));
        assertEquals(1.5f, WowClientDouble.floatle(pingP, 8), 0.01);
        assertEquals(2.5f, WowClientDouble.floatle(pingP, 12), 0.01);
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
