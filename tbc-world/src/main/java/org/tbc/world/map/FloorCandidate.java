package org.tbc.world.map;

/** One walkable floor at an XY, tagged by geometry source. */
public record FloorCandidate(float z, Source source) {
    public enum Source {
        ADT,
        VMAP
    }
}
