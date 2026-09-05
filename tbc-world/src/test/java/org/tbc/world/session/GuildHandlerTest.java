package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GuildHandlerTest {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void inviteWhenUnknownNameShouldSendNotFound() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        createGuild(a.session, world, "Lions");
        a.ops.clear();
        a.last.clear();
        GuildHandler.invite(a.session, world, cstring("Nobody"));
        WowBuffer r = new WowBuffer(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT));
        assertEquals(GuildHandler.GUILD_INVITE_S, r.getU32());
        assertEquals("Nobody", r.getCString());
        assertEquals(GuildHandler.ERR_GUILD_PLAYER_NOT_FOUND_S, r.getU32());
    }

    @Test
    void inviteWhenNotInGuildShouldSendNotInGuild() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        login(world, ACC_B, "Mate");
        GuildHandler.invite(a.session, world, cstring("Mate"));
        WowBuffer r = new WowBuffer(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT));
        assertEquals(GuildHandler.GUILD_CREATE_S, r.getU32());
        assertEquals("", r.getCString());
        assertEquals(GuildHandler.ERR_GUILD_PLAYER_NOT_IN_GUILD, r.getU32());
    }

    @Test
    void inviteWhenWrongFactionShouldSendNotAllied() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        createGuild(a.session, world, "Lions");
        b.session.player().team = 67;
        a.ops.clear();
        a.last.clear();
        GuildHandler.invite(a.session, world, cstring("Mate"));
        assertEquals(GuildHandler.ERR_GUILD_NOT_ALLIED,
                resultOf(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT)));
        assertFalse(b.ops.contains(Opcodes.SMSG_GUILD_INVITE));
    }

    @Test
    void inviteWhenAlreadyInGuildOrInvitedOrNoRightsShouldRefuse() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        createGuild(a.session, world, "Lions");
        b.session.player().guildId = 9;
        a.ops.clear();
        a.last.clear();
        GuildHandler.invite(a.session, world, cstring("Mate"));
        assertEquals(GuildHandler.ERR_ALREADY_IN_GUILD_S,
                resultOf(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT)));

        b.session.player().guildId = 0;
        b.session.player().guildIdInvited = 2;
        a.ops.clear();
        a.last.clear();
        GuildHandler.invite(a.session, world, cstring("Mate"));
        assertEquals(GuildHandler.ERR_ALREADY_INVITED_TO_GUILD_S,
                resultOf(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT)));

        b.session.player().guildIdInvited = 0;
        a.session.player().guildRankRights = 0;
        a.ops.clear();
        a.last.clear();
        GuildHandler.invite(a.session, world, cstring("Mate"));
        assertEquals(GuildHandler.ERR_GUILD_PERMISSIONS,
                resultOf(a.last.get(Opcodes.SMSG_GUILD_COMMAND_RESULT)));
    }

    @Test
    void createWhenAlreadyInGuildShouldIgnore() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        createGuild(a.session, world, "Lions");
        int id = a.session.player().guildId;
        a.ops.clear();
        GuildHandler.create(a.session, world, cstring("Tigers"));
        assertEquals(id, a.session.player().guildId);
        assertEquals("Lions", a.session.player().guildName);
        assertFalse(a.ops.contains(Opcodes.SMSG_GUILD_ROSTER));
    }

    @Test
    void acceptWhenNoInviteOrAlreadyMemberShouldIgnore() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        b.ops.clear();
        GuildHandler.accept(b.session, world);
        assertFalse(b.ops.contains(Opcodes.SMSG_GUILD_EVENT));
        createGuild(a.session, world, "Lions");
        a.session.player().guildIdInvited = a.session.player().guildId;
        a.ops.clear();
        GuildHandler.accept(a.session, world);
        assertFalse(a.ops.contains(Opcodes.SMSG_GUILD_EVENT));
    }

    @Test
    void acceptWhenLeaderWrongFactionShouldIgnore() {
        World world = World.inMemory();
        Sink a = login(world, ACC_A, "Lead");
        Sink b = login(world, ACC_B, "Mate");
        createGuild(a.session, world, "Lions");
        b.session.player().guildIdInvited = a.session.player().guildId;
        a.session.player().team = 67;
        b.ops.clear();
        GuildHandler.accept(b.session, world);
        assertEquals(0, b.session.player().guildId);
        assertFalse(b.ops.contains(Opcodes.SMSG_GUILD_EVENT));
    }

    private static int resultOf(byte[] payload) {
        WowBuffer r = new WowBuffer(payload);
        r.getU32();
        r.getCString();
        return r.getU32();
    }

    private static void createGuild(WorldSession s, World world, String name) {
        GuildHandler.create(s, world, cstring(name));
    }

    private static WowBuffer cstring(String name) {
        WowBuffer b = new WowBuffer(16);
        b.putCString(name);
        return b;
    }

    private static Sink login(World world, World.Account acc, String name) {
        Sink sink = new Sink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.ops.clear();
        sink.last.clear();
        sink.session = s;
        return sink;
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();
        WorldSession session;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last.put(opcode, payload);
        }

        @Override
        public void close() {
        }
    }
}
