package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Characterization of the logged-in dispatch table (refactoring plan cycle 1.1+). */
class OpcodeTableTest {
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

    @Test
    void loggedInWhenBuiltShouldRegisterExactlyTheMovedFamilies() {
        Set<Integer> expected = Set.of(
                Opcodes.CMSG_CREATURE_QUERY, Opcodes.CMSG_GAMEOBJECT_QUERY, Opcodes.CMSG_ITEM_QUERY_SINGLE,
                Opcodes.CMSG_QUEST_QUERY, Opcodes.CMSG_PAGE_TEXT_QUERY, Opcodes.CMSG_ITEM_TEXT_QUERY,
                Opcodes.CMSG_NPC_TEXT_QUERY, Opcodes.CMSG_PET_NAME_QUERY, Opcodes.CMSG_WHOIS);
        assertEquals(expected, OpcodeTable.loggedIn().opcodes());
    }

    @Test
    void dispatchWhenOpcodeUnknownShouldReturnFalseAndSendNothing() {
        RecordingSink sink = new RecordingSink();
        WorldSession session = new WorldSession(sink, 0);
        boolean handled = new OpcodeTable().dispatch(session, null, Opcodes.CMSG_WHOIS, new WowBuffer(new byte[0]));
        assertFalse(handled);
        assertEquals(List.of(), sink.sent);
    }

    @Test
    void dispatchWhenOpcodeRegisteredShouldInvokeOperationOnce() {
        List<Integer> calls = new ArrayList<>();
        OpcodeTable t = new OpcodeTable().register(Opcodes.CMSG_WHOIS, (s, w, in) -> calls.add(in.remaining()));
        boolean handled = t.dispatch(new WorldSession(new RecordingSink(), 0), null, Opcodes.CMSG_WHOIS,
                new WowBuffer(new byte[3]));
        assertTrue(handled);
        assertEquals(List.of(3), calls);
    }

    @Test
    void registerWhenOpcodeAlreadyRegisteredShouldThrow() {
        OpcodeTable t = new OpcodeTable().register(Opcodes.CMSG_WHOIS, (s, w, in) -> { });
        assertThrows(IllegalStateException.class, () -> t.register(Opcodes.CMSG_WHOIS, (s, w, in) -> { }));
    }
}
