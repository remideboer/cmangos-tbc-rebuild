package org.tbc.editor.quest;

import org.tbc.world.map.FloorCandidates;

/** Picks the ADT floor for a dragged creature, or keeps the previous height when terrain is missing. */
public final class NpcGround {
    @FunctionalInterface
    public interface Query {
        FloorCandidates at(int mapId, float x, float y);
    }

    private NpcGround() {}

    public static float snap(FloorCandidates floors, float hintZ) {
        if (!found(floors)) {
            return hintZ;
        }
        return floors.suggestedZ();
    }

    public static boolean found(FloorCandidates floors) {
        return floors != null && floors.suggestedZ() != null && Float.isFinite(floors.suggestedZ());
    }
}
