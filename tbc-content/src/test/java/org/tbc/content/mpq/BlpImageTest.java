package org.tbc.content.mpq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlpImageTest {
    @Test
    void decodeWhenPaletteRedPixelShouldMatchNoggitChannelSwap() {
        byte[] file = paletteBlp(2, 2, new byte[]{0, 0, 0, 0});
        BlpImage.Image image = BlpImage.decode(file);
        assertEquals(2, image.width());
        assertEquals(0xFFFF0000, image.argb()[0]);
    }

    @Test
    void readWhenUncompressedMpqShouldReturnPackedBytes(@TempDir Path tmp) throws Exception {
        Path blp = tmp.resolve("tile.blp");
        byte[] raw = paletteBlp(2, 2, new byte[]{0, 0, 0, 0});
        Files.write(blp, raw);
        Path mpq = tmp.resolve("patch.MPQ");
        MpqWriter.create(mpq, Map.of("Interface\\WorldMap\\Elwynn\\Elwynn1.blp", blp));
        byte[] back = ClientMpq.read(mpq, "Interface\\WorldMap\\Elwynn\\Elwynn1.blp");
        assertEquals(raw.length, back.length);
        assertEquals(0xFFFF0000, BlpImage.decode(back).argb()[0]);
    }

    @Test
    void decodeWhenDxt5OpaqueRedShouldKeepAlpha() {
        BlpImage.Image image = BlpImage.decode(dxtBlp(7, dxt5Block(255, 0, 0, 0xF800, 0xF800, 0)));
        assertEquals(4, image.width());
        assertEquals(0xFFFF0000, image.argb()[0]);
    }

    @Test
    void decodeWhenDxt5IndexSixShouldBeTransparent() {
        long alphaBits = 0;
        for (int i = 0; i < 16; i++) {
            alphaBits |= 6L << (3 * i);
        }
        BlpImage.Image image = BlpImage.decode(dxtBlp(7, dxt5Block(0, 255, alphaBits, 0xF800, 0xF800, 0)));
        assertEquals(0x00FF0000, image.argb()[0]);
    }

    @Test
    void decodeWhenDxt3NibbleShouldExpandAlpha() {
        byte[] block = new byte[16];
        for (int i = 0; i < 8; i++) {
            block[i] = (byte) 0xFF;
        }
        block[8] = 0;
        block[9] = (byte) 0xF8;
        block[10] = 0;
        block[11] = (byte) 0xF8;
        BlpImage.Image image = BlpImage.decode(dxtBlp(1, block));
        assertEquals(0xFFFF0000, image.argb()[0]);
    }

    @Test
    void decodeWhenAlphaTypeUnusedShouldReturnNull() {
        assertEquals(null, BlpImage.decode(dxtBlp(2, new byte[16])));
    }

    static byte[] paletteBlp(int width, int height, byte[] indices) {
        int palBytes = 256 * 4;
        int pixelAt = 148 + palBytes;
        byte[] file = new byte[pixelAt + indices.length];
        ByteBuffer buf = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x32504C42);
        buf.putInt(1);
        file[8] = 1;
        buf.putInt(12, width);
        buf.putInt(16, height);
        buf.putInt(20, pixelAt);
        buf.putInt(84, indices.length);
        file[148] = 0;
        file[149] = 0;
        file[150] = (byte) 255;
        file[151] = (byte) 255;
        System.arraycopy(indices, 0, file, pixelAt, indices.length);
        return file;
    }

    private static byte[] dxt5Block(int a0, int a1, long alphaBits, int c0, int c1, int colorBits) {
        byte[] block = new byte[16];
        block[0] = (byte) a0;
        block[1] = (byte) a1;
        for (int i = 0; i < 6; i++) {
            block[2 + i] = (byte) ((alphaBits >>> (8 * i)) & 0xFF);
        }
        block[8] = (byte) c0;
        block[9] = (byte) (c0 >>> 8);
        block[10] = (byte) c1;
        block[11] = (byte) (c1 >>> 8);
        block[12] = (byte) colorBits;
        block[13] = (byte) (colorBits >>> 8);
        block[14] = (byte) (colorBits >>> 16);
        block[15] = (byte) (colorBits >>> 24);
        return block;
    }

    private static byte[] dxtBlp(int alphaType, byte[] block) {
        byte[] file = new byte[148 + block.length];
        ByteBuffer buf = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x32504C42);
        buf.putInt(1);
        file[8] = 2;
        file[9] = 8;
        file[10] = (byte) alphaType;
        buf.putInt(12, 4);
        buf.putInt(16, 4);
        buf.putInt(20, 148);
        buf.putInt(84, block.length);
        System.arraycopy(block, 0, file, 148, block.length);
        return file;
    }
}
