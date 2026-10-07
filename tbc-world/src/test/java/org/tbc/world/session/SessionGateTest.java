package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Status-gated pre-switch routing (refactoring plan cycle 1.4). Mirrors CMaNGOS
 * WorldSession::Update: STATUS_NEVER accepts only the handshake, STATUS_AUTHED the
 * character screen, recently-logged-out only CMSG_UPDATE_ACCOUNT_DATA; movement is
 * consumed before the logged-in switch, everything else is left to the caller.
 */
class SessionGateTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private static final class RecordingSink implements PacketSink {
        final List<Integer> sent = new ArrayList<>();

        @Override
        public void send(int opcode, byte[] payload) {
            sent.add(opcode);
        }

        @Override
        public void close() {
        }
    }

    private static WowBuffer empty() {
        return new WowBuffer(new byte[0]);
    }

    @Test
    void dispatchWhenStatusNeverShouldAnswerPing() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        WowBuffer ping = new WowBuffer(8);
        ping.putU32(7);
        ping.putU32(50);
        assertTrue(new SessionGate().dispatch(s, World.inMemory(), Opcodes.CMSG_PING, ping));
        assertEquals(List.of(Opcodes.SMSG_PONG), sink.sent);
    }

    @Test
    void dispatchWhenStatusNeverShouldDropCharacterScreenOpcodes() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        assertTrue(new SessionGate().dispatch(s, World.inMemory(), Opcodes.CMSG_CHAR_ENUM, empty()));
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenStatusAuthedShouldHandleCharEnum() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        assertTrue(new SessionGate().dispatch(s, World.inMemory(), Opcodes.CMSG_CHAR_ENUM, empty()));
        assertEquals(List.of(Opcodes.SMSG_CHAR_ENUM), sink.sent);
    }

    @Test
    void dispatchWhenStatusAuthedShouldDropLoggedInOpcodes() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        assertTrue(new SessionGate().dispatch(s, World.inMemory(), Opcodes.CMSG_LOGOUT_REQUEST, empty()));
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenStatusAuthedShouldConsumeUpdateAccountData() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        assertTrue(new SessionGate().dispatch(s, World.inMemory(), Opcodes.CMSG_UPDATE_ACCOUNT_DATA, empty()));
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenLoggedInShouldConsumeMovementAcks() {
        RecordingSink sink = new RecordingSink();
        World world = World.inMemory();
        WorldSession s = login(world, sink, "Gate");
        assertTrue(new SessionGate().dispatch(s, world, Opcodes.MSG_MOVE_TELEPORT_ACK, empty()));
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenLoggedInShouldLeaveSwitchOpcodesToCaller() {
        RecordingSink sink = new RecordingSink();
        World world = World.inMemory();
        WorldSession s = login(world, sink, "Gatetwo");
        assertFalse(new SessionGate().dispatch(s, world, Opcodes.CMSG_LOGOUT_REQUEST, empty()));
        assertEquals(List.of(), sink.sent);
    }

    private static WorldSession login(World world, RecordingSink sink, String name) {
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(created.guid);
        s.handle(world, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        sink.sent.clear();
        return s;
    }
}
