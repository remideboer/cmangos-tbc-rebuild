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

    @Test
    void tpSl19YouLeftWhenLeaveChannel() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Leaver");
        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinGeneral().array());
        assertTrue(client.session().channels.contains("General"));
        client.clear();
        WowBuffer leave = new WowBuffer(32);
        leave.putU32(0);
        leave.putCString("General");
        client.handle(world, Opcodes.CMSG_LEAVE_CHANNEL, leave.array());
        WowBuffer n = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.YOU_LEFT, n.getU8());
        assertEquals("General", n.getCString());
        assertEquals(ChannelHandler.CHANNEL_ID_GENERAL, n.getU32());
        assertEquals(0, n.getU8());
        assertFalse(client.session().channels.contains("General"));
    }

    @Test
    void tpSl19PasswordWhenWrongShouldNotifyWrongPassword() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Pass");
        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinGeneral().array());
        client.clear();
        WowBuffer set = new WowBuffer(48);
        set.putCString("General");
        set.putCString("secret");
        client.handle(world, Opcodes.CMSG_CHANNEL_PASSWORD, set.array());
        WowBuffer changed = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PASSWORD_CHANGED, changed.getU8());
        assertEquals("General", changed.getCString());
        assertEquals(client.session().player().guid, changed.getU64());
        WowBuffer leave = new WowBuffer(32);
        leave.putU32(0);
        leave.putCString("General");
        client.handle(world, Opcodes.CMSG_LEAVE_CHANNEL, leave.array());
        client.clear();
        WowBuffer bad = new WowBuffer(48);
        bad.putU32(0);
        bad.putU8(0);
        bad.putU8(0);
        bad.putCString("General");
        bad.putCString("wrong");
        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, bad.array());
        WowBuffer n = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.WRONG_PASSWORD, n.getU8());
        assertEquals("General", n.getCString());
        assertFalse(client.session().channels.contains("General"));
    }

    @Test
    void tpSl19ModeratorWhenNotMemberShouldNotify() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Mod");
        client.clear();
        WowBuffer mod = new WowBuffer(48);
        mod.putCString("General");
        mod.putCString("Someone");
        client.handle(world, Opcodes.CMSG_CHANNEL_MODERATOR, mod.array());
        WowBuffer n = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, n.getU8());
        assertEquals("General", n.getCString());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_OWNER reports the owner name.
     * SendChannelOwnerResponse: not a member → NOT_MEMBER; else CHANNEL_OWNER 0x0B + name.
     */
    @Test
    void tpSl19ChannelOwner() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Talker");
        client.clear();
        WowBuffer query = new WowBuffer(16);
        query.putCString("General");
        client.handle(world, Opcodes.CMSG_CHANNEL_OWNER, query.array());
        WowBuffer notMember = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());
        assertEquals("General", notMember.getCString());

        client.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinGeneral().array());
        client.clear();
        client.handle(world, Opcodes.CMSG_CHANNEL_OWNER, query.array());
        WowBuffer owner = new WowBuffer(lastPayload(client, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.CHANNEL_OWNER, owner.getU8());
        assertEquals("General", owner.getCString());
        assertEquals("Talker", owner.getCString());

        client.clear();
        client.handle(world, Opcodes.CMSG_CHANNEL_OWNER, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_CHANNEL_NOTIFY));
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_SET_OWNER on a custom channel.
     * Channel::SetOwner: not member 0x05, not owner 0x0A, missing player 0x09;
     * success with 2+ members → OWNER_CHANGED 0x08 + new owner guid.
     */
    @Test
    void tpSl19ChannelSetOwner() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));
        WowBuffer noName = new WowBuffer(16);
        noName.putCString("MyChan");
        a.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, noName.array());
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, setOwnerPayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());
        assertEquals("MyChan", notMember.getCString());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, setOwnerPayload("MyChan", "Talker"));
        WowBuffer notOwner = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_OWNER, notOwner.getU8());
        assertEquals("MyChan", notOwner.getCString());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, setOwnerPayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_SET_OWNER, setOwnerPayload("MyChan", "Wavee"));
        WowBuffer changed = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.OWNER_CHANGED, changed.getU8());
        assertEquals("MyChan", changed.getCString());
        assertEquals(b.session().player().guid, changed.getU64());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.OWNER_CHANGED, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());

        a.clear();
        WowBuffer query = new WowBuffer(16);
        query.putCString("MyChan");
        a.handle(world, Opcodes.CMSG_CHANNEL_OWNER, query.array());
        WowBuffer owner = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.CHANNEL_OWNER, owner.getU8());
        assertEquals("MyChan", owner.getCString());
        assertEquals("Wavee", owner.getCString());
    }

    private static byte[] setOwnerPayload(String channel, String newOwner) {
        WowBuffer in = new WowBuffer(48);
        in.putCString(channel);
        in.putCString(newOwner);
        return in.array();
    }

    private static WowBuffer joinGeneral() {
        return joinChannel("General");
    }

    private static WowBuffer joinChannel(String name) {
        WowBuffer join = new WowBuffer(32);
        join.putU32(0);
        join.putU8(0);
        join.putU8(0);
        join.putCString(name);
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
