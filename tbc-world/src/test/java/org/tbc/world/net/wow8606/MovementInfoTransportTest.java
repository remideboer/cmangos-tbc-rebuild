package org.tbc.world.net.wow8606;

import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MovementInfoTransportTest {
    @Test
    void tpSl27OnTransportPackedGuid() {
        WowBuffer b = new WowBuffer(64);
        b.putU32(MovementInfo.MOVEFLAG_ONTRANSPORT);
        b.putU8(0);
        b.putU32(1);
        b.putFloat(1);
        b.putFloat(2);
        b.putFloat(3);
        b.putFloat(0);
        b.putPackedGuid(0x1FC0000000000001L);
        b.putFloat(0.1f);
        b.putFloat(0.2f);
        b.putFloat(0.3f);
        b.putFloat(0.4f);
        b.putU32(9);
        b.putU32(0);
        MovementInfo m = MovementInfo.readC2s(new WowBuffer(b.array()));
        assertEquals(MovementInfo.MOVEFLAG_ONTRANSPORT, m.moveFlags);
        assertEquals(0x1FC0000000000001L, m.transportGuid);
        assertEquals(9, m.tTime);
    }

    /**
     * Live 8606 sometimes delivers a 41-byte FALLING packet (base 29 + 12 of 16 jump floats).
     * Must not throw ArrayIndexOutOfBoundsException mid-getFloat.
     */
    @Test
    void readC2sWhenFallingJumpBlockTruncatedShouldKeepPositionWithoutThrow() {
        WowBuffer b = new WowBuffer(64);
        b.putU32(MovementInfo.MOVEFLAG_FALLING | MovementInfo.MOVEFLAG_FORWARD);
        b.putU8(0);
        b.putU32(100);
        b.putFloat(10f);
        b.putFloat(20f);
        b.putFloat(30f);
        b.putFloat(1.5f);
        b.putU32(50);
        b.putFloat(1f);
        b.putFloat(2f);
        b.putFloat(3f);
        assertEquals(41, b.size());
        MovementInfo m = MovementInfo.readC2s(new WowBuffer(b.array()));
        assertEquals(10f, m.x);
        assertEquals(20f, m.y);
        assertEquals(30f, m.z);
        assertFalse((m.moveFlags & MovementInfo.MOVEFLAG_FALLING) != 0);
    }
}
