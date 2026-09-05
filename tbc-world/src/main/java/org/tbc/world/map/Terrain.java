package org.tbc.world.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * maps/*.map height. spec/05-domain/maps-grids-visibility.md.
 * Without DataDir, {@link #at} returns the hint Z (in-memory / CI).
 */
public final class Terrain {
    public static final float SIZE_OF_GRIDS = 533.33333f;
    public static final int MAP_RESOLUTION = 128;
    public static final float INVALID = -200000f;
    private static final int MAP_HEIGHT_NO_HEIGHT = 0x0001;
    private static final int MAP_HEIGHT_AS_INT16 = 0x0002;
    private static final int MAP_HEIGHT_AS_INT8 = 0x0004;
    private static final int MAPS = fourcc("MAPS");
    private static final int S14 = fourcc("s1.4");
    private static final int MHGT = fourcc("MHGT");
    private static final int AREA = fourcc("AREA");
    private static final int MAP_AREA_NO_AREA = 0x0001;
    private static final int[] HOLE_H = {0x1111, 0x2222, 0x4444, 0x8888};
    private static final int[] HOLE_V = {0x000F, 0x00F0, 0x0F00, 0xF000};

    @FunctionalInterface
    public interface Height {
        float at(int mapId, float x, float y, float hint);
    }

    public static final Height NONE = (mapId, x, y, hint) -> hint;

    private final Path mapsDir;
    private final ConcurrentHashMap<Long, Tile> tiles = new ConcurrentHashMap<>();

    public static Terrain fromDataDir(Path dataDir) {
        return new Terrain(dataDir);
    }

    public Terrain(Path dataDir) {
        this.mapsDir = dataDir == null ? null : dataDir.resolve("maps");
    }

    public float at(int mapId, float x, float y, float hint) {
        if (mapsDir == null || !Files.isDirectory(mapsDir)) {
            return hint;
        }
        int gx = (int) (32 - x / SIZE_OF_GRIDS);
        int gy = (int) (32 - y / SIZE_OF_GRIDS);
        if (gx < 0 || gx > 63 || gy < 0 || gy > 63) {
            return hint;
        }
        Tile tile = tiles.computeIfAbsent((((long) mapId) << 16) | (gx << 8) | gy, k -> load(mapId, gx, gy));
        if (tile == null) {
            return hint;
        }
        float h = tile.height(x, y);
        if (h <= INVALID + 1) {
            return hint;
        }
        return h;
    }

    public Height asHeight() {
        return this::at;
    }

    /** GridMap::getArea. 0 if the tile or AREA chunk is missing. */
    public int area(int mapId, float x, float y) {
        if (mapsDir == null || !Files.isDirectory(mapsDir)) {
            return 0;
        }
        int gx = (int) (32 - x / SIZE_OF_GRIDS);
        int gy = (int) (32 - y / SIZE_OF_GRIDS);
        if (gx < 0 || gx > 63 || gy < 0 || gy > 63) {
            return 0;
        }
        Tile tile = tiles.computeIfAbsent((((long) mapId) << 16) | (gx << 8) | gy, k -> load(mapId, gx, gy));
        if (tile == null) {
            return 0;
        }
        return tile.area(x, y);
    }

    private Tile load(int mapId, int gx, int gy) {
        Path file = mapsDir.resolve(String.format("%03d%02d%02d.map", mapId, gx, gy));
        if (!Files.isRegularFile(file)) {
            return Tile.MISSING;
        }
        try {
            ByteBuffer buf = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
            if (buf.remaining() < 40) {
                return Tile.MISSING;
            }
            int magic = buf.getInt();
            int ver = buf.getInt();
            if (magic != MAPS || ver != S14) {
                return Tile.MISSING;
            }
            int areaOff = buf.getInt();
            buf.getInt();
            int heightOff = buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            int holesOff = buf.getInt();
            buf.getInt();
            int gridArea = 0;
            char[] areas = null;
            if (areaOff > 0 && areaOff + 8 <= buf.capacity()) {
                buf.position(areaOff);
                if (buf.getInt() == AREA) {
                    int areaFlags = buf.getChar() & 0xFFFF;
                    gridArea = buf.getChar() & 0xFFFF;
                    if ((areaFlags & MAP_AREA_NO_AREA) == 0) {
                        areas = new char[16 * 16];
                        for (int i = 0; i < areas.length; i++) {
                            areas[i] = buf.getChar();
                        }
                    }
                }
            }
            char[] holes = new char[16 * 16];
            if (holesOff > 0 && holesOff + 512 <= buf.capacity()) {
                buf.position(holesOff);
                for (int i = 0; i < holes.length; i++) {
                    holes[i] = buf.getChar();
                }
            }
            if (heightOff <= 0 || heightOff + 16 > buf.capacity()) {
                return Tile.MISSING;
            }
            buf.position(heightOff);
            if (buf.getInt() != MHGT) {
                return Tile.MISSING;
            }
            int flags = buf.getInt();
            float gridHeight = buf.getFloat();
            float gridMax = buf.getFloat();
            if ((flags & MAP_HEIGHT_NO_HEIGHT) != 0) {
                return new Tile(flags, gridHeight, 0, null, null, null, null, holes, gridArea, areas);
            }
            if ((flags & MAP_HEIGHT_AS_INT16) != 0) {
                char[] v9 = new char[129 * 129];
                char[] v8 = new char[128 * 128];
                for (int i = 0; i < v9.length; i++) {
                    v9[i] = buf.getChar();
                }
                for (int i = 0; i < v8.length; i++) {
                    v8[i] = buf.getChar();
                }
                float mul = (gridMax - gridHeight) / 65535f;
                return new Tile(flags, gridHeight, mul, null, null, v9, v8, holes, gridArea, areas);
            }
            if ((flags & MAP_HEIGHT_AS_INT8) != 0) {
                byte[] v9 = new byte[129 * 129];
                byte[] v8 = new byte[128 * 128];
                buf.get(v9);
                buf.get(v8);
                float mul = (gridMax - gridHeight) / 255f;
                return new Tile(flags, gridHeight, mul, v9, v8, null, null, holes, gridArea, areas);
            }
            float[] v9f = new float[129 * 129];
            float[] v8f = new float[128 * 128];
            for (int i = 0; i < v9f.length; i++) {
                v9f[i] = buf.getFloat();
            }
            for (int i = 0; i < v8f.length; i++) {
                v8f[i] = buf.getFloat();
            }
            return new Tile(flags, gridHeight, 0, null, null, null, null, holes, v9f, v8f, gridArea, areas);
        } catch (Exception e) {
            return Tile.MISSING;
        }
    }

    private static int fourcc(String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
    }

    private static final class Tile {
        static final Tile MISSING = new Tile(MAP_HEIGHT_NO_HEIGHT, INVALID, 0, null, null, null, null, new char[256],
                null, null, 0, null);
        final int flags;
        final float gridHeight;
        final float mul;
        final byte[] u8v9;
        final byte[] u8v8;
        final char[] u16v9;
        final char[] u16v8;
        final float[] f9;
        final float[] f8;
        final char[] holes;
        final int gridArea;
        final char[] areas;

        Tile(int flags, float gridHeight, float mul, byte[] u8v9, byte[] u8v8, char[] u16v9, char[] u16v8,
                char[] holes, int gridArea, char[] areas) {
            this(flags, gridHeight, mul, u8v9, u8v8, u16v9, u16v8, holes, null, null, gridArea, areas);
        }

        Tile(int flags, float gridHeight, float mul, byte[] u8v9, byte[] u8v8, char[] u16v9, char[] u16v8,
                char[] holes, float[] f9, float[] f8, int gridArea, char[] areas) {
            this.flags = flags;
            this.gridHeight = gridHeight;
            this.mul = mul;
            this.u8v9 = u8v9;
            this.u8v8 = u8v8;
            this.u16v9 = u16v9;
            this.u16v8 = u16v8;
            this.holes = holes;
            this.f9 = f9;
            this.f8 = f8;
            this.gridArea = gridArea;
            this.areas = areas;
        }

        int area(float x, float y) {
            if (areas == null) {
                return gridArea;
            }
            float lx = 16 * (32 - x / SIZE_OF_GRIDS);
            float ly = 16 * (32 - y / SIZE_OF_GRIDS);
            int xi = (int) lx & 15;
            int yi = (int) ly & 15;
            return areas[xi * 16 + yi] & 0xFFFF;
        }

        float height(float x, float y) {
            if ((flags & MAP_HEIGHT_NO_HEIGHT) != 0 && f9 == null && u16v9 == null && u8v9 == null) {
                return gridHeight;
            }
            float lx = MAP_RESOLUTION * (32 - x / SIZE_OF_GRIDS);
            float ly = MAP_RESOLUTION * (32 - y / SIZE_OF_GRIDS);
            int xi = (int) lx;
            int yi = (int) ly;
            lx -= xi;
            ly -= yi;
            xi &= MAP_RESOLUTION - 1;
            yi &= MAP_RESOLUTION - 1;
            if (hole(xi, yi)) {
                return INVALID;
            }
            if (f9 != null) {
                return triangle(lx, ly, xi, yi, f9, f8, 1f, 0f);
            }
            if (u16v9 != null) {
                return triangle(lx, ly, xi, yi, u16v9, u16v8);
            }
            if (u8v9 != null) {
                return triangle8(lx, ly, xi, yi);
            }
            return gridHeight;
        }

        private float triangle(float x, float y, int xi, int yi, float[] v9, float[] v8, float scale, float base) {
            float a;
            float b;
            float c;
            if (x + y < 1) {
                if (x > y) {
                    float h1 = v9[xi * 129 + yi];
                    float h2 = v9[(xi + 1) * 129 + yi];
                    float h5 = 2 * v8[xi * 128 + yi];
                    a = h2 - h1;
                    b = h5 - h1 - h2;
                    c = h1;
                } else {
                    float h1 = v9[xi * 129 + yi];
                    float h3 = v9[xi * 129 + yi + 1];
                    float h5 = 2 * v8[xi * 128 + yi];
                    a = h5 - h1 - h3;
                    b = h3 - h1;
                    c = h1;
                }
            } else if (x > y) {
                float h2 = v9[(xi + 1) * 129 + yi];
                float h4 = v9[(xi + 1) * 129 + yi + 1];
                float h5 = 2 * v8[xi * 128 + yi];
                a = h2 + h4 - h5;
                b = h4 - h2;
                c = h5 - h4;
            } else {
                float h3 = v9[xi * 129 + yi + 1];
                float h4 = v9[(xi + 1) * 129 + yi + 1];
                float h5 = 2 * v8[xi * 128 + yi];
                a = h4 - h3;
                b = h3 + h4 - h5;
                c = h5 - h4;
            }
            return a * x + b * y + c;
        }

        private float triangle(float x, float y, int xi, int yi, char[] v9, char[] v8) {
            int a;
            int b;
            int c;
            int h5 = 2 * (v8[xi * 128 + yi] & 0xFFFF);
            if (x + y < 1) {
                if (x > y) {
                    int h1 = v9[xi * 129 + yi] & 0xFFFF;
                    int h2 = v9[(xi + 1) * 129 + yi] & 0xFFFF;
                    a = h2 - h1;
                    b = h5 - h1 - h2;
                    c = h1;
                } else {
                    int h1 = v9[xi * 129 + yi] & 0xFFFF;
                    int h3 = v9[xi * 129 + yi + 1] & 0xFFFF;
                    a = h5 - h1 - h3;
                    b = h3 - h1;
                    c = h1;
                }
            } else if (x > y) {
                int h2 = v9[(xi + 1) * 129 + yi] & 0xFFFF;
                int h4 = v9[(xi + 1) * 129 + yi + 1] & 0xFFFF;
                a = h2 + h4 - h5;
                b = h4 - h2;
                c = h5 - h4;
            } else {
                int h3 = v9[xi * 129 + yi + 1] & 0xFFFF;
                int h4 = v9[(xi + 1) * 129 + yi + 1] & 0xFFFF;
                a = h4 - h3;
                b = h3 + h4 - h5;
                c = h5 - h4;
            }
            return (a * x + b * y + c) * mul + gridHeight;
        }

        private float triangle8(float x, float y, int xi, int yi) {
            int a;
            int b;
            int c;
            int h5 = 2 * (u8v8[xi * 128 + yi] & 0xFF);
            if (x + y < 1) {
                if (x > y) {
                    int h1 = u8v9[xi * 129 + yi] & 0xFF;
                    int h2 = u8v9[(xi + 1) * 129 + yi] & 0xFF;
                    a = h2 - h1;
                    b = h5 - h1 - h2;
                    c = h1;
                } else {
                    int h1 = u8v9[xi * 129 + yi] & 0xFF;
                    int h3 = u8v9[xi * 129 + yi + 1] & 0xFF;
                    a = h5 - h1 - h3;
                    b = h3 - h1;
                    c = h1;
                }
            } else if (x > y) {
                int h2 = u8v9[(xi + 1) * 129 + yi] & 0xFF;
                int h4 = u8v9[(xi + 1) * 129 + yi + 1] & 0xFF;
                a = h2 + h4 - h5;
                b = h4 - h2;
                c = h5 - h4;
            } else {
                int h3 = u8v9[xi * 129 + yi + 1] & 0xFF;
                int h4 = u8v9[(xi + 1) * 129 + yi + 1] & 0xFF;
                a = h4 - h3;
                b = h3 + h4 - h5;
                c = h5 - h4;
            }
            return (a * x + b * y + c) * mul + gridHeight;
        }

        private boolean hole(int row, int col) {
            int cellRow = row / 8;
            int cellCol = col / 8;
            int holeRow = row % 8 / 2;
            int holeCol = (col - cellCol * 8) / 2;
            int hole = holes[cellRow * 16 + cellCol] & 0xFFFF;
            return (hole & HOLE_H[holeCol] & HOLE_V[holeRow]) != 0;
        }
    }
}
