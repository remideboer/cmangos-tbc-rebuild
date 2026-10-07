package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;

/** Gossip encodings owned by the Gossip split out of Content (plan cycle 5.2). */
class GossipTest {

    @Test
    void encodeBinderConfirmShouldSendRawGuid() {
        Creature c = new Creature();
        c.guid = 0xF13000000000000AL;

        byte[] out = Gossip.encodeBinderConfirm(c);

        WowBuffer expected = new WowBuffer(8);
        expected.putU64(c.guid);
        assertArrayEquals(expected.array(), out);
    }

    @Test
    void encodeGossipPoiShouldWriteFlagsPosIconDataName() {
        ObjectMgr.PointOfInterest poi = new ObjectMgr.PointOfInterest(1, 1.5f, -2.5f, 6, 7, 3, "Bank");

        WowBuffer b = new WowBuffer(Gossip.encodeGossipPoi(poi));

        assertEquals(7, b.getU32());
        assertEquals(1.5f, b.getFloat());
        assertEquals(-2.5f, b.getFloat());
        assertEquals(6, b.getU32());
        assertEquals(3, b.getU32());
        assertEquals("Bank", b.getCString());
    }
}
