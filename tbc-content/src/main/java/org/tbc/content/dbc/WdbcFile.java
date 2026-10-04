package org.tbc.content.dbc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mutable WDBC (read + write). Layout: spec/03-protocol/dbc-files.md */
public final class WdbcFile {
    public static final int MAGIC = 0x43424457; // WDBC

    private final int fieldCount;
    private final int recordSize;
    private final List<int[]> records;
    private byte[] strings;

    public WdbcFile(int fieldCount, int recordSize, List<int[]> records, byte[] strings) {
        this.fieldCount = fieldCount;
        this.recordSize = recordSize;
        this.records = new ArrayList<>(records.size());
        for (int[] row : records) {
            this.records.add(Arrays.copyOf(row, row.length));
        }
        this.strings = strings == null ? new byte[]{0} : Arrays.copyOf(strings, strings.length);
    }

    public int fieldCount() {
        return fieldCount;
    }

    public int recordSize() {
        return recordSize;
    }

    public List<int[]> records() {
        return records;
    }

    public byte[] strings() {
        return strings;
    }

    /** True when each field is one byte (e.g. CharBaseInfo race×class). */
    public boolean packedBytes() {
        return recordSize == fieldCount && fieldCount > 0;
    }

    public static WdbcFile load(Path path) throws IOException {
        return parse(Files.readAllBytes(path), String.valueOf(path));
    }

    public static WdbcFile read(byte[] all) throws IOException {
        return parse(all, "bytes");
    }

    private static WdbcFile parse(byte[] all, String source) throws IOException {
        if (all == null) {
            throw new IOException("not WDBC: " + source);
        }
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("not WDBC: " + source);
        }
        int recordCount = buf.getInt();
        int fieldCount = buf.getInt();
        int recordSize = buf.getInt();
        int stringSize = buf.getInt();
        boolean packed = recordSize == fieldCount && fieldCount > 0;
        if (!packed && recordSize != fieldCount * 4) {
            throw new IOException("recordSize " + recordSize + " incompatible with fieldCount "
                    + fieldCount + " in " + source);
        }
        List<int[]> rows = new ArrayList<>(recordCount);
        for (int r = 0; r < recordCount; r++) {
            int[] row = new int[fieldCount];
            if (packed) {
                for (int i = 0; i < fieldCount; i++) {
                    row[i] = buf.get() & 0xFF;
                }
            } else {
                for (int i = 0; i < fieldCount; i++) {
                    row[i] = buf.getInt();
                }
            }
            rows.add(row);
        }
        byte[] strings = new byte[Math.max(1, stringSize)];
        if (stringSize > 0) {
            buf.get(strings, 0, stringSize);
        } else {
            strings = new byte[]{0};
        }
        return new WdbcFile(fieldCount, recordSize, rows, strings);
    }

    public void save(Path path) throws IOException {
        rebuildStringBlockIfNeeded(null);
        int recordCount = records.size();
        int stringSize = strings.length;
        ByteBuffer buf = ByteBuffer.allocate(20 + recordCount * recordSize + stringSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(MAGIC);
        buf.putInt(recordCount);
        buf.putInt(fieldCount);
        buf.putInt(recordSize);
        buf.putInt(stringSize);
        boolean packed = packedBytes();
        for (int[] row : records) {
            if (packed) {
                for (int i = 0; i < fieldCount; i++) {
                    buf.put((byte) (row[i] & 0xFF));
                }
            } else {
                for (int i = 0; i < fieldCount; i++) {
                    buf.putInt(row[i]);
                }
            }
        }
        buf.put(strings);
        Files.createDirectories(path.getParent() != null ? path.getParent() : Path.of("."));
        Files.write(path, buf.array());
    }

    public String str(int offset) {
        if (offset <= 0 || offset >= strings.length) {
            return "";
        }
        int e = offset;
        while (e < strings.length && strings[e] != 0) {
            e++;
        }
        return new String(strings, offset, e - offset, StandardCharsets.UTF_8);
    }

    public int findRecordIndexById(int id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i)[0] == id) {
                return i;
            }
        }
        return -1;
    }

    /** Index of first row matching all field values, or -1. */
    public int findRecordIndex(int... values) {
        if (values == null || values.length != fieldCount) {
            return -1;
        }
        outer:
        for (int i = 0; i < records.size(); i++) {
            int[] row = records.get(i);
            for (int f = 0; f < fieldCount; f++) {
                if (row[f] != values[f]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /** Append a row when no identical row exists. */
    public void appendUniqueRecord(int... values) {
        if (values == null || values.length != fieldCount) {
            throw new IllegalArgumentException("expected " + fieldCount + " fields");
        }
        if (findRecordIndex(values) >= 0) {
            return;
        }
        records.add(Arrays.copyOf(values, fieldCount));
    }

    /**
     * Set a field on the record with matching id (column 0). String fields update the string block.
     */
    public void setField(DbcBinding binding, int id, String fieldName, Object value) {
        BindingField field = binding.field(fieldName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "unknown field '" + fieldName + "' in " + binding.dbcName()));
        if (field.index() >= fieldCount) {
            throw new IllegalArgumentException("field index out of range: " + field.name());
        }
        int rowIdx = findRecordIndexById(id);
        if (rowIdx < 0) {
            throw new IllegalArgumentException(binding.dbcName() + " has no record id=" + id);
        }
        int[] row = records.get(rowIdx);
        switch (field.kind()) {
            case STRING -> {
                String text = value == null ? "" : String.valueOf(value);
                row[field.index()] = internString(text);
            }
            case FLOAT -> {
                float f = value instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(value));
                row[field.index()] = Float.floatToIntBits(f);
            }
            case INT, UINT -> {
                int v = value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
                row[field.index()] = v;
            }
        }
    }

    /** Append string to block; returns offset. Does not dedupe (merge rebuilds later). */
    public int internString(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int offset = strings.length;
        byte[] next = Arrays.copyOf(strings, strings.length + bytes.length + 1);
        System.arraycopy(bytes, 0, next, strings.length, bytes.length);
        next[next.length - 1] = 0;
        strings = next;
        return offset;
    }

    /**
     * Rebuild string block from all string-typed columns so offsets stay compact after many edits.
     */
    public void rebuildStringBlock(DbcBinding binding) {
        Map<String, Integer> seen = new HashMap<>();
        seen.put("", 0);
        List<Byte> block = new ArrayList<>();
        block.add((byte) 0);
        for (int[] row : records) {
            for (BindingField f : binding.fields()) {
                if (!f.isString()) {
                    continue;
                }
                String s = str(row[f.index()]);
                Integer off = seen.get(s);
                if (off == null) {
                    off = block.size();
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    for (byte b : bytes) {
                        block.add(b);
                    }
                    block.add((byte) 0);
                    seen.put(s, off);
                }
                row[f.index()] = off;
            }
        }
        strings = new byte[block.size()];
        for (int i = 0; i < block.size(); i++) {
            strings[i] = block.get(i);
        }
    }

    private void rebuildStringBlockIfNeeded(DbcBinding binding) {
        // no-op for raw save; callers rebuild explicitly when binding known
    }

    /**
     * Clone an existing record and assign a new id (column 0). Used to insert Classless id 6
     * from a Warrior template row without inventing full DBC column defaults.
     */
    public void cloneRecord(int templateId, int newId) {
        int src = findRecordIndexById(templateId);
        if (src < 0) {
            throw new IllegalArgumentException("no template id=" + templateId);
        }
        if (findRecordIndexById(newId) >= 0) {
            return;
        }
        int[] copy = Arrays.copyOf(records.get(src), records.get(src).length);
        copy[0] = newId;
        records.add(copy);
    }

    /** Tiny in-memory fixture for tests (id + one string field). */
    public static WdbcFile fixtureTwoFields(int id, String text) {
        byte[] strBlock = new byte[]{0};
        WdbcFile f = new WdbcFile(2, 8, List.of(new int[]{id, 0}), strBlock);
        f.records.get(0)[1] = f.internString(text);
        return f;
    }
}
