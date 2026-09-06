package org.tbc.world.entity;

import java.util.HashMap;
import java.util.Map;

/** In-memory arena team. ArenaTeam.cpp InspectStats / roster. */
public final class ArenaTeam {
    public int id;
    /** Slot 0=2v2, 1=3v3, 2=5v5 (ArenaTeam::GetSlot). */
    public int slot;
    public String name = "";
    public long captainGuid;
    public int rating;
    public int gamesSeason;
    public int winsSeason;
    public final Map<Long, Member> members = new HashMap<>();

    /** Max members = type × 2; slot 0→2, 1→3, 2→5. */
    public int maxMembers() {
        return switch (slot) {
            case 1 -> 6;
            case 2 -> 10;
            default -> 4;
        };
    }

    public static final class Member {
        public int gamesSeason;
        public int personalRating;
    }
}
