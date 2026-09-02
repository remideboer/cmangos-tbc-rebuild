package org.tbc.common;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Little-endian growable buffer. Packed GUID and C-strings as CMaNGOS ByteBuffer. */
public final class WowBuffer {
    private byte[] data;
    private int wpos;
    private int rpos;

    public WowBuffer() {
        this(64);
    }

    public WowBuffer(int cap) {
        this.data = new byte[Math.max(16, cap)];
    }

    public WowBuffer(byte[] raw) {
        this.data = raw.clone();
        this.wpos = raw.length;
    }

    public int size() {
        return wpos;
    }

    public int remaining() {
        return wpos - rpos;
    }

    public int rpos() {
        return rpos;
    }

    public void rpos(int p) {
        rpos = p;
    }

    public byte[] array() {
        return Arrays.copyOf(data, wpos);
    }

    public byte[] remainingBytes() {
        return Arrays.copyOfRange(data, rpos, wpos);
    }

    private void ensure(int n) {
        if (wpos + n <= data.length) {
            return;
        }
        int cap = data.length;
        while (cap < wpos + n) {
            cap *= 2;
        }
        data = Arrays.copyOf(data, cap);
    }

    public void putU8(int v) {
        ensure(1);
        data[wpos++] = (byte) v;
    }

    public void putU16(int v) {
        ensure(2);
        data[wpos++] = (byte) v;
        data[wpos++] = (byte) (v >>> 8);
    }

    public void putU16BE(int v) {
        ensure(2);
        data[wpos++] = (byte) (v >>> 8);
        data[wpos++] = (byte) v;
    }

    public void putU32(int v) {
        ensure(4);
        data[wpos++] = (byte) v;
        data[wpos++] = (byte) (v >>> 8);
        data[wpos++] = (byte) (v >>> 16);
        data[wpos++] = (byte) (v >>> 24);
    }

    public void putU64(long v) {
        putU32((int) v);
        putU32((int) (v >>> 32));
    }

    public void putFloat(float f) {
        putU32(Float.floatToIntBits(f));
    }

    public void putBytes(byte[] b) {
        ensure(b.length);
        System.arraycopy(b, 0, data, wpos, b.length);
        wpos += b.length;
    }

    public void putCString(String s) {
        if (s != null && !s.isEmpty()) {
            putBytes(s.getBytes(StandardCharsets.UTF_8));
        }
        putU8(0);
    }

    public void putPackedGuid(long guid) {
        int maskPos = wpos;
        putU8(0);
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            int b = (int) ((guid >>> (8 * i)) & 0xFF);
            if (b != 0) {
                mask |= 1 << i;
                putU8(b);
            }
        }
        data[maskPos] = (byte) mask;
    }

    public int getU8() {
        return data[rpos++] & 0xFF;
    }

    public int getU16() {
        int lo = getU8();
        return lo | (getU8() << 8);
    }

    public int getU16BE() {
        int hi = getU8();
        return (hi << 8) | getU8();
    }

    public int getU32() {
        int a = getU8();
        int b = getU8();
        int c = getU8();
        int d = getU8();
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    public long getU32U() {
        return getU32() & 0xFFFFFFFFL;
    }

    public long getU64() {
        long lo = getU32U();
        long hi = getU32U();
        return lo | (hi << 32);
    }

    public float getFloat() {
        return Float.intBitsToFloat(getU32());
    }

    public byte[] getBytes(int n) {
        byte[] out = Arrays.copyOfRange(data, rpos, rpos + n);
        rpos += n;
        return out;
    }

    public String getCString() {
        int start = rpos;
        while (rpos < wpos && data[rpos] != 0) {
            rpos++;
        }
        String s = new String(data, start, rpos - start, StandardCharsets.UTF_8);
        if (rpos < wpos) {
            rpos++;
        }
        return s;
    }

    public long getPackedGuid() {
        int mask = getU8();
        long guid = 0;
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) != 0) {
                guid |= (long) getU8() << (8 * i);
            }
        }
        return guid;
    }

    public void skip(int n) {
        rpos += n;
    }
}
