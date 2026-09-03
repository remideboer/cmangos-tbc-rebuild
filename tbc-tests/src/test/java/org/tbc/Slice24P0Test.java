package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
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
