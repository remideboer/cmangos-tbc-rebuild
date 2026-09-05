package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL06-003: creature SMSG_ATTACKSTART so the 8606 client plays the fight. */
class Slice06P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl06CreatureAttackStartWhenPlayerSwings() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Fighter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.attackSwing(world, c.guid);
        boolean sawCreatureStart = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            assertTrue(payload.length >= 16);
            long attacker = WowClientDouble.u64le(payload, 0);
            long victim = WowClientDouble.u64le(payload, 8);
            if (attacker == c.guid && victim == p.guid) {
                sawCreatureStart = true;
            }
        }
        assertTrue(sawCreatureStart);
    }

    @Test
    void tpSl06HostileWhenPlayerEntersDetectionShouldAttackStart() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Pull", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x + 10, c.y, c.z, c.o);
        client.clear();
        world.tick(50);
        boolean sawCreatureStart = false;
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_ATTACKSTART) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            long attacker = WowClientDouble.u64le(payload, 0);
            long victim = WowClientDouble.u64le(payload, 8);
            if (attacker == c.guid && victim == p.guid) {
                sawCreatureStart = true;
            }
        }
        assertTrue(sawCreatureStart);
        assertTrue(c.inCombat);
    }
}
