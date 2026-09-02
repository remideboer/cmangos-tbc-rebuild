package org.tbc.world.net.wow8606;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
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
        try (FileChannel ch = FileChannel.open(path)) {
            ByteBuffer hdr = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
            ch.read(hdr);
            hdr.flip();
            int magic = hdr.getInt();
            if (magic != 0x43424457) {
                throw new IOException("not WDBC: " + path);
            }
            int recordCount = hdr.getInt();
            int fieldCount = hdr.getInt();
            int recordSize = hdr.getInt();
            int stringSize = hdr.getInt();
            ByteBuffer rec = ByteBuffer.allocate(recordCount * recordSize).order(ByteOrder.LITTLE_ENDIAN);
            ch.read(rec);
            rec.flip();
            byte[] strings = new byte[stringSize];
            ByteBuffer sb = ByteBuffer.wrap(strings);
            ch.read(sb);
            DbcFile f = new DbcFile(fieldCount, recordSize, strings);
            int ints = recordSize / 4;
            for (int r = 0; r < recordCount; r++) {
                int[] row = new int[ints];
                for (int i = 0; i < ints; i++) {
                    row[i] = rec.getInt();
                }
                f.records.add(row);
            }
            return f;
        }
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
