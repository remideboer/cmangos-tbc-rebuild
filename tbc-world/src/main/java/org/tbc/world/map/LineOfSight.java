package org.tbc.world.map;

import org.tbc.world.entity.Entity;

/** vmaps/ LOS. Without DataDir geometry, C++ LOS-disabled: always clear. */
public final class LineOfSight {
    private LineOfSight() {
    }

    public static boolean clear(Entity a, Entity b) {
        return true;
    }
}
