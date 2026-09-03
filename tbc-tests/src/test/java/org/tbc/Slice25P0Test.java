package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL25-001 deepen — Silithyst deliver emits WS 2313 + buff 30754. */
class Slice25P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpSl25SilithystDeliverWorldState() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Sili", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.zoneId = 1377;
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_SILITHYST_A, 200));
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SILITHYST_WIN));
    }

    @Test
    void tpSl25NorthpassTowerWorldState() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "EpTower", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.GO_EP_NORTHPASS);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_EP_NORTHPASS_A, 1));
        assertTrue(hasWorldState(client, PvpObjectives.WS_EP_NORTHPASS_N, 0));
    }

    @Test
    void tpSl25ZmEastBeaconWorldState() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ZmEast", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(PvpObjectives.GO_ZM_EAST);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        assertTrue(hasWorldState(client, PvpObjectives.WS_ZM_EAST_A, 1));
        assertTrue(hasWorldState(client, PvpObjectives.WS_ZM_EAST_N, 0));
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
