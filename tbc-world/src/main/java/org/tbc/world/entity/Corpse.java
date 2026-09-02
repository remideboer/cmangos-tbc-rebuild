package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;

public final class Corpse extends Entity {
    public static final int TYPEID_CORPSE = 7;
    public long ownerGuid;
    public long expireAtMs;

    public Corpse() {
        super(UpdateFields.CORPSE_END, TYPEID_CORPSE);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, 0x81);
    }
}
