package org.tbc.world.map;

/** Outcome of a supporting-surface query for creature grounding. */
public record SurfaceResult(Kind kind, float z) {
    public enum Kind {
        VALID,
        NO_FLOOR,
        UNAVAILABLE
    }

    public static SurfaceResult valid(float z) {
        return new SurfaceResult(Kind.VALID, z);
    }

    public static SurfaceResult noFloor() {
        return new SurfaceResult(Kind.NO_FLOOR, Float.NaN);
    }

    public static SurfaceResult unavailable(float hintZ) {
        return new SurfaceResult(Kind.UNAVAILABLE, hintZ);
    }

    /** Ground movement must stop/replan — hole or blocked vertical segment. */
    public boolean blocksGroundMove() {
        return kind == Kind.NO_FLOOR;
    }
}
