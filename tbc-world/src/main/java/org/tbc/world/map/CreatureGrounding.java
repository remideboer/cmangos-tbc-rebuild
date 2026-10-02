package org.tbc.world.map;

/**
 * Floor-safe Z for creatures. ADT {@link Terrain} height is ground under WMOs; pulling down to it
 * digs NPCs under building floors. Players land on client collision — we keep DB/path hint Z unless
 * map height is already at the hint (outdoor surface sync).
 */
public final class CreatureGrounding {
    /** SharedDefines.h INHABIT_GROUND. */
    public static final int INHABIT_GROUND = 1;
    /** SharedDefines.h INHABIT_WATER. */
    public static final int INHABIT_WATER = 2;
    /** SharedDefines.h INHABIT_AIR — CanFly. */
    public static final int INHABIT_AIR = 4;
    /** Default creature_template InhabitType (ground|water). */
    public static final int DEFAULT_INHABIT = INHABIT_GROUND | INHABIT_WATER;
    /** Treat map and hint as the same surface within this yards. */
    public static final float SNAP_EPSILON = 0.5f;

    private CreatureGrounding() {
    }

    public static boolean canFly(int inhabitType) {
        return (inhabitType & INHABIT_AIR) != 0;
    }

    /**
     * Pick Z for a creature at (x,y). Never lower Z when ADT is below the hint (building floor).
     * Never raise Z when ADT is above the hint beyond epsilon.
     */
    public static float resolveZ(float hintZ, float mapZ) {
        if (Math.abs(hintZ - mapZ) <= SNAP_EPSILON) {
            return mapZ;
        }
        return hintZ;
    }
}
