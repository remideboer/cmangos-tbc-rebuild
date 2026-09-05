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

    @Test
    void tpSl21PetitionShowList() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Buyer");
        Player p = client.session().player();
        org.tbc.world.entity.Creature npc = null;
        for (org.tbc.world.entity.Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == org.tbc.world.content.Content.NPC_REBECCA_LAUGHLIN) {
                npc = c;
                break;
            }
        }
        assertTrue(npc != null);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        client.clear();
        WowBuffer in = new WowBuffer(8);
        in.putU64(npc.guid);
        client.handle(world, Opcodes.CMSG_PETITION_SHOWLIST, in.array());
        WowBuffer out = new WowBuffer(lastPayload(client, Opcodes.SMSG_PETITION_SHOWLIST));
        assertEquals(npc.guid, out.getU64());
        assertEquals(1, out.getU8());
        assertEquals(1, out.getU32());
        assertEquals(org.tbc.world.content.Content.ITEM_GUILD_CHARTER, out.getU32());
        assertEquals(org.tbc.world.content.Content.CHARTER_DISPLAY_ID, out.getU32());
        assertEquals(org.tbc.world.content.Content.GUILD_CHARTER_COST, out.getU32());
        assertEquals(0, out.getU32());
        assertEquals(9, out.getU32());
    }

    @Test
    void tpSl21PetitionShowSignatures() {
        World world = World.inMemory();
        WowClientDouble a = login(world, "Owner");
        WowClientDouble b = new WowClientDouble();
        b.connect(new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86"));
        Player pb = world.characters.create(2, "Signer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        b.login(world, pb.guid);
        Player owner = a.session().player();
        owner.setMoney(org.tbc.world.content.Content.GUILD_CHARTER_COST);
        org.tbc.world.entity.Creature npc = petitioner(world);
        owner.relocate(npc.x, npc.y, npc.z, npc.o);
        b.session().player().relocate(npc.x, npc.y, npc.z, npc.o);
        a.petitionBuy(world, npc.guid, "SignList", 1);
        long petition = charterGuid(owner);
        b.petitionSign(world, petition);
        a.clear();
        WowBuffer show = new WowBuffer(8);
        show.putU64(petition);
        a.handle(world, Opcodes.CMSG_PETITION_SHOW_SIGNATURES, show.array());
        WowBuffer out = new WowBuffer(lastPayload(a, Opcodes.SMSG_PETITION_SHOW_SIGNATURES));
        assertEquals(petition, out.getU64());
        assertEquals(owner.guid, out.getU64());
        assertEquals((int) petition, out.getU32());
        assertEquals(1, out.getU8());
        assertEquals(b.session().player().guid, out.getU64());
        assertEquals(0, out.getU32());
    }

    @Test
    void tpSl21PetitionRename() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Renamer");
        Player p = client.session().player();
        p.setMoney(org.tbc.world.content.Content.GUILD_CHARTER_COST);
        org.tbc.world.entity.Creature npc = petitioner(world);
        p.relocate(npc.x, npc.y, npc.z, npc.o);
        client.petitionBuy(world, npc.guid, "OldName", 1);
        long petition = charterGuid(p);
        client.clear();
        WowBuffer rename = new WowBuffer(24);
        rename.putU64(petition);
        rename.putCString("NewName");
        client.handle(world, Opcodes.MSG_PETITION_RENAME, rename.array());
        WowBuffer out = new WowBuffer(lastPayload(client, Opcodes.MSG_PETITION_RENAME));
        assertEquals(petition, out.getU64());
        assertEquals("NewName", out.getCString());
    }

    private static org.tbc.world.entity.Creature petitioner(World world) {
        for (org.tbc.world.entity.Creature c : world.map(0, 0).creatures.values()) {
            if (c.entry == org.tbc.world.content.Content.NPC_REBECCA_LAUGHLIN) {
                return c;
            }
        }
        throw new AssertionError("no petitioner");
    }

    private static long charterGuid(Player p) {
        for (org.tbc.world.entity.Item it : p.items.values()) {
            if (it.entry == org.tbc.world.content.Content.ITEM_GUILD_CHARTER) {
                return it.guid;
            }
        }
        throw new AssertionError("no charter");
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
