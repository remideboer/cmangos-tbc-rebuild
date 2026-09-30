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

    public static WdbcFile load(Path path) throws IOException {
        byte[] all = Files.readAllBytes(path);
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("not WDBC: " + path);
        }
        int recordCount = buf.getInt();
        int fieldCount = buf.getInt();
        int recordSize = buf.getInt();
        int stringSize = buf.getInt();
        if (recordSize != fieldCount * 4) {
            throw new IOException("recordSize != fieldCount*4 in " + path);
        }
        int ints = fieldCount;
        List<int[]> rows = new ArrayList<>(recordCount);
        for (int r = 0; r < recordCount; r++) {
            int[] row = new int[ints];
            for (int i = 0; i < ints; i++) {
                row[i] = buf.getInt();
            }
            rows.add(row);
        }
        byte[] strings = new byte[Math.max(1, stringSize)];
        buf.get(strings, 0, stringSize);
        if (stringSize == 0) {
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
        for (int[] row : records) {
            for (int i = 0; i < fieldCount; i++) {
                buf.putInt(row[i]);
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

    /** Tiny in-memory fixture for tests (id + one string field). */
    public static WdbcFile fixtureTwoFields(int id, String text) {
        byte[] strBlock = new byte[]{0};
        WdbcFile f = new WdbcFile(2, 8, List.of(new int[]{id, 0}), strBlock);
        f.records.get(0)[1] = f.internString(text);
        return f;
    }
}
