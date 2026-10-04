package org.tbc.content.mpq;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientMpqTest {
    /**
     * A compressed file smaller than one sector still starts with a sector-offset table
     * unless SINGLE_UNIT is set. The first byte of that table is not the zlib method.
     */
    @Test
    void expandWhenSectorTablePrecedesSingleZlibSectorShouldInflate() throws Exception {
        byte[] plain = "WDBC-area".getBytes(StandardCharsets.US_ASCII);
        Deflater deflater = new Deflater();
        deflater.setInput(plain);
        deflater.finish();
        byte[] zipped = new byte[64];
        int n = deflater.deflate(zipped);
        deflater.end();
        byte[] chunk = new byte[1 + n];
        chunk[0] = 0x02;
        System.arraycopy(zipped, 0, chunk, 1, n);
        byte[] data = new byte[12 + chunk.length];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0, 12)
                .putInt(4, data.length)
                .putInt(8, data.length);
        System.arraycopy(chunk, 0, data, 12, chunk.length);
        byte[] out = ClientMpq.expand(data, 0x84000200, plain.length, 4096);
        assertEquals("WDBC-area", new String(out, StandardCharsets.US_ASCII));
    }
}
