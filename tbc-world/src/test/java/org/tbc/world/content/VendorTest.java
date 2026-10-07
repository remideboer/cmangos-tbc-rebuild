package org.tbc.world.content;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;

/** SMSG_LIST_INVENTORY encoding owned by the Vendor split out of Content (plan cycle 5.2). */
class VendorTest {

    @Test
    void encodeVendorListWhenNoStockShouldSendGuidAndTwoZeroBytes() {
        ObjectMgr mgr = new ObjectMgr();
        Vendor vendor = new Vendor(mgr, (p, map, entry, count, send) -> { });
        Creature c = new Creature();
        c.guid = 0xF130000000000008L;
        c.entry = 424242;

        byte[] out = vendor.encodeVendorList(c);

        WowBuffer expected = new WowBuffer(10);
        expected.putU64(c.guid);
        expected.putU8(0);
        expected.putU8(0);
        assertArrayEquals(expected.array(), out);
    }

    @Test
    void encodeVendorListWhenSeededVendorShouldListStockWithDisplayAndPrice() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Vendor vendor = new Vendor(mgr, (p, map, entry, count, send) -> { });
        Creature c = new Creature();
        c.guid = 8;
        c.entry = Content.NPC_CORINA_STEELE;

        WowBuffer b = new WowBuffer(vendor.encodeVendorList(c));
        b.getU64();
        int count = b.getU8();
        int slot = b.getU32();
        int itemId = b.getU32();

        assertEquals(mgr.itemsForVendor(c.entry).size(), count);
        assertEquals(1, slot);
        assertEquals(mgr.itemsForVendor(c.entry).get(0), itemId);
    }
}
