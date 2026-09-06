package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.AbBattlefield;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL24-* deepen — AB/AV world-state and timer emits. */
class Slice24P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpSl24AbStablesContestedThenOccupied() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AbCap");
        Player p = client.session().player();
        world.teleport(p, 529, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.AB_STABLES);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());

        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_STABLES_CONT_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_CONT, world.ab.stablesStatus());

        client.clear();
        world.advanceMs(PvpObjectives.AB_CONTEST_MS);
        client.session().tick(world, 50);

        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_STABLES_OCC_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_OCC, world.ab.stablesStatus());
        assertEquals(1, world.ab.ownedAlliance());
    }

    @Test
    void tpSl24AvSnowfallFirstClaimTimer() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AvCap");
        Player p = client.session().player();
        world.teleport(p, 30, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());

        assertEquals(PvpObjectives.AV_SNOWFALL_MS, world.av.captureDurationMs());
        assertTrue(world.av.captureReadyAt() > world.nowMs());

        client.clear();
        world.advanceMs(PvpObjectives.AV_SNOWFALL_MS);
        client.session().tick(world, 50);

        assertTrue(hasWorldState(client, PvpObjectives.WS_AV_SCORE_A, 600));
    }

    @Test
    void tpSl24AvMineTickWorldState() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AvMine");
        Player p = client.session().player();
        world.teleport(p, 30, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.GO_AV_IRONDEEP);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertEquals(600, world.av.reinforcementsAlliance());

        client.clear();
        world.advanceMs(PvpObjectives.AV_MINE_TICK_MS);
        client.session().tick(world, 50);

        assertEquals(601, world.av.reinforcementsAlliance());
        assertTrue(hasWorldState(client, PvpObjectives.WS_AV_SCORE_A, 601));
    }

    @Test
    void tpSl24EyFlagAuraOnUse() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "EyFlag");
        Player p = client.session().player();
        world.teleport(p, 566, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.EY_FLAG_AURA));
        assertEquals(1, world.ey.towersAlliance());
        assertTrue(hasWorldState(client, PvpObjectives.WS_EY_TOWERS_A, 1));

        client.clear();
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertEquals(75, world.ey.resourcesAlliance());
        assertTrue(hasWorldState(client, PvpObjectives.WS_EY_RES_A, 75));
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.EY_FLAG_AURA));
    }

    @Test
    void tpSl24AbBlacksmithOccupiedWs() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AbSmith");
        Player p = client.session().player();
        world.teleport(p, 529, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.AB_BLACKSMITH);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());

        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_BLACKSMITH_CONT_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_CONT, world.ab.blacksmithStatus());

        client.clear();
        world.advanceMs(PvpObjectives.AB_CONTEST_MS);
        client.session().tick(world, 50);

        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_BLACKSMITH_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_OCC, world.ab.blacksmithStatus());
    }

    @Test
    void tpSl24AvReinforcementsDropOnDeath() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AvDie");
        Player p = client.session().player();
        world.teleport(p, 30, 0, 0, 0, 0);
        assertEquals(600, world.av.reinforcementsAlliance());
        client.clear();
        org.tbc.world.session.DeathHandler.killPlayer(client.session(), world);
        assertEquals(599, world.av.reinforcementsAlliance());
        assertTrue(hasWorldState(client, PvpObjectives.WS_AV_SCORE_A, 599));
    }

    @Test
    void tpSl24AbFarmContestedThenOccupied() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AbFarm");
        Player p = client.session().player();
        world.teleport(p, 529, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.AB_FARM);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_FARM_CONT_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_CONT, world.ab.farmStatus());
        client.clear();
        world.advanceMs(PvpObjectives.AB_CONTEST_MS);
        client.session().tick(world, 50);
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_FARM_OCC_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_OCC, world.ab.farmStatus());
    }

    @Test
    void tpSl24AbLumberMillContestedThenOccupied() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AbLm");
        Player p = client.session().player();
        world.teleport(p, 529, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.AB_LUMBER_MILL);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_LUMBER_CONT_A, 1));
        client.clear();
        world.advanceMs(PvpObjectives.AB_CONTEST_MS);
        client.session().tick(world, 50);
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_LUMBER_OCC_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_OCC, world.ab.nodeStatus(AbBattlefield.NODE_LUMBER));
    }

    @Test
    void tpSl24AbGoldMineContestedThenOccupied() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AbGold");
        Player p = client.session().player();
        world.teleport(p, 529, 0, 0, 0, 0);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.AB_GOLD_MINE);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_GOLD_CONT_A, 1));
        client.clear();
        world.advanceMs(PvpObjectives.AB_CONTEST_MS);
        client.session().tick(world, 50);
        assertTrue(hasWorldState(client, PvpObjectives.WS_AB_GOLD_OCC_A, 1));
        assertEquals(AbBattlefield.STATUS_ALLY_OCC, world.ab.nodeStatus(AbBattlefield.NODE_GOLD));
    }

    @Test
    void tpSl24AvVanndarKillShouldEndWithHordeWinnerLog() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AvBoss");
        Player p = client.session().player();
        world.teleport(p, 30, 0, 0, 0, 0);
        Creature vanndar = world.objectMgr.spawnCreature(11948, 30, p.x, p.y, p.z, p.o, world.scripts);
        vanndar.entry = 11948;
        vanndar.setHealth(1);
        world.map(p.mapId, p.instanceId).add(vanndar);
        client.clear();
        for (int i = 0; i < 50 && vanndar.alive(); i++) {
            world.meleeHit(p, vanndar);
        }
        assertTrue(!vanndar.alive());
        byte[] log = lastPayload(client, Opcodes.MSG_PVP_LOG_DATA);
        assertEquals(0, log[0] & 0xFF, "BG type");
        assertEquals(1, log[1] & 0xFF, "ended");
        assertEquals(0, log[2] & 0xFF, "WINNER_HORDE");
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static boolean hasWorldState(WowClientDouble client, int field, int value) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_UPDATE_WORLD_STATE) {
                continue;
            }
            byte[] payload = client.payloads.get(i);
            if (WowClientDouble.u32le(payload, 0) == field && WowClientDouble.u32le(payload, 4) == value) {
                return true;
            }
        }
        return false;
    }
}
