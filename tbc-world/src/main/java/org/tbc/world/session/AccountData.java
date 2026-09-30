package org.tbc.world.session;

import org.tbc.common.WowBuffer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** WorldSession.h AccountDataType (0–7). session-misc.md */
public final class AccountData {
    public static final int NUM_TYPES = 8;
    public static final int MAX_SIZE = 0xFFFF;
    /** Bits 0, 2, 4 — account_data table. */
    public static final int GLOBAL_CACHE_MASK = 0x15;
    /** Bits 1, 3, 5, 6, 7 — character_account_data table. */
    public static final int PER_CHARACTER_CACHE_MASK = 0xEA;

    private final String[] slots = new String[NUM_TYPES];
    private final long[] times = new long[NUM_TYPES];

    public AccountData() {
        Arrays.fill(slots, "");
    }

    public static boolean isGlobal(int type) {
        return (GLOBAL_CACHE_MASK & (1 << type)) != 0;
    }

    /** Clear slots covered by mask (LoadAccountData). */
    public void clearMask(int mask) {
        for (int i = 0; i < NUM_TYPES; i++) {
            if ((mask & (1 << i)) != 0) {
                slots[i] = "";
                times[i] = 0;
            }
        }
    }

    public void setSlot(int type, long time, String data) {
        if (Integer.compareUnsigned(type, NUM_TYPES) >= 0) {
            return;
        }
        slots[type] = data == null ? "" : data;
        times[type] = time;
    }

    public String slot(int type) {
        return slots[type];
    }

    /**
     * HandleUpdateAccountData — no SMSG. Returns type written, or -1 when ignored.
     */
    public int update(WowBuffer in) {
        if (in.remaining() < 8) {
            return -1;
        }
        int type = in.getU32();
        int size = in.getU32();
        if (Integer.compareUnsigned(type, NUM_TYPES) >= 0) {
            return -1;
        }
        if (size == 0) {
            slots[type] = "";
            times[type] = 0;
            return type;
        }
        if (Integer.compareUnsigned(size, MAX_SIZE) > 0) {
            return -1;
        }
        byte[] raw = inflate(in.remainingBytes(), size);
        if (raw == null) {
            return -1;
        }
        slots[type] = cString(raw);
        times[type] = 0;
        return type;
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

    /** SMSG_ACCOUNT_DATA_TIMES — 8 × 16-byte MD5 (empty → zeros). login-burst.md */
    public byte[] timesDigest() {
        byte[] out = new byte[NUM_TYPES * 16];
        for (int i = 0; i < NUM_TYPES; i++) {
            if (!slots[i].isEmpty()) {
                System.arraycopy(md5(slots[i]), 0, out, i * 16, 16);
            }
        }
        return out;
    }

    static byte[] md5(String data) {
        try {
            return MessageDigest.getInstance("MD5").digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
