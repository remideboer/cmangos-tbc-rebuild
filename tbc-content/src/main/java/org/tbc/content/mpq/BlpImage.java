package org.tbc.content.mpq;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * BLP2 mip 0, following Noggit {@code blp_texture}: palette images are expanded
 * here, DXT blocks are decoded to ARGB instead of being uploaded to GL.
 */
public final class BlpImage {
    public record Image(int width, int height, int[] argb) {}

    private BlpImage() {}

    public static Image decode(byte[] file) {
        if (file == null || file.length < 148) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if (buf.getInt(0) != 0x32504C42) {
            return null;
        }
        int compression = file[8] & 0xFF;
        int alphaDepth = file[9] & 0xFF;
        int alphaType = file[10] & 0xFF;
        int width = buf.getInt(12);
        int height = buf.getInt(16);
        int offset = buf.getInt(20);
        int size = buf.getInt(20 + 64);
        if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
            return null;
        }
        if (offset <= 0 || size <= 0 || offset + size > file.length) {
            return null;
        }
        int[] argb = new int[width * height];
        if (compression == 1) {
            decodePalette(file, width, height, alphaDepth, offset, argb);
        } else if (compression == 2) {
            int kind = alphaType & 3;
            if (kind == 0) {
                decodeDxt1(file, offset, width, height, argb);
            } else if (kind == 1) {
                decodeDxt3(file, offset, width, height, argb);
            } else if (kind == 3) {
                decodeDxt5(file, offset, width, height, argb);
            } else {
                return null;
            }
        } else {
            return null;
        }
        return new Image(width, height, argb);
    }

    public static byte[] zlib(byte[] payload, int expected) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(payload);
            byte[] out = new byte[expected];
            int n = inflater.inflate(out);
            return n == expected ? out : java.util.Arrays.copyOf(out, Math.max(n, 0));
        } finally {
            inflater.end();
        }
    }

    private static void decodePalette(byte[] file, int width, int height, int alphaDepth, int offset, int[] argb) {
        int palAt = 148;
        int pixels = width * height;
        int i = 0;
        int alphaBit = 0;
        int alphaByte = offset + pixels;
        for (int p = 0; p < pixels; p++) {
            int index = file[offset + p] & 0xFF;
            int pal = ByteBuffer.wrap(file, palAt + index * 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int b = pal & 0xFF;
            int g = (pal >> 8) & 0xFF;
            int r = (pal >> 16) & 0xFF;
            int alpha = 0xFF;
            if (alphaDepth == 8 && alphaByte + p < file.length) {
                alpha = file[alphaByte + p] & 0xFF;
            } else if (alphaDepth == 1 && alphaByte < file.length) {
                int src = file[alphaByte] & 0xFF;
                alpha = (src & (1 << alphaBit)) != 0 ? 0xFF : 0;
                alphaBit++;
                if (alphaBit == 8) {
                    alphaBit = 0;
                    alphaByte++;
                }
            }
            argb[i++] = (alpha << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static void decodeDxt1(byte[] file, int offset, int width, int height, int[] argb) {
        int blocksX = (width + 3) / 4;
        int blocksY = (height + 3) / 4;
        int pos = offset;
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                if (pos + 8 > file.length) {
                    return;
                }
                int c0 = (file[pos] & 0xFF) | ((file[pos + 1] & 0xFF) << 8);
                int c1 = (file[pos + 2] & 0xFF) | ((file[pos + 3] & 0xFF) << 8);
                int bits = (file[pos + 4] & 0xFF) | ((file[pos + 5] & 0xFF) << 8)
                        | ((file[pos + 6] & 0xFF) << 16) | ((file[pos + 7] & 0xFF) << 24);
                pos += 8;
                paintBlock(argb, width, height, bx, by, rgb565(c0, c1), bits, null);
            }
        }
    }

    /** DXT3: 4-bit explicit alpha, then a DXT color block that always has four colors. */
    private static void decodeDxt3(byte[] file, int offset, int width, int height, int[] argb) {
        decodeExplicitAlpha(file, offset, width, height, argb, 8);
    }

    /** DXT5: interpolated 8-bit alpha, then the same four-color block. Noggit alphatype 7. */
    private static void decodeDxt5(byte[] file, int offset, int width, int height, int[] argb) {
        int blocksX = (width + 3) / 4;
        int blocksY = (height + 3) / 4;
        int pos = offset;
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                if (pos + 16 > file.length) {
                    return;
                }
                int[] alpha = dxt5Alpha(file, pos);
                pos += 8;
                int c0 = (file[pos] & 0xFF) | ((file[pos + 1] & 0xFF) << 8);
                int c1 = (file[pos + 2] & 0xFF) | ((file[pos + 3] & 0xFF) << 8);
                int bits = (file[pos + 4] & 0xFF) | ((file[pos + 5] & 0xFF) << 8)
                        | ((file[pos + 6] & 0xFF) << 16) | ((file[pos + 7] & 0xFF) << 24);
                pos += 8;
                int[] colors = rgb565Four(c0, c1);
                paintBlock(argb, width, height, bx, by, colors, bits, alpha);
            }
        }
    }

    private static void decodeExplicitAlpha(byte[] file, int offset, int width, int height, int[] argb, int alphaBytes) {
        int blocksX = (width + 3) / 4;
        int blocksY = (height + 3) / 4;
        int pos = offset;
        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                if (pos + alphaBytes + 8 > file.length) {
                    return;
                }
                int[] alpha = new int[16];
                for (int i = 0; i < 16; i += 2) {
                    int packed = file[pos + (i / 2)] & 0xFF;
                    alpha[i] = (packed & 0x0F) * 17;
                    alpha[i + 1] = ((packed >> 4) & 0x0F) * 17;
                }
                pos += alphaBytes;
                int c0 = (file[pos] & 0xFF) | ((file[pos + 1] & 0xFF) << 8);
                int c1 = (file[pos + 2] & 0xFF) | ((file[pos + 3] & 0xFF) << 8);
                int bits = (file[pos + 4] & 0xFF) | ((file[pos + 5] & 0xFF) << 8)
                        | ((file[pos + 6] & 0xFF) << 16) | ((file[pos + 7] & 0xFF) << 24);
                pos += 8;
                paintBlock(argb, width, height, bx, by, rgb565Four(c0, c1), bits, alpha);
            }
        }
    }

    private static int[] dxt5Alpha(byte[] file, int pos) {
        int a0 = file[pos] & 0xFF;
        int a1 = file[pos + 1] & 0xFF;
        int[] table = new int[8];
        table[0] = a0;
        table[1] = a1;
        if (a0 > a1) {
            table[2] = (6 * a0 + a1) / 7;
            table[3] = (5 * a0 + 2 * a1) / 7;
            table[4] = (4 * a0 + 3 * a1) / 7;
            table[5] = (3 * a0 + 4 * a1) / 7;
            table[6] = (2 * a0 + 5 * a1) / 7;
            table[7] = (a0 + 6 * a1) / 7;
        } else {
            table[2] = (4 * a0 + a1) / 5;
            table[3] = (3 * a0 + 2 * a1) / 5;
            table[4] = (2 * a0 + 3 * a1) / 5;
            table[5] = (a0 + 4 * a1) / 5;
            table[6] = 0;
            table[7] = 255;
        }
        long bits = 0;
        for (int i = 0; i < 6; i++) {
            bits |= (long) (file[pos + 2 + i] & 0xFF) << (8 * i);
        }
        int[] alpha = new int[16];
        for (int i = 0; i < 16; i++) {
            alpha[i] = table[(int) ((bits >>> (3 * i)) & 7)];
        }
        return alpha;
    }

    private static void paintBlock(int[] argb, int width, int height, int bx, int by,
                                   int[] colors, int bits, int[] alpha) {
        for (int py = 0; py < 4; py++) {
            for (int px = 0; px < 4; px++) {
                int x = bx * 4 + px;
                int y = by * 4 + py;
                if (x >= width || y >= height) {
                    continue;
                }
                int code = (bits >>> (2 * (py * 4 + px))) & 3;
                int rgb = colors[code] & 0x00FFFFFF;
                int a = alpha == null ? 0xFF : (alpha[py * 4 + px] & 0xFF);
                argb[y * width + x] = (a << 24) | rgb;
            }
        }
    }

    private static int[] rgb565(int c0, int c1) {
        int col0 = pack565(c0);
        int col1 = pack565(c1);
        int[] colors = new int[4];
        colors[0] = col0;
        colors[1] = col1;
        if (c0 > c1) {
            colors[2] = lerp(col0, col1, 1, 2);
            colors[3] = lerp(col0, col1, 2, 1);
        } else {
            colors[2] = lerp(col0, col1, 1, 1);
            colors[3] = 0x00000000;
        }
        return colors;
    }

    /** DXT3/DXT5 color block always uses the four-color palette. */
    private static int[] rgb565Four(int c0, int c1) {
        int col0 = pack565(c0);
        int col1 = pack565(c1);
        return new int[]{col0, col1, lerp(col0, col1, 1, 2), lerp(col0, col1, 2, 1)};
    }

    private static int pack565(int c) {
        int r5 = (c >> 11) & 31;
        int g6 = (c >> 5) & 63;
        int b5 = c & 31;
        int r = (r5 << 3) | (r5 >> 2);
        int g = (g6 << 2) | (g6 >> 4);
        int b = (b5 << 3) | (b5 >> 2);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, int wa, int wb) {
        int r = (((a >> 16) & 0xFF) * wa + ((b >> 16) & 0xFF) * wb) / (wa + wb);
        int g = (((a >> 8) & 0xFF) * wa + ((b >> 8) & 0xFF) * wb) / (wa + wb);
        int bl = ((a & 0xFF) * wa + (b & 0xFF) * wb) / (wa + wb);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
