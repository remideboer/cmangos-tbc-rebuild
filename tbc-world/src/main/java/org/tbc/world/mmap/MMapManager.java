package org.tbc.world.mmap;

import org.tbc.world.ai.NavQuery;
import org.tbc.world.ai.Waypoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * DataDir {@code mmaps/%03d.mmap} + tiles. Native Detour via {@code tbcnav} when loaded;
 * otherwise {@link #available} is false so PathFinder stays conservative when required.
 */
public final class MMapManager implements NavQuery {
    public static final int MMAP_MAGIC = 0x4d4d4150;
    public static final int MMAP_VERSION = 8;

    private final Path mmapsDir;
    private final boolean enabled;
    private final boolean nativeReady;

    public static MMapManager fromDataDir(Path dataDir, boolean enabled) {
        Path dir = dataDir == null ? null : dataDir.resolve("mmaps");
        boolean on = enabled && dir != null && Files.isDirectory(dir);
        MMapManager m = new MMapManager(dir, on, DetourNative.isLoaded());
        if (m.nativeReady && dataDir != null) {
            try {
                DetourNative.setDataDir(dataDir.toAbsolutePath().toString());
            } catch (UnsatisfiedLinkError ignored) {
                return new MMapManager(dir, on, false);
            }
        }
        return m;
    }

    MMapManager(Path mmapsDir, boolean enabled, boolean nativeReady) {
        this.mmapsDir = mmapsDir;
        this.enabled = enabled;
        this.nativeReady = nativeReady;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean nativeReady() {
        return nativeReady;
    }

    /** True when the mmap params file exists (MMAP v8). */
    public boolean hasMapFile(int mapId) {
        if (mmapsDir == null) {
            return false;
        }
        Path f = mmapsDir.resolve(String.format("%03d.mmap", mapId));
        if (!Files.isRegularFile(f)) {
            return false;
        }
        try {
            byte[] raw = Files.readAllBytes(f);
            if (raw.length < 4) {
                return false;
            }
            // dtNavMeshParams blob — presence is enough; native validates on load.
            return raw.length >= 28;
        } catch (Exception e) {
            return false;
        }
    }

    /** Tile header magic/version (MoveMap.cpp addTile). */
    public static boolean validTileHeader(byte[] header) {
        if (header == null || header.length < 20) {
            return false;
        }
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        return b.getInt() == MMAP_MAGIC && b.getInt() >= 0 && b.getInt() == MMAP_VERSION;
    }

    @Override
    public boolean available(int mapId) {
        return enabled && nativeReady && hasMapFile(mapId);
    }

    @Override
    public List<Waypoint> findPath(int mapId, float sx, float sy, float sz,
            float dx, float dy, float dz, boolean straightLine) {
        if (!available(mapId)) {
            return List.of();
        }
        float[] pts = DetourNative.findPath(mapId, sx, sy, sz, dx, dy, dz, straightLine);
        return decode(pts);
    }

    @Override
    public List<Waypoint> randomPoint(int mapId, float hx, float hy, float hz, float radius,
            double angle01, double dist01) {
        if (!available(mapId)) {
            return List.of();
        }
        float[] pts = DetourNative.randomPoint(mapId, hx, hy, hz, radius, (float) angle01, (float) dist01);
        return decode(pts);
    }

    static List<Waypoint> decode(float[] xyz) {
        if (xyz == null || xyz.length < 3 || xyz.length % 3 != 0) {
            return List.of();
        }
        List<Waypoint> out = new ArrayList<>(xyz.length / 3);
        for (int i = 0; i < xyz.length; i += 3) {
            out.add(new Waypoint(xyz[i], xyz[i + 1], xyz[i + 2]));
        }
        return out;
    }
}
