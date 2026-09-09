package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountDataTest {
    @Test
    void requestWhenTypeOutOfRangeShouldReturnNull() {
        AccountData data = new AccountData();
        WowBuffer in = new WowBuffer(4);
        in.putU32(AccountData.NUM_TYPES);
        assertNull(data.request(in));
    }

    @Test
    void requestWhenTruncatedShouldReturnNull() {
        assertNull(new AccountData().request(new WowBuffer(2)));
    }

    @Test
    void updateWhenSizeZeroShouldEraseStoredBlob() {
        AccountData data = new AccountData();
        byte[] raw = "keep\0".getBytes(StandardCharsets.UTF_8);
        data.update(updatePacket(1, raw.length, deflate(raw)));
        WowBuffer erase = new WowBuffer(8);
        erase.putU32(1);
        erase.putU32(0);
        data.update(erase);
        WowBuffer req = new WowBuffer(4);
        req.putU32(1);
        WowBuffer out = new WowBuffer(data.request(req));
        assertEquals(1, out.getU32());
        assertEquals(0, out.getU32());
        assertEquals(0, out.remaining());
    }

    @Test
    void updateWhenSizeTooBigOrTruncatedShouldIgnore() {
        AccountData data = new AccountData();
        WowBuffer huge = new WowBuffer(8);
        huge.putU32(0);
        huge.putU32(AccountData.MAX_SIZE + 1);
        data.update(huge);
        WowBuffer req = new WowBuffer(4);
        req.putU32(0);
        WowBuffer out = new WowBuffer(data.request(req));
        assertEquals(0, out.getU32());
        assertEquals(0, out.getU32());
        data.update(new WowBuffer(4));
    }

    private static WowBuffer updatePacket(int type, int size, byte[] zlib) {
        WowBuffer b = new WowBuffer(8 + zlib.length);
        b.putU32(type);
        b.putU32(size);
        b.putBytes(zlib);
        return b;
    }

    private static byte[] deflate(byte[] raw) {
        Deflater def = new Deflater();
        def.setInput(raw);
        def.finish();
        byte[] z = new byte[128];
        int n = def.deflate(z);
        def.end();
        return java.util.Arrays.copyOf(z, n);
    }
}
