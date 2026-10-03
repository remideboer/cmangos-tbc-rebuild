package org.tbc.world.mmap;

/**
 * Optional JNI/Panama bridge to vendored Detour ({@code tbcnav}). Absent library is not an error —
 * {@link MMapManager#available} stays false and PathFinder uses the CI/lab policy.
 */
public final class DetourNative {
    private static final boolean LOADED;

    static {
        boolean ok = false;
        try {
            System.loadLibrary("tbcnav");
            ok = true;
        } catch (UnsatisfiedLinkError ignored) {
            ok = false;
        }
        LOADED = ok;
    }

    private DetourNative() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static native boolean setDataDir(String path);

    public static native float[] findPath(int mapId, float sx, float sy, float sz,
            float dx, float dy, float dz, boolean straightLine);

    public static native float[] randomPoint(int mapId, float hx, float hy, float hz,
            float radius, float angle01, float dist01);
}
