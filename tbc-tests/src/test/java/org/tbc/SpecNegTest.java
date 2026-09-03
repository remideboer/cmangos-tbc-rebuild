package org.tbc;

import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecNegTest {
    @Test
    void tpNegVisionOutIsNotV1() {
        assertEquals(0x2E7, Opcodes.CMSG_WARDEN_DATA);
        assertEquals(0x3AF, Opcodes.CMSG_VOICE_SESSION_ENABLE);
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tbc.soap.SoapServer"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tbc.ra.RaSession"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tbc.warden.Warden"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("playerbot.PlayerbotMgr"));
    }
}
