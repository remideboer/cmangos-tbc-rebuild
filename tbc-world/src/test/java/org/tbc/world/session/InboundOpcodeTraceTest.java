package org.tbc.world.session;

import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lab inbound C2S opcode filter — gossip/query stay, movement/ping stay quiet. */
class InboundOpcodeTraceTest {

    @Test
    void shouldLogWhenGossipHelloShouldBeTrue() {
        assertTrue(InboundOpcodeTrace.shouldLog(Opcodes.CMSG_GOSSIP_HELLO));
    }

    @Test
    void shouldLogWhenHeartbeatShouldBeFalse() {
        assertFalse(InboundOpcodeTrace.shouldLog(Opcodes.MSG_MOVE_HEARTBEAT));
    }

    @Test
    void shouldLogWhenPingShouldBeFalse() {
        assertFalse(InboundOpcodeTrace.shouldLog(Opcodes.CMSG_PING));
    }

    @Test
    void formatWhenPlayerNamedShouldIncludeOpcodeAndPlayer() {
        String line = InboundOpcodeTrace.format("man", Opcodes.CMSG_GOSSIP_HELLO, 8);
        assertEquals("C2S CMSG_GOSSIP_HELLO (0x17B) size=8 player=man", line);
    }
}
