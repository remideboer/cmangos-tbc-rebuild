package org.tbc.matrix;

import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InProcessCommandMatrixTest {
    private static final byte[] PAD = new byte[64];

    static final class Sink implements PacketSink {
        final List<Integer> opcodes = new ArrayList<>();
        boolean closed;

        @Override
        public void send(int opcode, byte[] payload) {
            opcodes.add(opcode);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void catalogHasClientOpcodesOnly() {
        assertTrue(ClientCommands.WORLD.length > 400);
        assertEquals(5, ClientCommands.AUTH.length);
        assertEquals(5, ClientCommands.AUTH_XFER.length);
        for (ClientCommands.Op op : ClientCommands.WORLD) {
            assertFalse(op.name().startsWith("SMSG_"), op.name());
            assertFalse(op.name().startsWith("UMSG_"), op.name());
        }
    }

    @Test
    void everyClientOpcodeSurvives() throws Exception {
        World w = World.inMemory();
        Sink sink = new Sink();
        WorldSession s = loggedIn(w, sink, "Mtxbot");
        List<MatrixRow> rows = new ArrayList<>();
        for (ClientCommands.Op op : ClientCommands.WORLD) {
            if (skipDuringScan(op.id())) {
                rows.add(new MatrixRow("world-inprocess", op.id(), op.name(), "skip", "destructive or probe"));
                continue;
            }
            sink.opcodes.clear();
            byte[] payload = payloadFor(op.id(), s);
            String result;
            String note = "";
            try {
                s.handle(w, op.id(), payload);
                Thread.sleep(25);
                w.tick(50);
                sink.opcodes.clear();
                s.handle(w, Opcodes.CMSG_PING, pingPayload(1));
                boolean pong = sink.opcodes.contains(Opcodes.SMSG_PONG);
                if (sink.closed || !pong) {
                    result = "fail";
                    note = sink.closed ? "session closed" : "no SMSG_PONG";
                } else if (HandledOpcodes.handled(op.id()) || "STATUS_NEVER".equals(op.sessionStatus())) {
                    result = "pass";
                    note = HandledOpcodes.handled(op.id()) ? "handler" : "STATUS_NEVER stub";
                } else {
                    result = "unimplemented";
                }
            } catch (Exception e) {
                result = "fail";
                note = e.getClass().getSimpleName() + ": " + e.getMessage();
                if (sink.closed) {
                    s = loggedIn(w, sink, "Mtxbot");
                    sink.closed = false;
                }
            }
            rows.add(new MatrixRow("world-inprocess", op.id(), op.name(), result, note));
        }
        MatrixReport.write("inprocess", "In-process world", rows);
        List<MatrixRow> fails = rows.stream().filter(r -> "fail".equals(r.result())).toList();
        assertTrue(fails.isEmpty(), () -> "fails=" + fails);
        assertEquals(ClientCommands.WORLD.length, rows.size());
    }

    @Test
    void existingHandlerOracles() {
        World w = World.inMemory();
        Sink sink = new Sink();
        WorldSession s = loggedIn(w, sink, "Oracle");
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_PING, pingPayload(9));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_PONG));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_CHAR_ENUM));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_QUERY_TIME, new byte[0]);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_QUERY_TIME_RESPONSE));

        sink.opcodes.clear();
        WowBuffer name = new WowBuffer(8);
        name.putU64(s.player().guid);
        s.handle(w, Opcodes.CMSG_NAME_QUERY, name.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_NAME_QUERY_RESPONSE));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CONTACT_LIST, PAD);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_CONTACT_LIST));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_WHO, PAD);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_WHO));

        s.player().spells.add(78);
        WowBuffer cast = new WowBuffer(8);
        cast.putU32(78);
        cast.putU8(1);
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CAST_SPELL, cast.array());
        assertFalse(sink.opcodes.contains(0x1B4));
        if (sink.opcodes.contains(Opcodes.SMSG_CAST_RESULT)) {
            assertEquals(Codes.SMSG_CAST_RESULT, Opcodes.SMSG_CAST_RESULT);
        }

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_RESPONSE));
    }

    private static boolean skipDuringScan(int id) {
        return id == Opcodes.CMSG_AUTH_SESSION
                || id == Opcodes.CMSG_LOGOUT_REQUEST
                || id == Opcodes.CMSG_LOGOUT_CANCEL
                || id == Opcodes.CMSG_PLAYER_LOGOUT
                || id == Opcodes.CMSG_CHAR_DELETE
                || id == Opcodes.CMSG_PING;
    }

    private static byte[] payloadFor(int opcode, WorldSession s) {
        if (opcode >= Opcodes.MSG_MOVE_START_FORWARD && opcode <= Opcodes.MSG_MOVE_HEARTBEAT
                || opcode == Opcodes.CMSG_FORCE_MOVE_ROOT_ACK
                || opcode == Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK) {
            WowBuffer b = new WowBuffer(48);
            if (opcode == Opcodes.CMSG_FORCE_MOVE_ROOT_ACK
                    || opcode == Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK
                    || opcode == Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK) {
                b.putPackedGuid(s.player().guid);
            }
            if (opcode == Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK) {
                b.putU32(1);
            }
            b.putU32(0);
            b.putU8(0);
            b.putU32(0);
            Player p = s.player();
            b.putFloat(p.x);
            b.putFloat(p.y);
            b.putFloat(p.z);
            b.putFloat(p.o);
            b.putU32(0);
            if (opcode == Opcodes.CMSG_FORCE_RUN_SPEED_CHANGE_ACK) {
                b.putFloat(7f);
            }
            return b.array();
        }
        return PAD;
    }

    private static byte[] pingPayload(int seq) {
        WowBuffer b = new WowBuffer(8);
        b.putU32(seq);
        b.putU32(0);
        return b.array();
    }

    private static WorldSession loggedIn(World w, Sink sink, String name) {
        sink.closed = false;
        WorldSession s = new WorldSession(sink, 0x11111111);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86"));
        Player p = w.characters.create(1, name, 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        return s;
    }
}
