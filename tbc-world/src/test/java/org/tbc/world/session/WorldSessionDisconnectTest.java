package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorldSessionDisconnectTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void onSocketClosedWhenInCombatShouldSaveOnlyAfterSixtySeconds() {
        World world = World.inMemory();
        List<Integer> ops = new ArrayList<>();
        WorldSession s = new WorldSession(new PacketSink() {
            @Override
            public void send(int opcode, byte[] payload) {
                ops.add(opcode);
            }

            @Override
            public void close() {
            }
        }, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), "Drop", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        Player p = s.player();
        p.money = 12345;
        p.inCombat = true;
        s.markSocketClosed();
        s.tick(world, 0);
        world.advanceMs(20_000);
        s.tick(world, 20_000);
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
        world.advanceMs(40_000);
        s.tick(world, 40_000);
        assertEquals(WorldSession.STATUS_AUTHED, s.status());
        Player loaded = world.characters.load(ACC.id(), created.guid, world.objectMgr);
        assertNotNull(loaded);
        assertEquals(12345, loaded.money);
    }
}
