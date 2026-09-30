package org.tbc.content.mpq;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Read-side checks for archives written by {@link MpqWriter} (unit tests / verify). */
public final class MpqReader {
    private MpqReader() {}

    public static byte[] readFile(Path archive, String archivePath) throws IOException {
        byte[] all = Files.readAllBytes(archive);
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        if (buf.getInt() != 0x1A51504D) {
            throw new IOException("not MPQ: " + archive);
        }
        int headerSize = buf.getInt();
        buf.getInt(); // archiveSize
        buf.getShort();
        buf.getShort();
        int hashTablePos = buf.getInt();
        int blockTablePos = buf.getInt();
        int hashSize = buf.getInt();
        int blockCount = buf.getInt();

        byte[] hashBytes = Arrays.copyOfRange(all, hashTablePos, hashTablePos + hashSize * 16);
        MpqWriter.decryptBlock(hashBytes, MpqWriter.hashString("(hash table)", MpqWriter.HASH_FILE_KEY));
        byte[] blockBytes = Arrays.copyOfRange(all, blockTablePos, blockTablePos + blockCount * 16);
        MpqWriter.decryptBlock(blockBytes, MpqWriter.hashString("(block table)", MpqWriter.HASH_FILE_KEY));

        String name = archivePath.replace('/', '\\');
        int nameA = MpqWriter.hashString(name, MpqWriter.HASH_NAME_A);
        int nameB = MpqWriter.hashString(name, MpqWriter.HASH_NAME_B);
        int index = MpqWriter.hashString(name, MpqWriter.HASH_TABLE_OFFSET) & (hashSize - 1);
        int start = index;
        ByteBuffer hashBuf = ByteBuffer.wrap(hashBytes).order(ByteOrder.LITTLE_ENDIAN);
        int blockIndex = -1;
        while (true) {
            int off = index * 16;
            int a = hashBuf.getInt(off);
            int b = hashBuf.getInt(off + 4);
            int bi = hashBuf.getInt(off + 12);
            if (bi == 0xFFFFFFFF) {
                break;
            }
            if (a == nameA && b == nameB) {
                blockIndex = bi;
                break;
            }
            index = (index + 1) & (hashSize - 1);
            if (index == start) {
                break;
            }
        }
        if (blockIndex < 0 || blockIndex >= blockCount) {
            throw new IOException("file not found in MPQ: " + archivePath);
        }
        ByteBuffer blockBuf = ByteBuffer.wrap(blockBytes).order(ByteOrder.LITTLE_ENDIAN);
        int bo = blockIndex * 16;
        int filePos = blockBuf.getInt(bo);
        int cSize = blockBuf.getInt(bo + 4);
        return Arrays.copyOfRange(all, filePos, filePos + cSize);
    }

    public static String readAscii(Path archive, String archivePath) throws IOException {
        return new String(readFile(archive, archivePath), StandardCharsets.US_ASCII);
    }
}
