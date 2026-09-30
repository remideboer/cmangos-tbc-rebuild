package org.tbc.world.world;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRespawnTest {
    @Test
    void tickWhenCorpseRespawnDueShouldRestoreHealth() {
        World world = World.inMemory();
        Creature c = world.objectMgr.spawnCreature(6, 0, 0, 0, 0, 0, world.scripts);
        world.map(0, 0).add(c);
        Player watcher = new Player();
        watcher.guid = 1;
        watcher.relocate(0, 0, 0, 0);
        world.map(0, 0).add(watcher);
        c.setHealth(0);
        c.respawnDelayMs = 1;
        c.respawnAtMs = world.nowMs();
        world.tick(50);
        assertTrue(c.alive());
        assertEquals(c.maxHealth(), c.health());
    }

    /**
     * TP-SL06-026 — CORPSE update IsCorpseExpired → RemoveCorpse before respawn (SMSG_DESTROY_OBJECT).
     */
    @Test
    void tickWhenCorpseExpiredShouldDestroyObjectBeforeRespawn() {
        World world = World.inMemory();
        Creature c = world.objectMgr.spawnCreature(6, 0, 0, 0, 0, 0, world.scripts);
        world.map(0, 0).add(c);
        List<Integer> ops = new ArrayList<>();
        PacketSink sink = new PacketSink() {
            @Override
            public void send(int opcode, byte[] payload) {
                ops.add(opcode);
            }

            @Override
            public void close() {
            }
        };
        WorldSession session = new WorldSession(sink, 1);
        Player watcher = new Player();
        watcher.guid = 1;
        watcher.relocate(0, 0, 0, 0);
        watcher.session = session;
        world.map(0, 0).add(watcher);
        long now = world.nowMs();
        c.setHealth(0);
        c.corpseExpireAtMs = now;
        c.respawnAtMs = now + 60_000;
        world.tick(50);
        assertTrue(c.corpseRemoved);
        assertFalse(c.alive());
        assertTrue(ops.contains(Opcodes.SMSG_DESTROY_OBJECT));
    }
}
