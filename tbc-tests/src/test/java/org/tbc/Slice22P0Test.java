package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.Honor;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL22-* from honor.md / inspect-duel.md */
class Slice22P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl22HonorCapAndMidnight() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Killer");
        WowClientDouble b = login(world, ACC_B, "Victim");
        Player killer = a.session().player();
        Player victim = b.session().player();
        killer.honorPoints = 74990;
        a.clear();
        Honor.reward(killer, a.session(), victim.guid, 20);
        byte[] credit = lastPayload(a, Opcodes.SMSG_PVP_CREDIT);
        assertEquals(20, WowClientDouble.u32le(credit, 0));
        assertEquals(victim.guid, WowClientDouble.u64le(credit, 4));
        assertEquals(Honor.MAX_HONOR_POINTS, killer.honorPoints);
        Honor.midnightRoll(killer);
        assertEquals(0, killer.honorToday);
        assertEquals(20, killer.honorYesterday);
    }

    @Test
    void tpSl22InspectPackedGuid() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Killer");
        WowClientDouble b = login(world, ACC_B, "Victim");
        Player victim = b.session().player();
        a.clear();
        WowBuffer insp = new WowBuffer(8);
        insp.putU64(victim.guid);
        a.handle(world, Opcodes.CMSG_INSPECT, insp.array());
        WowBuffer tal = new WowBuffer(lastPayload(a, Opcodes.SMSG_INSPECT_TALENT));
        assertEquals(victim.guid, tal.getPackedGuid());
        assertEquals(0x3D, tal.getU32());
        WowBuffer hon = new WowBuffer(8);
        hon.putU64(victim.guid);
        a.handle(world, Opcodes.MSG_INSPECT_HONOR_STATS, hon.array());
        WowBuffer hs = new WowBuffer(lastPayload(a, Opcodes.MSG_INSPECT_HONOR_STATS));
        assertEquals(victim.guid, hs.getU64());
    }

    @Test
    void tpSl22DuelCountdownOutOfBounds() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Killer");
        WowClientDouble b = login(world, ACC_B, "Victim");
        a.session().player().selection = b.session().player().guid;
        WowBuffer go = new WowBuffer(8);
        go.putU64(7);
        a.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(b.saw(Opcodes.SMSG_DUEL_REQUESTED));
        a.clear();
        a.handle(world, Opcodes.CMSG_DUEL_ACCEPTED, new byte[0]);
        assertEquals(3000, WowClientDouble.u32le(lastPayload(a, Opcodes.SMSG_DUEL_COUNTDOWN), 0));
        a.clear();
        a.heartbeat(world, 1000, 1000, 0, 0);
        assertTrue(a.saw(Opcodes.SMSG_DUEL_OUTOFBOUNDS));
    }

    @Test
    void tpSl22SetTitleWhenKnownShouldSetChosenTitle() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Titled");
        Player p = client.session().player();
        p.knownTitles.add(1);
        client.clear();
        WowBuffer in = new WowBuffer(4);
        in.putU32(1);
        client.handle(world, Opcodes.CMSG_SET_TITLE, in.array());
        assertEquals(1, p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_CHOSEN_TITLE));
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void tpSl22InspectArenaTeams() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Viewer");
        WowClientDouble b = login(world, ACC_B, "Target");
        Player target = b.session().player();
        org.tbc.world.entity.ArenaTeam team = new org.tbc.world.entity.ArenaTeam();
        team.id = 42;
        team.slot = 0;
        team.rating = 1600;
        team.gamesSeason = 20;
        team.winsSeason = 12;
        org.tbc.world.entity.ArenaTeam.Member mem = new org.tbc.world.entity.ArenaTeam.Member();
        mem.gamesSeason = 10;
        mem.personalRating = 1500;
        team.members.put(target.guid, mem);
        world.objectMgr.arenaTeams.put(team.id, team);
        target.arenaTeam = team.id;
        a.clear();
        WowBuffer in = new WowBuffer(8);
        in.putU64(target.guid);
        a.handle(world, Opcodes.MSG_INSPECT_ARENA_TEAMS, in.array());
        WowBuffer out = new WowBuffer(lastPayload(a, Opcodes.MSG_INSPECT_ARENA_TEAMS));
        assertEquals(target.guid, out.getU64());
        assertEquals(0, out.getU8());
        assertEquals(42, out.getU32());
        assertEquals(1600, out.getU32());
        assertEquals(20, out.getU32());
        assertEquals(12, out.getU32());
        assertEquals(10, out.getU32());
        assertEquals(1500, out.getU32());
    }

    @Test
    void tpSl22TogglePvpWhenEmptyShouldSetDesiredAndUnitPvp() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Pvper");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.CMSG_TOGGLE_PVP, new byte[0]);
        int flags = p.getInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_FLAGS);
        assertEquals(Player.PLAYER_FLAGS_PVP_DESIRED, flags & Player.PLAYER_FLAGS_PVP_DESIRED);
        int unitFlags = p.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_FLAGS);
        assertEquals(org.tbc.world.entity.Unit.UNIT_FLAG_PVP, unitFlags & org.tbc.world.entity.Unit.UNIT_FLAG_PVP);
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_OBJECT) || client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
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
