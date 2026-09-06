package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice 10 P0 wire: dungeon difficulty C2S (instance.md). */
class Slice10P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final World.Account ACC_GM =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl10SetDungeonDifficultyWhenHeroicShouldEchoMode() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_GM);
        Player created = world.characters.create(ACC.id(), "Diff", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.level = 70;
        assertEquals(0, p.difficulty);
        client.clear();
        client.setDungeonDifficulty(world, 1);
        assertEquals(1, p.difficulty);
        byte[] payload = client.payload(Opcodes.MSG_SET_DUNGEON_DIFFICULTY);
        assertEquals(12, payload.length);
        assertEquals(1, WowClientDouble.u32le(payload, 0));
        assertEquals(1, WowClientDouble.u32le(payload, 4));
        assertEquals(0, WowClientDouble.u32le(payload, 8));
    }

    @Test
    void tpSl10HourlyCapWhenSixthNewInstanceShouldAbort() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Cap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        for (int i = 0; i < 5; i++) {
            client.clear();
            client.areaTrigger(world, 2230);
            client.worldportAck(world);
            assertEquals(389, p.mapId);
            world.teleport(p, 0, -8949f, -132f, 83f, 0f);
            p.instanceId = 0;
        }
        assertEquals(5, p.enteredInstances.size());
        client.clear();
        client.areaTrigger(world, 2230);
        assertTrue(client.saw(Opcodes.SMSG_TRANSFER_ABORTED));
        byte[] abort = client.payload(Opcodes.SMSG_TRANSFER_ABORTED);
        assertEquals(5, abort.length);
        assertEquals(389, WowClientDouble.u32le(abort, 0));
        assertEquals(0x03, abort[4] & 0xFF);
        assertEquals(0, p.mapId);
        assertTrue(client.opcodes.stream().noneMatch(op -> op == Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void tpSl10LeaveWsgWhenNotInCombatShouldTeleportToEntry() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_GM);
        Player created = world.characters.create(ACC.id(), "Leave", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int entryMap = p.mapId;
        float entryX = p.x;
        float entryY = p.y;
        float entryZ = p.z;
        client.battlemasterJoin(world);
        client.clear();
        client.battlefieldPort(world, 1);
        client.worldportAck(world);
        assertEquals(489, p.mapId);
        client.clear();
        client.leaveBattlefield(world, 2);
        assertEquals(entryMap, p.mapId);
        assertEquals(entryX, p.x, 0.01f);
        assertEquals(entryY, p.y, 0.01f);
        assertEquals(entryZ, p.z, 0.01f);
        assertTrue(client.saw(Opcodes.SMSG_NEW_WORLD));
        assertEquals(entryMap, WowClientDouble.u32le(client.payload(Opcodes.SMSG_NEW_WORLD), 0));
    }

    @Test
    void leaveWsgWhenInCombatShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_GM);
        Player created = world.characters.create(ACC.id(), "Fight", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        client.battlemasterJoin(world);
        client.battlefieldPort(world, 1);
        client.worldportAck(world);
        assertEquals(489, p.mapId);
        p.inCombat = true;
        client.clear();
        client.leaveBattlefield(world, 2);
        assertEquals(489, p.mapId);
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void setDungeonDifficultyWhenSameModeShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_GM);
        Player created = world.characters.create(ACC.id(), "Same", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.level = 70;
        client.clear();
        client.setDungeonDifficulty(world, 0);
        assertEquals(0, p.difficulty);
        assertTrue(client.opcodes.stream().noneMatch(op -> op == Opcodes.MSG_SET_DUNGEON_DIFFICULTY));
    }

    @Test
    void setDungeonDifficultyWhenBelowHeroicLevelShouldIgnoreHeroic() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC_GM);
        Player created = world.characters.create(ACC.id(), "Low", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        assertTrue(p.level < 70);
        client.clear();
        client.setDungeonDifficulty(world, 1);
        assertEquals(0, p.difficulty);
        assertTrue(client.opcodes.stream().noneMatch(op -> op == Opcodes.MSG_SET_DUNGEON_DIFFICULTY));
    }
}
