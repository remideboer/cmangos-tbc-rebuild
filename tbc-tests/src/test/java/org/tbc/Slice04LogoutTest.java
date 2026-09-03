package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL04-007 deepen — logout.md wire contracts.
 * CMSG_PLAYER_LOGOUT remains a documented no-op.
 */
class Slice04LogoutTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final World.Account ACC2 =
            new World.Account(2, "PLAYER2", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpSl04PlayerLogoutIsNoOp() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Noop");
        client.clear();
        client.playerLogout(world);
        assertFalse(client.saw(Opcodes.SMSG_LOGOUT_RESPONSE));
        assertFalse(client.saw(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_LOGGEDIN, client.session().status());
        assertNotNull(client.session().player());
    }

    @Test
    void tpSl04LogoutBlockedWhileDueling() {
        World world = World.inMemory();
        WowClientDouble a = enter(world, ACC, "DuelA");
        WowClientDouble b = enter(world, ACC2, "DuelB");
        a.session().player().engageDuel(b.session().player());
        a.clear();
        a.logout(world);
        WowBuffer r = new WowBuffer(a.payload(Opcodes.SMSG_LOGOUT_RESPONSE));
        assertEquals(1, r.getU32());
        assertEquals(0, r.getU8());
        assertFalse(a.saw(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_LOGGEDIN, a.session().status());
    }

    @Test
    void tpSl04LogoutBlockedWhileFallingFar() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Faller");
        Player p = client.session().player();
        p.movement.moveFlags |= MovementInfo.MOVEFLAG_FALLINGFAR;
        client.clear();
        client.logout(world);
        WowBuffer r = new WowBuffer(client.payload(Opcodes.SMSG_LOGOUT_RESPONSE));
        assertEquals(1, r.getU32());
        assertEquals(0, r.getU8());
        assertFalse(client.saw(Opcodes.SMSG_LOGOUT_COMPLETE));
    }

    @Test
    void tpSl04LogoutInstantWhileTaxiFlying() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Flyer");
        client.session().player().startTaxiFlight(6);
        client.clear();
        client.logout(world);
        WowBuffer r = new WowBuffer(client.payload(Opcodes.SMSG_LOGOUT_RESPONSE));
        assertEquals(0, r.getU32());
        assertEquals(1, r.getU8());
        assertTrue(client.saw(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_AUTHED, client.session().status());
    }

    private static WowClientDouble enter(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
