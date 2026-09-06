package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL23-* from battleground.md */
class Slice23P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_C =
            new World.Account(3, "THIRD", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl23PetitionRosterRating() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Arena");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.CMSG_TURN_IN_PETITION, new byte[8]);
        assertEquals(1, p.arenaTeam);
        WowBuffer r = new WowBuffer(lastPayload(client, Opcodes.SMSG_ARENA_TEAM_ROSTER));
        r.getU32();
        r.getU32();
        r.getU32();
        assertEquals(p.guid, r.getU64());
        r.getU8();
        assertEquals(p.name, r.getCString());
        r.getU32();
        r.getU8();
        r.getU8();
        r.getU32();
        r.getU32();
        r.getU32();
        r.getU32();
        assertEquals(0, r.getU32(), "personalRating");
    }

    @Test
    void tpSl23PvpLogTypeBg() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Arena");
        Player p = client.session().player();
        world.teleport(p, 489, 0, 0, 0, 0);
        client.clear();
        client.handle(world, Opcodes.MSG_PVP_LOG_DATA, new byte[0]);
        byte[] log = lastPayload(client, Opcodes.MSG_PVP_LOG_DATA);
        assertEquals(0, log[0] & 0xFF, "BG type");
        assertEquals(0, log[1] & 0xFF, "not ended");
        assertEquals(0, WowClientDouble.u32le(log, 2), "scoreCount");
    }

    @Test
    void tpSl23PvpLogWhenNotInBgShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "World");
        client.clear();
        client.handle(world, Opcodes.MSG_PVP_LOG_DATA, new byte[0]);
        assertTrue(client.opcodes.stream().noneMatch(op -> op == Opcodes.MSG_PVP_LOG_DATA));
    }

    @Test
    void tpSl23AfkThreeUniqueReporters() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Afk");
        WowClientDouble b = login(world, ACC_B, "Rep1");
        WowClientDouble c = login(world, ACC_C, "Rep2");
        Player target = a.session().player();
        WowBuffer report = new WowBuffer(8);
        report.putU64(target.guid);
        b.handle(world, Opcodes.CMSG_REPORT_PVP_AFK, report.array());
        c.handle(world, Opcodes.CMSG_REPORT_PVP_AFK, report.array());
        a.handle(world, Opcodes.CMSG_REPORT_PVP_AFK, report.array());
        assertTrue(target.auras.stream().anyMatch(aura -> aura.spellId() == PvpObjectives.IDLE_AFK));
    }

    @Test
    void tpSl23ArenaTeamInviteShouldSendInvitePacket() {
        World world = World.inMemory();
        WowClientDouble captain = login(world, ACC_A, "Captain");
        WowClientDouble invitee = login(world, ACC_B, "Invitee");
        Player cap = captain.session().player();
        Player inv = invitee.session().player();
        inv.level = 70;
        org.tbc.world.entity.ArenaTeam team = new org.tbc.world.entity.ArenaTeam();
        team.id = 7;
        team.slot = 0;
        team.name = "Glads";
        team.captainGuid = cap.guid;
        team.members.put(cap.guid, new org.tbc.world.entity.ArenaTeam.Member());
        world.objectMgr.arenaTeams.put(team.id, team);
        cap.arenaTeam = team.id;
        invitee.clear();
        WowBuffer in = new WowBuffer(32);
        in.putU32(team.id);
        in.putCString("Invitee");
        captain.handle(world, Opcodes.CMSG_ARENA_TEAM_INVITE, in.array());
        WowBuffer out = new WowBuffer(lastPayload(invitee, Opcodes.SMSG_ARENA_TEAM_INVITE));
        assertEquals("Captain", out.getCString());
        assertEquals("Glads", out.getCString());
        assertEquals(team.id, inv.arenaTeamIdInvited);
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
