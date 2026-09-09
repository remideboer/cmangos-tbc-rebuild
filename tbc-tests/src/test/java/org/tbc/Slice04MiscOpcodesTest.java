package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL04-016 — MiscHandler leftover C2S (one method per opcode).
 */
class Slice04MiscOpcodesTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    /**
     * CMSG_PLAYED_TIME → SMSG_PLAYED_TIME total+level u32 (HandlePlayedTime).
     * A new character has both clocks at 0.
     */
    @Test
    void tpSl04PlayedTime() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Played");
        client.clear();
        client.handle(world, Opcodes.CMSG_PLAYED_TIME, new byte[0]);
        assertTrue(client.saw(Opcodes.SMSG_PLAYED_TIME));
        WowBuffer b = new WowBuffer(client.payload(Opcodes.SMSG_PLAYED_TIME));
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.remaining());
    }

    private static WowClientDouble enter(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
