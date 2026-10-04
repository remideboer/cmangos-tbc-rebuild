package org.tbc.world.net.wow8606;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** WDBC loader. spec/03-protocol/dbc-files.md */
public final class DbcFile {
    public final int fieldCount;
    public final int recordSize;
    public final List<int[]> records = new ArrayList<>();
    public final byte[] strings;

    private DbcFile(int fieldCount, int recordSize, byte[] strings) {
        this.fieldCount = fieldCount;
        this.recordSize = recordSize;
        this.strings = strings;
    }

    public static DbcFile load(Path path) throws IOException {
        return read(Files.readAllBytes(path), String.valueOf(path));
    }

    public static DbcFile read(byte[] all) throws IOException {
        return read(all, "bytes");
    }

    private static DbcFile read(byte[] all, String source) throws IOException {
        if (all == null || all.length < 20) {
            throw new IOException("not WDBC: " + source);
        }
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buf.getInt();
        if (magic != 0x43424457) {
            throw new IOException("not WDBC: " + source);
        }
        int recordCount = buf.getInt();
        int fieldCount = buf.getInt();
        int recordSize = buf.getInt();
        int stringSize = buf.getInt();
        int recBytes = recordCount * recordSize;
        if (recBytes < 0 || stringSize < 0 || 20L + recBytes + stringSize > all.length) {
            throw new IOException("truncated WDBC: " + source);
        }
        byte[] strings = new byte[stringSize];
        System.arraycopy(all, 20 + recBytes, strings, 0, stringSize);
        DbcFile f = new DbcFile(fieldCount, recordSize, strings);
        int ints = recordSize / 4;
        ByteBuffer rec = ByteBuffer.wrap(all, 20, recBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int r = 0; r < recordCount; r++) {
            int[] row = new int[ints];
            for (int i = 0; i < ints; i++) {
                row[i] = rec.getInt();
            }
            f.records.add(row);
        }
        return f;
    }

    public String str(int offset) {
        if (offset <= 0 || offset >= strings.length) {
            return "";
        }
        int e = offset;
        while (e < strings.length && strings[e] != 0) {
            e++;
        }
        return new String(strings, offset, e - offset, java.nio.charset.StandardCharsets.UTF_8);
    }
}
