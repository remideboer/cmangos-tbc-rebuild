package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;

public final class Corpse extends Entity {
    public static final int TYPEID_CORPSE = 7;
    /** Corpse.h CORPSE_BONES / CORPSE_RESURRECTABLE_PVE. */
    public static final int CORPSE_BONES = 0;
    public static final int CORPSE_RESURRECTABLE_PVE = 1;
    /** Corpse.cpp IsExpired: bones 60 minutes, resurrectable 3 days. */
    public static final long BONES_MS = 60L * 60_000;
    public static final long RESURRECTABLE_MS = 3L * 24 * 60 * 60_000;
    public long ownerGuid;
    public long expireAtMs;
    public int corpseType = CORPSE_RESURRECTABLE_PVE;

    public Corpse() {
        super(UpdateFields.CORPSE_END, TYPEID_CORPSE);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, 0x81);
    }

    public boolean expired(long nowMs) {
        return expireAtMs != 0 && expireAtMs <= nowMs;
    }
}
