package org.tbc.world.map;

/** GridDefines.h MaNGOS::IsValidMapCoord. */
public final class MapCoords {
    /** MAP_HALFSIZE = SIZE_OF_GRIDS * MAX_NUMBER_OF_GRIDS / 2. */
    public static final float MAP_HALFSIZE = 533.33333f * 64 / 2;

    private MapCoords() {}

    public static boolean valid(float c) {
        return Float.isFinite(c) && Math.abs(c) <= MAP_HALFSIZE - 0.5f;
    }

    public static boolean valid(float x, float y) {
        return valid(x) && valid(y);
    }

    public static boolean valid(float x, float y, float z, float o) {
        return valid(x, y) && Float.isFinite(z) && Float.isFinite(o);
    }
}
