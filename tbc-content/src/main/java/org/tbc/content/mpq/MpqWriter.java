package org.tbc.content.mpq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal MPQ version-1 creator (uncompressed single-unit files).
 * Hash/crypt match StormLib / wowdev.wiki/MPQ so viewers and the 8606 client can list files.
 */
public final class MpqWriter {
    private static final int MPQ_MAGIC = 0x1A51504D; // 'MPQ\x1A' LE
    private static final int HEADER_SIZE = 32;
    private static final int HASH_ENTRY_SIZE = 16;
    private static final int BLOCK_ENTRY_SIZE = 16;
    /** Plain uncompressed file. Do not set SINGLE_UNIT — 8606 StormLib mis-reads large DBCs with it (ERROR #131). */
    private static final int FILE_EXISTS = 0x80000000;

    /** StormLib hash types as crypt-table offsets (type * 0x100). */
    static final int HASH_TABLE_OFFSET = 0x000;
    static final int HASH_NAME_A = 0x100;
    static final int HASH_NAME_B = 0x200;
    static final int HASH_FILE_KEY = 0x300;

    private static final int[] CRYPT_TABLE = buildCryptTable();

    private MpqWriter() {}

    public static void create(Path archivePath, Map<String, Path> archivePathToFile) throws IOException {
        if (archivePathToFile == null || archivePathToFile.isEmpty()) {
            throw new IOException("MPQ needs at least one file");
        }
        List<Entry> entries = new ArrayList<>();
        for (var e : archivePathToFile.entrySet()) {
            byte[] data = Files.readAllBytes(e.getValue());
            entries.add(new Entry(normalize(e.getKey()), data));
        }
        String list = String.join("\r\n", entries.stream().map(en -> en.name).toList()) + "\r\n";
        entries.add(new Entry("(listfile)", list.getBytes(StandardCharsets.US_ASCII)));

        int hashSize = nextPow2(Math.max(16, entries.size() * 2));
        int blockCount = entries.size();

        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        int[] blockOffsets = new int[blockCount];
        int[] blockCSizes = new int[blockCount];
        int[] blockFSizes = new int[blockCount];
        int[] blockFlags = new int[blockCount];
        for (int i = 0; i < blockCount; i++) {
            Entry en = entries.get(i);
            blockOffsets[i] = HEADER_SIZE + dataOut.size();
            dataOut.write(en.data);
            blockCSizes[i] = en.data.length;
            blockFSizes[i] = en.data.length;
            blockFlags[i] = FILE_EXISTS;
        }
        byte[] fileData = dataOut.toByteArray();

        HashSlot[] hashTable = new HashSlot[hashSize];
        for (int i = 0; i < hashSize; i++) {
            hashTable[i] = HashSlot.empty();
        }
        for (int i = 0; i < blockCount; i++) {
            String name = entries.get(i).name;
            int index = hashString(name, HASH_TABLE_OFFSET) & (hashSize - 1);
            int nameA = hashString(name, HASH_NAME_A);
            int nameB = hashString(name, HASH_NAME_B);
            int start = index;
            while (hashTable[index].blockIndex != 0xFFFFFFFF) {
                index = (index + 1) & (hashSize - 1);
                if (index == start) {
                    throw new IOException("hash table full");
                }
            }
            hashTable[index] = new HashSlot(nameA, nameB, 0, 0, i);
        }

        int hashTablePos = HEADER_SIZE + fileData.length;
        int blockTablePos = hashTablePos + hashSize * HASH_ENTRY_SIZE;
        int archiveSize = blockTablePos + blockCount * BLOCK_ENTRY_SIZE;

        ByteBuffer hashBuf = ByteBuffer.allocate(hashSize * HASH_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        for (HashSlot s : hashTable) {
            hashBuf.putInt(s.nameA);
            hashBuf.putInt(s.nameB);
            hashBuf.putShort((short) s.locale);
            hashBuf.putShort((short) s.platform);
            hashBuf.putInt(s.blockIndex);
        }
        byte[] hashBytes = hashBuf.array();
        encryptBlock(hashBytes, hashString("(hash table)", HASH_FILE_KEY));

        ByteBuffer blockBuf = ByteBuffer.allocate(blockCount * BLOCK_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < blockCount; i++) {
            blockBuf.putInt(blockOffsets[i]);
            blockBuf.putInt(blockCSizes[i]);
            blockBuf.putInt(blockFSizes[i]);
            blockBuf.putInt(blockFlags[i]);
        }
        byte[] blockBytes = blockBuf.array();
        encryptBlock(blockBytes, hashString("(block table)", HASH_FILE_KEY));

        ByteBuffer out = ByteBuffer.allocate(archiveSize).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(MPQ_MAGIC);
        out.putInt(HEADER_SIZE);
        out.putInt(archiveSize);
        out.putShort((short) 0);
        out.putShort((short) 3);
        out.putInt(hashTablePos);
        out.putInt(blockTablePos);
        out.putInt(hashSize);
        out.putInt(blockCount);
        out.put(fileData);
        out.put(hashBytes);
        out.put(blockBytes);

        Files.createDirectories(archivePath.getParent() != null ? archivePath.getParent() : Path.of("."));
        Files.write(archivePath, out.array());
    }

    private static String normalize(String path) {
        return path.replace('/', '\\');
    }

    private static int nextPow2(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /**
     * StormLib HashString. {@code hashType} is the crypt-table offset
     * (0 / 0x100 / 0x200 / 0x300), not a 0–3 enum — do not mask with 0xFF.
     */
    static int hashString(String str, int hashType) {
        byte[] bytes = str.toUpperCase(Locale.ROOT).replace('/', '\\').getBytes(StandardCharsets.US_ASCII);
        int seed1 = 0x7FED7FED;
        int seed2 = 0xEEEEEEEE;
        for (byte b : bytes) {
            int ch = b & 0xFF;
            seed1 = CRYPT_TABLE[hashType + ch] ^ (seed1 + seed2);
            seed2 = ch + seed1 + seed2 + (seed2 << 5) + 3;
        }
        return seed1;
    }

    /** StormLib EncryptMPQBlock — seed advances from plaintext. */
    static void encryptBlock(byte[] data, int key) {
        int seed = 0xEEEEEEEE;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < data.length; i += 4) {
            seed += CRYPT_TABLE[0x400 + (key & 0xFF)];
            int plain = buf.getInt(i);
            buf.putInt(i, plain ^ (key + seed));
            key = ((~key << 21) + 0x11111111) | (key >>> 11);
            seed = plain + seed + (seed << 5) + 3;
        }
    }

    /** StormLib DecryptMPQBlock — seed advances from recovered plaintext. */
    static void decryptBlock(byte[] data, int key) {
        int seed = 0xEEEEEEEE;
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < data.length; i += 4) {
            seed += CRYPT_TABLE[0x400 + (key & 0xFF)];
            int plain = buf.getInt(i) ^ (key + seed);
            buf.putInt(i, plain);
            key = ((~key << 21) + 0x11111111) | (key >>> 11);
            seed = plain + seed + (seed << 5) + 3;
        }
    }

    private static int[] buildCryptTable() {
        int[] table = new int[0x500];
        int seed = 0x00100001;
        for (int i = 0; i < 0x100; i++) {
            int idx = i;
            for (int j = 0; j < 5; j++) {
                seed = (seed * 125 + 3) % 0x2AAAAB;
                int temp1 = (seed & 0xFFFF) << 16;
                seed = (seed * 125 + 3) % 0x2AAAAB;
                int temp2 = seed & 0xFFFF;
                table[idx] = temp1 | temp2;
                idx += 0x100;
            }
        }
        return table;
    }

    private record Entry(String name, byte[] data) {}

    private record HashSlot(int nameA, int nameB, int locale, int platform, int blockIndex) {
        static HashSlot empty() {
            return new HashSlot(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFF, 0xFFFF, 0xFFFFFFFF);
        }
    }
}
