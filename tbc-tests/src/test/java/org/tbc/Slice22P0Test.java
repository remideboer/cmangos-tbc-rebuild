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
