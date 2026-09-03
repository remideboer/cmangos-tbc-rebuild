package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.ChannelHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL19-* from spec/03-protocol/packets/chat.md */
class Slice19P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl19YouJoined() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Talker");
        WowBuffer join = joinGeneral();
        client.clear();
        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, join.array());
        WowBuffer n = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.YOU_JOINED, n.getU8());
        assertEquals("General", n.getCString());
        n.getU8();
        assertEquals(ChannelHandler.CHANNEL_ID_GENERAL, n.getU32());
        assertEquals(0, n.getU32());
    }

    @Test
    void tpSl19ChannelListGuids() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Talker");
        Player p = client.session().player();
        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinGeneral().array());
        client.clear();
        WowBuffer list = new WowBuffer(16);
        list.putCString("General");
        client.handle(world, Opcodes.CMSG_CHANNEL_LIST, list.array());
        WowBuffer b = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_LIST));
        b.getU8();
        assertEquals("General", b.getCString());
        b.getU8();
        assertEquals(1, b.getU32());
        assertEquals(p.guid, b.getU64());
        assertEquals(0, b.getU8());
    }

    @Test
    void tpSl19TextEmoteNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.clear();
        b.clear();
        WowBuffer emote = new WowBuffer(16);
        emote.putU32(1);
        emote.putU32(0);
        emote.putU64(b.session().player().guid);
        a.handle(world, Opcodes.CMSG_TEXT_EMOTE, emote.array());
        byte[] payload = lastPayload(a, Opcodes.SMSG_TEXT_EMOTE);
        assertEquals(a.session().player().guid, WowClientDouble.u64le(payload, 0));
        assertEquals(1, WowClientDouble.u32le(payload, 8));
        assertTrue(b.saw(Opcodes.SMSG_TEXT_EMOTE));
    }

    @Test
    void tpSl19VoiceIgnored() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Talker");
        client.clear();
        client.handle(world, Opcodes.CMSG_VOICE_SESSION_ENABLE, new byte[4]);
        assertFalse(client.saw(Opcodes.SMSG_VOICE_SESSION_ROSTER_UPDATE));
    }

    private static WowBuffer joinGeneral() {
        WowBuffer join = new WowBuffer(32);
        join.putU32(0);
        join.putU8(0);
        join.putU8(0);
        join.putCString("General");
        join.putCString("");
        return join;
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
