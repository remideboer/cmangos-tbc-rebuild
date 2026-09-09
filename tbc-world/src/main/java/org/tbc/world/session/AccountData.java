package org.tbc.world.session;

import org.tbc.common.WowBuffer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** WorldSession.h AccountDataType (0–7). session-misc.md */
public final class AccountData {
    public static final int NUM_TYPES = 8;
    public static final int MAX_SIZE = 0xFFFF;
    private final String[] slots = new String[NUM_TYPES];

    public AccountData() {
        Arrays.fill(slots, "");
    }

    /** HandleUpdateAccountData — no SMSG. */
    public void update(WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        int type = in.getU32();
        int size = in.getU32();
        if (Integer.compareUnsigned(type, NUM_TYPES) >= 0) {
            return;
        }
        if (size == 0) {
            slots[type] = "";
            return;
        }
        if (Integer.compareUnsigned(size, MAX_SIZE) > 0) {
            return;
        }
        byte[] raw = inflate(in.remainingBytes(), size);
        if (raw == null) {
            return;
        }
        slots[type] = cString(raw);
    }

    /** HandleRequestAccountData payload, or null when the type is invalid. */
    public byte[] request(WowBuffer in) {
        if (in.remaining() < 4) {
            return null;
        }
        int type = in.getU32();
        if (Integer.compareUnsigned(type, NUM_TYPES) >= 0) {
            return null;
        }
        byte[] raw = slots[type].getBytes(StandardCharsets.UTF_8);
        WowBuffer out = new WowBuffer(8 + raw.length + 32);
        out.putU32(type);
        out.putU32(raw.length);
        if (raw.length > 0) {
            byte[] z = deflate(raw);
            if (z == null) {
                return null;
            }
            out.putBytes(z);
        }
        return out.array();
    }

    static String cString(byte[] raw) {
        int n = 0;
        while (n < raw.length && raw[n] != 0) {
            n++;
        }
        return new String(raw, 0, n, StandardCharsets.UTF_8);
    }

    static byte[] inflate(byte[] z, int size) {
        byte[] raw = new byte[size];
        Inflater inf = new Inflater();
        inf.setInput(z);
        try {
            if (inf.inflate(raw) != size) {
                return null;
            }
            return raw;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inf.end();
        }
    }

    static byte[] deflate(byte[] raw) {
        Deflater def = new Deflater();
        def.setInput(raw);
        def.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length + 16);
        byte[] buf = new byte[256];
        while (!def.finished()) {
            int n = def.deflate(buf);
            out.write(buf, 0, n);
        }
        def.end();
        return out.toByteArray();
    }
}
