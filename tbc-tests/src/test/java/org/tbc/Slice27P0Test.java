package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL27-* from movement.md */
class Slice27P0Test {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");
    private static final long TRANSPORT = 0x1FC0000000000001L;

    @Test
    void tpSl27OnTransportEcho() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Mounter");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        b.clear();
        WowBuffer hb = new WowBuffer(64);
        hb.putU32(MovementInfo.MOVEFLAG_ONTRANSPORT);
        hb.putU8(0);
        hb.putU32(1);
        hb.putFloat(a.session().player().x);
        hb.putFloat(a.session().player().y);
        hb.putFloat(a.session().player().z);
        hb.putFloat(0);
        hb.putPackedGuid(TRANSPORT);
        hb.putFloat(0.1f);
        hb.putFloat(0.2f);
        hb.putFloat(0.3f);
        hb.putFloat(0.4f);
        hb.putU32(9);
        hb.putU32(0);
        a.handle(world, Opcodes.MSG_MOVE_HEARTBEAT, hb.array());
        byte[] echo = lastPayload(b, Opcodes.MSG_MOVE_HEARTBEAT);
        WowBuffer e = new WowBuffer(echo);
        e.getPackedGuid();
        assertEquals(MovementInfo.MOVEFLAG_ONTRANSPORT, e.getU32());
        e.getU8();
        e.getU32();
        e.getFloat();
        e.getFloat();
        e.getFloat();
        e.getFloat();
        assertEquals(TRANSPORT, e.getPackedGuid());
    }

    @Test
    void tpSl27BoardMoTransportRuntime() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Mounter");
        Player p = client.session().player();
        org.tbc.world.entity.GameObject boat = new org.tbc.world.entity.GameObject();
        boat.guid = TRANSPORT;
        boat.type = org.tbc.world.spell.GameObjectUse.TYPE_MO_TRANSPORT;
        boat.pathProgress = 1234;
        boat.periodMs = 60_000;
        world.map(p.mapId, p.instanceId).gameObjects.put(boat.guid, boat);
        client.clear();
        WowBuffer use = new WowBuffer(8);
        use.putU64(TRANSPORT);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, use.array());
        assertEquals(MovementInfo.MOVEFLAG_ONTRANSPORT, p.movement.moveFlags & MovementInfo.MOVEFLAG_ONTRANSPORT);
        assertEquals(TRANSPORT, p.movement.transportGuid);
        assertEquals(1234, p.movement.tTime);
        p.leaveMoTransport();
        assertEquals(0, p.movement.moveFlags & MovementInfo.MOVEFLAG_ONTRANSPORT);
        assertEquals(0, p.movement.transportGuid);
    }

    @Test
    void tpSl27ForceRunSpeedAck() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Mounter");
        Player p = client.session().player();
        WowBuffer ack = new WowBuffer(64);
        ack.putPackedGuid(p.guid);
        ack.putU32(1);
        ack.putU32(0);
        ack.putU8(0);
        ack.putU32(0);
        ack.putFloat(p.x);
        ack.putFloat(p.y);
        ack.putFloat(p.z);
        ack.putFloat(p.o);
        ack.putU32(0);
        ack.putFloat(7.0f);
        client.handle(world, Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK, ack.array());
        assertEquals(7.0f, p.lastAckSpeed);
    }

    @Test
    void tpSl27CancelMountAura() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Mounter");
        Player p = client.session().player();
        p.auras.add(new Unit.Aura(PvpObjectives.MOUNT_AURA, 0, 1));
        p.mounted = true;
        client.handle(world, Opcodes.CMSG_CANCEL_MOUNT_AURA, new byte[0]);
        assertFalse(p.mounted);
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.MOUNT_AURA));
    }

    private static WowClientDouble login(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
