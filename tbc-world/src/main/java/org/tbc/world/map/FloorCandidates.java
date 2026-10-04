package org.tbc.world.map;

import java.util.List;

/** Editor-facing floor query: unique ground, stacked floors, or missing data. */
public record FloorCandidates(Status status, List<FloorCandidate> floors, Float suggestedZ) {
    public enum Status {
        UNIQUE,
        AMBIGUOUS,
        UNRESOLVED
    }

    public FloorCandidates {
        floors = floors == null ? List.of() : List.copyOf(floors);
    }

    public static FloorCandidates unresolved() {
        return new FloorCandidates(Status.UNRESOLVED, List.of(), null);
    }

    public static FloorCandidates unique(List<FloorCandidate> floors, float suggestedZ) {
        return new FloorCandidates(Status.UNIQUE, floors, suggestedZ);
    }

    public static FloorCandidates ambiguous(List<FloorCandidate> floors, Float suggestedZ) {
        return new FloorCandidates(Status.AMBIGUOUS, floors, suggestedZ);
    }
}
