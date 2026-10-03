package org.tbc.world.vmap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Little-endian cursor over a vmap file. Truncated or oversized reads throw {@link IllegalArgumentException}. */
final class VMapReader {
    private final ByteBuffer buf;

    private VMapReader(ByteBuffer buf) {
        this.buf = buf;
    }

    static VMapReader open(Path file) throws IOException {
        return new VMapReader(ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN));
    }

    static VMapReader of(byte[] bytes) {
        return new VMapReader(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN));
    }

    boolean hasRemaining() {
        return buf.hasRemaining();
    }

    /** VMAP readChunk: consumes expected.length() bytes, true when they match. */
    boolean chunk(String expected) {
        byte[] want = expected.getBytes(StandardCharsets.US_ASCII);
        if (buf.remaining() < want.length) {
            return false;
        }
        boolean same = true;
        for (byte b : want) {
            same &= buf.get() == b;
        }
        return same;
    }

    int u8() {
        need(1);
        return buf.get() & 0xFF;
    }

    int u16() {
        need(2);
        return buf.getShort() & 0xFFFF;
    }

    int u32() {
        need(4);
        return buf.getInt();
    }

    float f32() {
        need(4);
        return buf.getFloat();
    }

    /** u32 element count whose payload (count * elemBytes) must still fit in the file. */
    int count(int elemBytes) {
        int n = u32();
        if (n < 0 || (long) n * elemBytes > buf.remaining()) {
            throw new IllegalArgumentException("bad count " + n);
        }
        return n;
    }

    float[] floats(int n) {
        need((long) n * 4);
        float[] out = new float[n];
        buf.asFloatBuffer().get(out);
        buf.position(buf.position() + n * 4);
        return out;
    }

    int[] ints(int n) {
        need((long) n * 4);
        int[] out = new int[n];
        buf.asIntBuffer().get(out);
        buf.position(buf.position() + n * 4);
        return out;
    }

    String ascii(int n) {
        need(n);
        byte[] raw = new byte[n];
        buf.get(raw);
        return new String(raw, StandardCharsets.US_ASCII);
    }

    void skip(int n) {
        need(n);
        buf.position(buf.position() + n);
    }

    private void need(long n) {
        if (n < 0 || n > buf.remaining()) {
            throw new IllegalArgumentException("truncated");
        }
    }
}
