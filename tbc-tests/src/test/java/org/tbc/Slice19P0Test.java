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

    /**
     * TP-SL19-008 — CMSG_CHANNEL_UNMODERATOR clears MEMBER_FLAG_MODERATOR.
     * SetModeFlags: not member 0x05, not moderator 0x06; success MODE_CHANGE 0x0C.
     */
    @Test
    void tpSl19ChannelUnmoderator() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNMODERATOR, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_UNMODERATOR, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());
        assertEquals("MyChan", notMember.getCString());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_UNMODERATOR, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());
        assertEquals("MyChan", notMod.getCString());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNMODERATOR, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_MODERATOR, namePayload("MyChan", "Wavee"));
        WowBuffer granted = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, granted.getU8());
        assertEquals("MyChan", granted.getCString());
        assertEquals(b.session().player().guid, granted.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, granted.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_MODERATOR, granted.getU8());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNMODERATOR, namePayload("MyChan", "Wavee"));
        WowBuffer revoked = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, revoked.getU8());
        assertEquals("MyChan", revoked.getCString());
        assertEquals(b.session().player().guid, revoked.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_MODERATOR, revoked.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, revoked.getU8());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_MUTE sets MEMBER_FLAG_MUTED.
     * SetMute → SetModeFlags; success MODE_CHANGE 0x0C old 0 new 0x08.
     */
    @Test
    void tpSl19ChannelMute() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_MUTE, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_MUTE, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_MUTE, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_MUTE, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_MUTE, namePayload("MyChan", "Wavee"));
        WowBuffer muted = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, muted.getU8());
        assertEquals("MyChan", muted.getCString());
        assertEquals(b.session().player().guid, muted.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, muted.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_MUTED, muted.getU8());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, toB.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_MUTED, toB.getU8());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_UNMUTE clears MEMBER_FLAG_MUTED.
     * SetMute(..., false) → MODE_CHANGE 0x0C old 0x08 new 0.
     */
    @Test
    void tpSl19ChannelUnmute() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        a.handle(world, Opcodes.CMSG_CHANNEL_MUTE, namePayload("MyChan", "Wavee"));

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNMUTE, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_UNMUTE, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_UNMUTE, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNMUTE, namePayload("MyChan", "Wavee"));
        WowBuffer unmuted = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, unmuted.getU8());
        assertEquals("MyChan", unmuted.getCString());
        assertEquals(b.session().player().guid, unmuted.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_MUTED, unmuted.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, unmuted.getU8());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.MODE_CHANGE, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());
        assertEquals(ChannelHandler.MEMBER_FLAG_MUTED, toB.getU8());
        assertEquals(ChannelHandler.MEMBER_FLAG_NONE, toB.getU8());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_INVITE: INVITE 0x18 to the target, PLAYER_INVITED 0x1D to the inviter.
     * Channel::Invite — not-member 0x05, missing 0x09, already 0x17, wrong faction 0x19.
     */
    @Test
    void tpSl19ChannelInvite() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Wavee"));
        WowBuffer invited = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_INVITED, invited.getU8());
        assertEquals("MyChan", invited.getCString());
        assertEquals("Wavee", invited.getCString());
        WowBuffer invite = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.INVITE, invite.getU8());
        assertEquals("MyChan", invite.getCString());
        assertEquals(a.session().player().guid, invite.getU64());

        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Wavee"));
        WowBuffer already = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_ALREADY_MEMBER, already.getU8());
        assertEquals("MyChan", already.getCString());
        assertEquals(b.session().player().guid, already.getU64());

        WowClientDouble horde = login(world, new World.Account(4, "HORDE", new byte[40], 3, 1, "Win", "x86"), "Grunt", 2);
        a.clear();
        horde.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Grunt"));
        WowBuffer faction = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.INVITE_WRONG_FACTION, faction.getU8());
        assertEquals("MyChan", faction.getCString());
        assertFalse(horde.saw(Opcodes.SMSG_CHANNEL_NOTIFY));
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_KICK broadcasts PLAYER_KICKED 0x12 (target, source).
     * Channel::KickOrBan(..., false).
     */
    @Test
    void tpSl19ChannelKick() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_KICK, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Outsider"));
        WowBuffer notOn = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, notOn.getU8());
        assertEquals("MyChan", notOn.getCString());
        assertEquals("Outsider", notOn.getCString());

        a.handle(world, Opcodes.CMSG_CHANNEL_MODERATOR, namePayload("MyChan", "Wavee"));
        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Talker"));
        WowBuffer notOwner = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_OWNER, notOwner.getU8());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_KICK, namePayload("MyChan", "Wavee"));
        WowBuffer kicked = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_KICKED, kicked.getU8());
        assertEquals("MyChan", kicked.getCString());
        assertEquals(b.session().player().guid, kicked.getU64());
        assertEquals(a.session().player().guid, kicked.getU64());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_KICKED, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());
        assertEquals(a.session().player().guid, toB.getU64());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_OWNER, channelOnly("MyChan"));
        WowBuffer left = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, left.getU8());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_BAN broadcasts PLAYER_BANNED 0x14 and blocks rejoin (BANNED 0x13).
     * Channel::KickOrBan(..., true).
     */
    @Test
    void tpSl19ChannelBan() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_BAN, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_BAN, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_BAN, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_BAN, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_BAN, namePayload("MyChan", "Wavee"));
        WowBuffer banned = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_BANNED, banned.getU8());
        assertEquals("MyChan", banned.getCString());
        assertEquals(b.session().player().guid, banned.getU64());
        assertEquals(a.session().player().guid, banned.getU64());
        WowBuffer toB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_BANNED, toB.getU8());
        assertEquals("MyChan", toB.getCString());
        assertEquals(b.session().player().guid, toB.getU64());
        assertEquals(a.session().player().guid, toB.getU64());

        b.clear();
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        WowBuffer rejoin = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.BANNED, rejoin.getU8());
        assertEquals("MyChan", rejoin.getCString());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_INVITE, namePayload("MyChan", "Wavee"));
        WowBuffer inviteBanned = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_INVITE_BANNED, inviteBanned.getU8());
        assertEquals("MyChan", inviteBanned.getCString());
        assertEquals("Wavee", inviteBanned.getCString());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_UNBAN broadcasts PLAYER_UNBANNED 0x15 so the target can rejoin.
     * Channel::UnBan; not banned → PLAYER_NOT_BANNED 0x16.
     */
    @Test
    void tpSl19ChannelUnban() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, namePayload("MyChan", "Wavee"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, namePayload("MyChan", "Talker"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, namePayload("MyChan", "Nobody"));
        WowBuffer missing = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_FOUND, missing.getU8());
        assertEquals("MyChan", missing.getCString());
        assertEquals("Nobody", missing.getCString());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, namePayload("MyChan", "Outsider"));
        WowBuffer notBanned = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_NOT_BANNED, notBanned.getU8());
        assertEquals("MyChan", notBanned.getCString());
        assertEquals("Outsider", notBanned.getCString());

        a.handle(world, Opcodes.CMSG_CHANNEL_BAN, namePayload("MyChan", "Wavee"));
        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_UNBAN, namePayload("MyChan", "Wavee"));
        WowBuffer unbanned = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.PLAYER_UNBANNED, unbanned.getU8());
        assertEquals("MyChan", unbanned.getCString());
        assertEquals(b.session().player().guid, unbanned.getU64());
        assertEquals(a.session().player().guid, unbanned.getU64());

        b.clear();
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        WowBuffer rejoined = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.YOU_JOINED, rejoined.getU8());
        assertEquals("MyChan", rejoined.getCString());
    }

    /**
     * TP-SL19-008 — CMSG_CHANNEL_ANNOUNCEMENTS toggles 0x0D/0x0E (custom default on → first toggle off).
     * Channel::ToggleAnnouncements.
     */
    @Test
    void tpSl19ChannelAnnouncements() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Talker");
        WowClientDouble b = login(world, ACC_B, "Wavee");
        a.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());
        b.handle(world, Opcodes.CMSG_JOIN_CHANNEL, joinChannel("MyChan").array());

        a.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS, new byte[0]);
        assertFalse(a.saw(Opcodes.SMSG_CHANNEL_NOTIFY));

        WowClientDouble outsider = login(world, new World.Account(3, "OUT", new byte[40], 3, 1, "Win", "x86"), "Outsider");
        outsider.clear();
        outsider.handle(world, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS, channelOnly("MyChan"));
        WowBuffer notMember = new WowBuffer(lastPayload(outsider, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MEMBER, notMember.getU8());

        b.clear();
        b.handle(world, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS, channelOnly("MyChan"));
        WowBuffer notMod = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.NOT_MODERATOR, notMod.getU8());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS, channelOnly("MyChan"));
        WowBuffer off = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.ANNOUNCEMENTS_OFF, off.getU8());
        assertEquals("MyChan", off.getCString());
        assertEquals(a.session().player().guid, off.getU64());
        WowBuffer offB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.ANNOUNCEMENTS_OFF, offB.getU8());

        a.clear();
        b.clear();
        a.handle(world, Opcodes.CMSG_CHANNEL_ANNOUNCEMENTS, channelOnly("MyChan"));
        WowBuffer on = new WowBuffer(lastPayload(a, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.ANNOUNCEMENTS_ON, on.getU8());
        assertEquals("MyChan", on.getCString());
        assertEquals(a.session().player().guid, on.getU64());
        WowBuffer onB = new WowBuffer(lastPayload(b, Opcodes.SMSG_CHANNEL_NOTIFY));
        assertEquals(ChannelHandler.ANNOUNCEMENTS_ON, onB.getU8());
    }

    private static byte[] channelOnly(String channel) {
        WowBuffer in = new WowBuffer(16);
        in.putCString(channel);
        return in.array();
    }

    private static byte[] namePayload(String channel, String player) {
        WowBuffer in = new WowBuffer(48);
        in.putCString(channel);
        in.putCString(player);
        return in.array();
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
        return login(world, acc, name, 1);
    }

    private static WowClientDouble login(World world, World.Account acc, String name, int race) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, race, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
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
