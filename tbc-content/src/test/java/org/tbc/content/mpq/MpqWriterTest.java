package org.tbc.content.mpq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MpqWriterTest {
    @TempDir
    Path tmp;

    @Test
    void createWhenOneDbcShouldWriteMpqMagicAndListfile() throws Exception {
        Path dbc = tmp.resolve("Spell.dbc");
        byte[] payload = new byte[]{0x57, 0x44, 0x42, 0x43, 1, 2, 3, 4};
        Files.write(dbc, payload);
        Path mpq = tmp.resolve("patch-tbc-custom.MPQ");
        Map<String, Path> files = new LinkedHashMap<>();
        files.put("DBFilesClient\\Spell.dbc", dbc);
        MpqWriter.create(mpq, files);
        byte[] all = Files.readAllBytes(mpq);
        assertTrue(all.length > 32);
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x1A51504D, buf.getInt());
        assertEquals(32, buf.getInt());

        assertArrayEquals(payload, MpqReader.readFile(mpq, "DBFilesClient\\Spell.dbc"));
        String list = MpqReader.readAscii(mpq, "(listfile)");
        assertTrue(list.contains("DBFilesClient\\Spell.dbc"));

        // 8606 rejects MPQ_FILE_SINGLE_UNIT on large DBCs (client ERROR #131 File Corrupt).
        int flags = readFirstBlockFlags(all);
        assertEquals(0x80000000, flags);
    }

    private static int readFirstBlockFlags(byte[] all) {
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        buf.getInt();
        buf.getInt();
        buf.getInt();
        buf.getShort();
        buf.getShort();
        buf.getInt(); // hashTablePos
        int blockTablePos = buf.getInt();
        buf.getInt(); // hashSize
        int blockCount = buf.getInt();
        assertTrue(blockCount >= 1);
        byte[] blockBytes = java.util.Arrays.copyOfRange(all, blockTablePos, blockTablePos + blockCount * 16);
        MpqWriter.decryptBlock(blockBytes, MpqWriter.hashString("(block table)", MpqWriter.HASH_FILE_KEY));
        return ByteBuffer.wrap(blockBytes).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
    }

    @Test
    void hashStringWhenNameAShouldDifferFromTableOffset() {
        // Regression: old code masked (hashType+ch)&0xFF so 0x100 collapsed to type 0.
        int offset = MpqWriter.hashString("(listfile)", MpqWriter.HASH_TABLE_OFFSET);
        int nameA = MpqWriter.hashString("(listfile)", MpqWriter.HASH_NAME_A);
        int nameB = MpqWriter.hashString("(listfile)", MpqWriter.HASH_NAME_B);
        assertTrue(offset != nameA);
        assertTrue(nameA != nameB);
    }

    @Test
    void encryptDecryptWhenRoundTripShouldRestorePlaintext() {
        byte[] data = "helloworld123456".getBytes(StandardCharsets.US_ASCII); // 16 bytes
        assertEquals(16, data.length);
        int key = MpqWriter.hashString("(hash table)", MpqWriter.HASH_FILE_KEY);
        byte[] enc = data.clone();
        MpqWriter.encryptBlock(enc, key);
        assertTrue(!java.util.Arrays.equals(data, enc));
        MpqWriter.decryptBlock(enc, key);
        assertArrayEquals(data, enc);
    }
}
