package org.tbc.world.entity;

/** HighGuid player = 0. */
public final class Guid {
    public static final int HIGH_PLAYER = 0;
    public static final int HIGH_UNIT = 0xF1300000; // packed in high 32 of 64-bit in TBC variants
    public static final long HIGH_CREATURE = 0xF130000000000000L;
    public static final long HIGH_GAMEOBJECT = 0xF110000000000000L;
    public static final long HIGH_ITEM = 0x4000000000000000L;
    public static final long HIGH_CORPSE = 0xF101000000000000L;
    /** ObjectGuid(HIGHGUID_GROUP, id) — high 16 bits 0x1F50. */
    public static final long HIGH_GROUP = 0x1F50000000000000L;

    private Guid() {}

    public static long player(int low) {
        return low & 0xFFFFFFFFL;
    }

    public static int low(long guid) {
        return (int) guid;
    }

    public static boolean isPlayer(long guid) {
        return (guid >>> 48) == 0;
    }
}
