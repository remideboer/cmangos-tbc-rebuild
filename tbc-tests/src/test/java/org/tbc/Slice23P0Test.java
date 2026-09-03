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
        client.clear();
        client.handle(world, Opcodes.MSG_PVP_LOG_DATA, new byte[0]);
        byte[] log = lastPayload(client, Opcodes.MSG_PVP_LOG_DATA);
        assertEquals(0, log[0] & 0xFF);
        assertEquals(1, log[1] & 0xFF);
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
