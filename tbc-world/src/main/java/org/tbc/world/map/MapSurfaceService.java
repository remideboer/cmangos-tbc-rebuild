package org.tbc.world.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only candidate floors at world XY for authoring. Does not replace
 * {@link SurfaceQuery#query(int, float, float, float, int)} creature grounding.
 */
public final class MapSurfaceService {
    public static final float MANUAL_TOLERANCE = 2f;

    private final GeometryProvider adt;
    private final GeometryProvider vmap;
    private final boolean dataAvailable;

    private MapSurfaceService(GeometryProvider adt, GeometryProvider vmap, boolean dataAvailable) {
        this.adt = adt;
        this.vmap = vmap;
        this.dataAvailable = dataAvailable;
    }

    public static MapSurfaceService unavailable() {
        return new MapSurfaceService(null, null, false);
    }

    public static MapSurfaceService of(GeometryProvider adt, GeometryProvider vmap) {
        return new MapSurfaceService(adt, vmap, true);
    }

    public static MapSurfaceService fromTerrain(Terrain terrain, GeometryProvider vmapFloors) {
        if (terrain == null) {
            return unavailable();
        }
        GeometryProvider adt = (mapId, x, y) -> {
            float z = terrain.at(mapId, x, y, Terrain.INVALID);
            if (!Float.isFinite(z) || z <= Terrain.INVALID + 1) {
                return new float[0];
            }
            return new float[]{z};
        };
        return of(adt, vmapFloors);
    }

    public FloorCandidates candidateFloors(int mapId, float x, float y) {
        if (!dataAvailable) {
            return FloorCandidates.unresolved();
        }
        List<FloorCandidate> raw = new ArrayList<>();
        collect(adt, mapId, x, y, FloorCandidate.Source.ADT, raw);
        collect(vmap, mapId, x, y, FloorCandidate.Source.VMAP, raw);
        if (raw.isEmpty()) {
            return FloorCandidates.unresolved();
        }
        List<FloorCandidate> merged = mergeNear(raw);
        merged.sort(Comparator.comparingDouble(FloorCandidate::z));
        if (distinctFloors(merged) <= 1) {
            FloorCandidate pick = preferredAdt(merged);
            return FloorCandidates.unique(merged, pick.z());
        }
        FloorCandidate adtFloor = firstAdt(merged);
        Float suggested = adtFloor == null ? null : adtFloor.z();
        return FloorCandidates.ambiguous(merged, suggested);
    }

    public boolean manualZStillValid(float z, FloorCandidates candidates) {
        if (candidates == null || candidates.status() == FloorCandidates.Status.UNRESOLVED
                || !Float.isFinite(z)) {
            return false;
        }
        for (FloorCandidate f : candidates.floors()) {
            if (Math.abs(f.z() - z) <= MANUAL_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private static void collect(GeometryProvider provider, int mapId, float x, float y,
                                FloorCandidate.Source source, List<FloorCandidate> out) {
        if (provider == null) {
            return;
        }
        float[] floors = provider.floors(mapId, x, y);
        if (floors == null) {
            return;
        }
        for (float z : floors) {
            if (Float.isFinite(z)) {
                out.add(new FloorCandidate(z, source));
            }
        }
    }

    private static List<FloorCandidate> mergeNear(List<FloorCandidate> raw) {
        List<FloorCandidate> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingDouble(FloorCandidate::z));
        List<FloorCandidate> merged = new ArrayList<>();
        for (FloorCandidate f : sorted) {
            if (merged.isEmpty()
                    || Math.abs(merged.get(merged.size() - 1).z() - f.z()) > CreatureGrounding.SNAP_EPSILON) {
                merged.add(f);
            }
        }
        return merged;
    }

    private static int distinctFloors(List<FloorCandidate> floors) {
        return floors.size();
    }

    private static FloorCandidate preferredAdt(List<FloorCandidate> floors) {
        FloorCandidate adt = firstAdt(floors);
        return adt == null ? floors.get(0) : adt;
    }

    private static FloorCandidate firstAdt(List<FloorCandidate> floors) {
        for (FloorCandidate f : floors) {
            if (f.source() == FloorCandidate.Source.ADT) {
                return f;
            }
        }
        return null;
    }
}
