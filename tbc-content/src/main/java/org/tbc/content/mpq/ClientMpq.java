package org.tbc.content.mpq;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Reads one file from a Storm MPQ v1 archive without loading the whole archive.
 * Uncompressed files match {@link MpqWriter}. Zlib sectors cover the 8606 client archives.
 */
public final class ClientMpq {
    private static final int MAGIC = 0x1A51504D;
    private static final int FILE_COMPRESS = 0x00000200;
    private static final int FILE_IMPLODE = 0x00000100;
    private static final int FILE_ENCRYPTED = 0x00010000;
    private static final int FILE_SINGLE = 0x01000000;

    private static final Map<String, ArchiveIndex> INDEX = new HashMap<>();

    private ClientMpq() {}

    public static byte[] read(Path archive, String name) throws IOException {
        ArchiveIndex index = index(archive);
        int blockIndex = findBlock(index.hashBytes, index.hashSize, name);
        if (blockIndex < 0 || blockIndex >= index.blockCount) {
            throw new IOException("file not found in MPQ: " + name);
        }
        ByteBuffer blocks = ByteBuffer.wrap(index.blockBytes).order(ByteOrder.LITTLE_ENDIAN);
        int bo = blockIndex * 16;
        long filePos = Integer.toUnsignedLong(blocks.getInt(bo));
        int cSize = blocks.getInt(bo + 4);
        int fSize = blocks.getInt(bo + 8);
        int flags = blocks.getInt(bo + 12);
        if ((flags & FILE_ENCRYPTED) != 0) {
            throw new IOException("encrypted MPQ file: " + name);
        }
        try (FileChannel ch = FileChannel.open(archive, StandardOpenOption.READ)) {
            byte[] raw = readAt(ch, filePos, cSize);
            int sector = 512 << index.blockShift;
            return expand(raw, flags, fSize, sector);
        }
    }

    private static ArchiveIndex index(Path archive) throws IOException {
        String key = archive.toAbsolutePath().normalize().toString();
        synchronized (INDEX) {
            ArchiveIndex cached = INDEX.get(key);
            if (cached != null) {
                return cached;
            }
        }
        try (FileChannel ch = FileChannel.open(archive, StandardOpenOption.READ)) {
            ByteBuffer head = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
            ch.read(head, 0);
            head.flip();
            if (head.remaining() < 32 || head.getInt() != MAGIC) {
                throw new IOException("not MPQ: " + archive);
            }
            int headerSize = head.getInt();
            head.getInt();
            head.getShort();
            int blockShift = Short.toUnsignedInt(head.getShort());
            long hashPos = Integer.toUnsignedLong(head.getInt());
            long blockPos = Integer.toUnsignedLong(head.getInt());
            int hashSize = head.getInt();
            int blockCount = head.getInt();
            if (headerSize >= 44 && head.remaining() >= 12) {
                head.getLong();
                int hashHi = Short.toUnsignedInt(head.getShort());
                int blockHi = Short.toUnsignedInt(head.getShort());
                hashPos |= ((long) hashHi) << 32;
                blockPos |= ((long) blockHi) << 32;
            }
            if (hashSize <= 0 || blockCount <= 0 || (hashSize & (hashSize - 1)) != 0) {
                throw new IOException("bad MPQ tables: " + archive);
            }
            byte[] hashBytes = readAt(ch, hashPos, hashSize * 16L);
            MpqWriter.decryptBlock(hashBytes, MpqWriter.hashString("(hash table)", MpqWriter.HASH_FILE_KEY));
            byte[] blockBytes = readAt(ch, blockPos, blockCount * 16L);
            MpqWriter.decryptBlock(blockBytes, MpqWriter.hashString("(block table)", MpqWriter.HASH_FILE_KEY));
            ArchiveIndex index = new ArchiveIndex(blockShift, hashSize, blockCount, hashBytes, blockBytes);
            synchronized (INDEX) {
                INDEX.put(key, index);
            }
            return index;
        }
    }

    private record ArchiveIndex(int blockShift, int hashSize, int blockCount, byte[] hashBytes, byte[] blockBytes) {}

    static byte[] expand(byte[] data, int flags, int fileSize, int sectorSize) throws IOException {
        boolean packed = (flags & (FILE_COMPRESS | FILE_IMPLODE)) != 0;
        if (!packed) {
            return data.length == fileSize ? data : Arrays.copyOf(data, Math.min(data.length, fileSize));
        }
        if ((flags & FILE_IMPLODE) != 0) {
            throw new IOException("PKWARE implode is not supported");
        }
        boolean single = (flags & FILE_SINGLE) != 0 || fileSize <= sectorSize;
        if (single) {
            return decompressBlock(data, fileSize);
        }
        int sectors = (fileSize + sectorSize - 1) / sectorSize;
        ByteBuffer off = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        byte[] out = new byte[fileSize];
        int written = 0;
        for (int i = 0; i < sectors; i++) {
            int start = off.getInt(i * 4);
            int end = off.getInt((i + 1) * 4);
            if (start < 0 || end > data.length || end < start) {
                throw new IOException("bad sector table");
            }
            int expect = Math.min(sectorSize, fileSize - written);
            byte[] plain = decompressBlock(Arrays.copyOfRange(data, start, end), expect);
            int n = Math.min(plain.length, expect);
            System.arraycopy(plain, 0, out, written, n);
            written += n;
        }
        return out;
    }

    private static byte[] decompressBlock(byte[] chunk, int expect) throws IOException {
        if (chunk.length == expect) {
            return chunk;
        }
        int method = chunk[0] & 0xFF;
        if (method != 0x02) {
            throw new IOException("unsupported MPQ compression 0x" + Integer.toHexString(method));
        }
        try {
            return BlpImage.zlib(Arrays.copyOfRange(chunk, 1, chunk.length), expect);
        } catch (DataFormatException e) {
            throw new IOException("zlib", e);
        }
    }

    private static int findBlock(byte[] hashBytes, int hashSize, String name) {
        int nameA = MpqWriter.hashString(name, MpqWriter.HASH_NAME_A);
        int nameB = MpqWriter.hashString(name, MpqWriter.HASH_NAME_B);
        int index = MpqWriter.hashString(name, MpqWriter.HASH_TABLE_OFFSET) & (hashSize - 1);
        int start = index;
        ByteBuffer hash = ByteBuffer.wrap(hashBytes).order(ByteOrder.LITTLE_ENDIAN);
        while (true) {
            int off = index * 16;
            int a = hash.getInt(off);
            int b = hash.getInt(off + 4);
            int bi = hash.getInt(off + 12);
            if (bi == 0xFFFFFFFF) {
                return -1;
            }
            if (a == nameA && b == nameB && bi != 0xFFFFFFFE) {
                return bi;
            }
            index = (index + 1) & (hashSize - 1);
            if (index == start) {
                return -1;
            }
        }
    }

    private static byte[] readAt(FileChannel ch, long pos, long size) throws IOException {
        if (size < 0 || size > Integer.MAX_VALUE - 8) {
            throw new IOException("MPQ read too large: " + size);
        }
        ByteBuffer buf = ByteBuffer.allocate((int) size);
        long at = pos;
        while (buf.hasRemaining()) {
            int n = ch.read(buf, at);
            if (n < 0) {
                throw new IOException("short MPQ read at " + pos);
            }
            at += n;
        }
        return buf.array();
    }
}
