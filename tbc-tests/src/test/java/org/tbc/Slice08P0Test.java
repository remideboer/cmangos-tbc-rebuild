package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL08-004: MovementType 1 creatures wander OOC so the 8606 client sees them walk. */
class Slice08P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl08WanderWhenMovementTypeRandomShouldSendMonsterMove() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wander", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature c = world.objectMgr.spawnCreature(6, 0, -5000f, -5000f, 80f, 0f, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        c.movementType = 1;
        c.spawnDist = 10f;
        int[] n = {0};
        c.motion.rng(() -> n[0]++ == 0 ? 0.0 : 1.0);
        c.startOocMotion();
        p.relocate(c.x + 40, c.y, c.z, c.o);
        client.clear();
        world.tick(1000);
        assertTrue(client.saw(Opcodes.SMSG_MONSTER_MOVE));
        WowBuffer move = new WowBuffer(client.payload(Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(c.guid, move.getPackedGuid());
        move.getFloat();
        move.getFloat();
        move.getFloat();
        move.getU32();
        move.getU8();
        move.getU32();
        int duration = move.getU32();
        assertEquals(1, move.getU32());
        float destX = move.getFloat();
        float destY = move.getFloat();
        move.getFloat();
        assertTrue(c.spawnDistance2d(destX, destY) <= 10f + 0.01f);
        float path = (float) Math.hypot(destX - c.spawnX, destY - c.spawnY);
        assertEquals(Math.max(1, (int) (path / UpdateBuilder.WALK * 1000f)), duration);
    }
}
