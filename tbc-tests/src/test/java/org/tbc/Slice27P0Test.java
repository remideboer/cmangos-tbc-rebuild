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
    void tpSl27ForceSwimSpeedAck() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Swimmer");
        Player p = client.session().player();
        WowBuffer ack = new WowBuffer(64);
        ack.putPackedGuid(p.guid);
        ack.putU32(2);
        ack.putU32(0);
        ack.putU8(0);
        ack.putU32(0);
        ack.putFloat(p.x);
        ack.putFloat(p.y);
        ack.putFloat(p.z);
        ack.putFloat(p.o);
        ack.putU32(0);
        ack.putFloat(4.722946f);
        client.handle(world, Opcodes.CMSG_FORCE_SWIM_SPEED_CHANGE_ACK, ack.array());
        assertEquals(4.722946f, p.lastAckSpeed, 0.0001f);
    }

    @Test
    void tpSl27SummonResponseTeleport() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Summoned");
        Player p = client.session().player();
        long summoner = 42L;
        p.offerSummon(summoner, 1, -7200f, -200f, 10f, world.nowMs() + 60_000);
        client.clear();
        WowBuffer resp = new WowBuffer(9);
        resp.putU64(summoner);
        resp.putU8(1);
        client.handle(world, Opcodes.CMSG_SUMMON_RESPONSE, resp.array());
        assertTrue(client.saw(Opcodes.SMSG_NEW_WORLD));
        assertEquals(1, WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_NEW_WORLD), 0));
        assertEquals(1, p.mapId);
        assertEquals(-7200f, p.x, 0.01f);
    }

    @Test
    void tpSl27MoveSplineDoneCounter() {
        World world = World.inMemory();
        WowClientDouble client = login(world, ACC_A, "Flyer");
        Player p = client.session().player();
        WowBuffer done = new WowBuffer(64);
        done.putU32(0);
        done.putU8(0);
        done.putU32(0);
        done.putFloat(p.x);
        done.putFloat(p.y);
        done.putFloat(p.z);
        done.putFloat(p.o);
        done.putU32(0);
        done.putU32(17);
        client.handle(world, Opcodes.CMSG_MOVE_SPLINE_DONE, done.array());
        assertEquals(17, p.lastSplineDoneCounter);
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

    /**
     * TP-SL27-007 — HandleMoveTimeSkippedOpcode. Nearby players get MSG_MOVE_TIME_SKIPPED
     * packed guid + skipped ms. The sender does not.
     */
    @Test
    void tpSl27MoveTimeSkippedReachesNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Skipper");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player skipper = a.session().player();
        a.clear();
        b.clear();
        WowBuffer in = new WowBuffer(12);
        in.putU64(skipper.guid);
        in.putU32(500);
        a.handle(world, Opcodes.CMSG_MOVE_TIME_SKIPPED, in.array());
        assertFalse(a.saw(Opcodes.MSG_MOVE_TIME_SKIPPED));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.MSG_MOVE_TIME_SKIPPED));
        assertEquals(skipper.guid, out.getPackedGuid());
        assertEquals(500, out.getU32());
    }

    /**
     * TP-SL27-007 — HandleMovementOpcodes returns before the observer broadcast for
     * CMSG_MOVE_FALL_RESET. Position follows MovementInfo. Nearby clients get no echo.
     */
    @Test
    void tpSl27FallResetMovesWithoutEcho() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Faller");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        WowBuffer in = new WowBuffer(32);
        in.putU32(0);
        in.putU8(0);
        in.putU32(0);
        in.putFloat(100f);
        in.putFloat(200f);
        in.putFloat(30f);
        in.putFloat(0f);
        in.putU32(0);
        a.handle(world, Opcodes.CMSG_MOVE_FALL_RESET, in.array());
        assertEquals(100f, p.x, 0.01f);
        assertEquals(200f, p.y, 0.01f);
        assertEquals(30f, p.z, 0.01f);
        assertFalse(a.saw(Opcodes.CMSG_MOVE_FALL_RESET));
        assertFalse(b.saw(Opcodes.CMSG_MOVE_FALL_RESET));
    }

    /**
     * TP-SL27-007 — HandleMovementOpcodes broadcasts CMSG_MOVE_SET_FLY
     * (packed guid + MovementInfo) to nearby players. The sender is excluded.
     */
    @Test
    void tpSl27SetFlyEchoesToNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Flyer");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        float x = p.x + 1f;
        float y = p.y;
        float z = p.z + 1f;
        WowBuffer in = new WowBuffer(32);
        in.putU32(0);
        in.putU8(0);
        in.putU32(0);
        in.putFloat(x);
        in.putFloat(y);
        in.putFloat(z);
        in.putFloat(0f);
        in.putU32(0);
        a.handle(world, Opcodes.CMSG_MOVE_SET_FLY, in.array());
        assertFalse(a.saw(Opcodes.CMSG_MOVE_SET_FLY));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.CMSG_MOVE_SET_FLY));
        assertEquals(p.guid, out.getPackedGuid());
        assertEquals(0, out.getU32());
        assertEquals(0, out.getU8());
        out.getU32();
        assertEquals(x, out.getFloat(), 0.01f);
        assertEquals(y, out.getFloat(), 0.01f);
        assertEquals(z, out.getFloat(), 0.01f);
        assertEquals(x, p.x, 0.01f);
        assertEquals(z, p.z, 0.01f);
    }

    /**
     * TP-SL27-007 — HandleMoveKnockBackAck. Nearby get MSG_MOVE_KNOCK_BACK:
     * packed guid + MovementInfo + jump cos/sin/xy/zspeed. Sender excluded.
     */
    @Test
    void tpSl27KnockBackAckEchoesToNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Knocked");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        float x = p.x + 5f;
        float y = p.y + 3f;
        float z = p.z + 2f;
        float jumpZ = -10f;
        float jumpCos = 0.6f;
        float jumpSin = 0.8f;
        float jumpXy = 12f;
        WowBuffer in = new WowBuffer(64);
        in.putPackedGuid(p.guid);
        in.putU32(1);
        in.putU32(MovementInfo.MOVEFLAG_FALLING);
        in.putU8(0);
        in.putU32(100);
        in.putFloat(x);
        in.putFloat(y);
        in.putFloat(z);
        in.putFloat(1.2f);
        in.putU32(0);
        in.putFloat(jumpZ);
        in.putFloat(jumpCos);
        in.putFloat(jumpSin);
        in.putFloat(jumpXy);
        a.handle(world, Opcodes.CMSG_MOVE_KNOCK_BACK_ACK, in.array());
        assertFalse(a.saw(Opcodes.MSG_MOVE_KNOCK_BACK));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.MSG_MOVE_KNOCK_BACK));
        assertEquals(p.guid, out.getPackedGuid());
        assertEquals(MovementInfo.MOVEFLAG_FALLING, out.getU32());
        assertEquals(0, out.getU8());
        out.getU32();
        assertEquals(x, out.getFloat(), 0.01f);
        assertEquals(y, out.getFloat(), 0.01f);
        assertEquals(z, out.getFloat(), 0.01f);
        assertEquals(1.2f, out.getFloat(), 0.01f);
        assertEquals(0, out.getU32());
        assertEquals(jumpZ, out.getFloat(), 0.01f);
        assertEquals(jumpCos, out.getFloat(), 0.01f);
        assertEquals(jumpSin, out.getFloat(), 0.01f);
        assertEquals(jumpXy, out.getFloat(), 0.01f);
        assertEquals(jumpCos, out.getFloat(), 0.01f);
        assertEquals(jumpSin, out.getFloat(), 0.01f);
        assertEquals(jumpXy, out.getFloat(), 0.01f);
        assertEquals(jumpZ, out.getFloat(), 0.01f);
        assertEquals(x, p.x, 0.01f);
        assertEquals(y, p.y, 0.01f);
        assertEquals(z, p.z, 0.01f);
    }

    /**
     * TP-SL27-007 — HandleMoveFlagChangeOpcode CMSG_MOVE_HOVER_ACK.
     * Nearby get MSG_MOVE_HOVER (packed guid + MovementInfo). Sender excluded.
     */
    @Test
    void tpSl27HoverAckEchoesToNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Hoverer");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        float x = p.x + 2f;
        float y = p.y + 1f;
        float z = p.z;
        WowBuffer in = new WowBuffer(48);
        in.putPackedGuid(p.guid);
        in.putU32(2);
        in.putU32(0);
        in.putU8(0);
        in.putU32(50);
        in.putFloat(x);
        in.putFloat(y);
        in.putFloat(z);
        in.putFloat(0f);
        in.putU32(0);
        in.putU32(1);
        a.handle(world, Opcodes.CMSG_MOVE_HOVER_ACK, in.array());
        assertFalse(a.saw(Opcodes.MSG_MOVE_HOVER));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.MSG_MOVE_HOVER));
        assertEquals(p.guid, out.getPackedGuid());
        assertEquals(0, out.getU32());
        assertEquals(0, out.getU8());
        out.getU32();
        assertEquals(x, out.getFloat(), 0.01f);
        assertEquals(y, out.getFloat(), 0.01f);
        assertEquals(z, out.getFloat(), 0.01f);
        assertEquals(x, p.x, 0.01f);
    }

    /**
     * TP-SL27-007 — HandleMoveFlagChangeOpcode CMSG_MOVE_WATER_WALK_ACK → MSG_MOVE_WATER_WALK.
     */
    @Test
    void tpSl27WaterWalkAckEchoesToNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Walker");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        float x = p.x + 3f;
        WowBuffer in = new WowBuffer(48);
        in.putPackedGuid(p.guid);
        in.putU32(3);
        in.putU32(0);
        in.putU8(0);
        in.putU32(60);
        in.putFloat(x);
        in.putFloat(p.y);
        in.putFloat(p.z);
        in.putFloat(0f);
        in.putU32(0);
        in.putU32(1);
        a.handle(world, Opcodes.CMSG_MOVE_WATER_WALK_ACK, in.array());
        assertFalse(a.saw(Opcodes.MSG_MOVE_WATER_WALK));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.MSG_MOVE_WATER_WALK));
        assertEquals(p.guid, out.getPackedGuid());
        out.getU32();
        out.getU8();
        out.getU32();
        assertEquals(x, out.getFloat(), 0.01f);
        assertEquals(x, p.x, 0.01f);
    }

    /**
     * TP-SL27-007 — HandleMoveFlagChangeOpcode CMSG_MOVE_FEATHER_FALL_ACK → MSG_MOVE_FEATHER_FALL.
     */
    @Test
    void tpSl27FeatherFallAckEchoesToNearby() {
        World world = World.inMemory();
        WowClientDouble a = login(world, ACC_A, "Feather");
        WowClientDouble b = login(world, ACC_B, "Watcher");
        Player p = a.session().player();
        a.clear();
        b.clear();
        float z = p.z + 4f;
        WowBuffer in = new WowBuffer(48);
        in.putPackedGuid(p.guid);
        in.putU32(4);
        in.putU32(0);
        in.putU8(0);
        in.putU32(70);
        in.putFloat(p.x);
        in.putFloat(p.y);
        in.putFloat(z);
        in.putFloat(0f);
        in.putU32(0);
        in.putU32(1);
        a.handle(world, Opcodes.CMSG_MOVE_FEATHER_FALL_ACK, in.array());
        assertFalse(a.saw(Opcodes.MSG_MOVE_FEATHER_FALL));
        WowBuffer out = new WowBuffer(lastPayload(b, Opcodes.MSG_MOVE_FEATHER_FALL));
        assertEquals(p.guid, out.getPackedGuid());
        out.getU32();
        out.getU8();
        out.getU32();
        out.getFloat();
        out.getFloat();
        assertEquals(z, out.getFloat(), 0.01f);
        assertEquals(z, p.z, 0.01f);
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
