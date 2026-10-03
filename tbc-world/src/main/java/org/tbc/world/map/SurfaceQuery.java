package org.tbc.world.map;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.tbc.world.vmap.VMapManager;

/**
 * Nearest supporting surface around a creature hint Z (map + vmap candidates).
 * Emulates CMaNGOS GetHeightInRange / GetHeightStatic selection spirit.
 */
public final class SurfaceQuery {
    /** GridDefines.h DEFAULT_HEIGHT_SEARCH. */
    public static final float DEFAULT_HEIGHT_SEARCH = 10f;

    /** VMapManager2::getHeight shape; a miss is at or below {@link #INVALID_HEIGHT}. */
    @FunctionalInterface
    interface VmapHeight {
        float getHeight(int mapId, float x, float y, float z, float maxSearchDist);
    }

    /** MapDefines INVALID_HEIGHT: anything at or below is "no height". */
    private static final float INVALID_HEIGHT = -100000f;

    private final List<GeometryProvider> providers;
    private final boolean available;
    private final Function<QueryArgs, SurfaceResult> override;

    private record QueryArgs(int mapId, float x, float y, float hintZ, int inhabitType, float maxSearchDist) {
    }

    private SurfaceQuery(List<GeometryProvider> providers, boolean available,
            Function<QueryArgs, SurfaceResult> override) {
        this.providers = providers;
        this.available = available;
        this.override = override;
    }

    public static SurfaceQuery of(GeometryProvider... providers) {
        List<GeometryProvider> list = new ArrayList<>();
        if (providers != null) {
            for (GeometryProvider p : providers) {
                if (p != null) {
                    list.add(p);
                }
            }
        }
        return new SurfaceQuery(List.copyOf(list), !list.isEmpty(), null);
    }

    public static SurfaceQuery unavailable() {
        return new SurfaceQuery(List.of(), false, null);
    }

    /**
     * ADT-only adapter. Keeps floor-safe rule: never pull down to ADT far below the hint
     * (building floors sit above terrain under WMOs). Near-hint ADT syncs outdoor slopes.
     */
    public static SurfaceQuery fromHeight(Terrain.Height height) {
        Terrain.Height h = height == null ? Terrain.NONE : height;
        return new SurfaceQuery(List.of(), true, args -> {
            if (CreatureGrounding.canFly(args.inhabitType)) {
                return SurfaceResult.valid(args.hintZ);
            }
            float mapZ = h.at(args.mapId, args.x, args.y, args.hintZ);
            if (!Float.isFinite(mapZ) || mapZ <= Terrain.INVALID + 1) {
                return SurfaceResult.noFloor();
            }
            if (Math.abs(args.hintZ - mapZ) <= CreatureGrounding.SNAP_EPSILON) {
                return SurfaceResult.valid(mapZ);
            }
            if (mapZ < args.hintZ - CreatureGrounding.SNAP_EPSILON) {
                return SurfaceResult.valid(args.hintZ);
            }
            if (mapZ <= args.hintZ + args.maxSearchDist) {
                return SurfaceResult.valid(mapZ);
            }
            return SurfaceResult.valid(args.hintZ);
        });
    }

    /** TerrainInfo::GetHeightStatic with ADT + VMAP; falls back to {@link #fromHeight} when vmap misses. */
    public static SurfaceQuery combine(Terrain.Height adt, VMapManager vmap) {
        return vmap == null ? fromHeight(adt) : combineHeights(adt, vmap::getHeight);
    }

    static SurfaceQuery combineHeights(Terrain.Height adt, VmapHeight vmap) {
        Terrain.Height h = adt == null ? Terrain.NONE : adt;
        SurfaceQuery adtOnly = fromHeight(h);
        return new SurfaceQuery(List.of(), true, args -> {
            if (CreatureGrounding.canFly(args.inhabitType)) {
                return SurfaceResult.valid(args.hintZ);
            }
            float mapZ = h.at(args.mapId, args.x, args.y, Terrain.INVALID);
            boolean haveMap = Float.isFinite(mapZ) && mapZ > INVALID_HEIGHT;
            float z2 = args.hintZ + 2f;
            float search = args.maxSearchDist;
            if (haveMap && z2 - mapZ > search) {
                search = z2 - mapZ + 1f;
            }
            float vmapZ = vmap.getHeight(args.mapId, args.x, args.y, z2, search);
            if (vmapZ <= INVALID_HEIGHT) {
                vmapZ = vmap.getHeight(args.mapId, args.x, args.y, z2, 10000f);
            }
            if (vmapZ <= INVALID_HEIGHT && haveMap && mapZ > z2 && Math.abs(z2 - mapZ) > 30f) {
                vmapZ = vmap.getHeight(args.mapId, args.x, args.y, z2, -search);
            }
            if (vmapZ <= INVALID_HEIGHT && haveMap && z2 < mapZ) {
                vmapZ = vmap.getHeight(args.mapId, args.x, args.y, mapZ + 2f, DEFAULT_HEIGHT_SEARCH);
            }
            if (vmapZ <= INVALID_HEIGHT) {
                return adtOnly.query(args.mapId, args.x, args.y, args.hintZ, args.inhabitType,
                        args.maxSearchDist);
            }
            if (haveMap && args.hintZ >= mapZ && vmapZ <= mapZ) {
                return SurfaceResult.valid(mapZ);
            }
            return SurfaceResult.valid(vmapZ);
        });
    }

    public SurfaceResult query(int mapId, float x, float y, float hintZ, int inhabitType) {
        return query(mapId, x, y, hintZ, inhabitType, DEFAULT_HEIGHT_SEARCH);
    }

    public SurfaceResult query(int mapId, float x, float y, float hintZ, int inhabitType,
            float maxSearchDist) {
        if (override != null) {
            return override.apply(new QueryArgs(mapId, x, y, hintZ, inhabitType, maxSearchDist));
        }
        if (CreatureGrounding.canFly(inhabitType)) {
            return SurfaceResult.valid(hintZ);
        }
        if (!available) {
            return SurfaceResult.unavailable(hintZ);
        }
        List<Float> candidates = new ArrayList<>();
        boolean sawProvider = false;
        for (GeometryProvider p : providers) {
            float[] floors = p.floors(mapId, x, y);
            if (floors == null) {
                continue;
            }
            sawProvider = true;
            for (float z : floors) {
                if (!Float.isFinite(z)) {
                    continue;
                }
                if (Math.abs(z - hintZ) <= maxSearchDist) {
                    candidates.add(z);
                }
            }
        }
        if (!sawProvider) {
            return SurfaceResult.unavailable(hintZ);
        }
        if (candidates.isEmpty()) {
            return SurfaceResult.noFloor();
        }
        float best = candidates.get(0);
        float bestDist = Math.abs(best - hintZ);
        for (int i = 1; i < candidates.size(); i++) {
            float z = candidates.get(i);
            float d = Math.abs(z - hintZ);
            if (d < bestDist) {
                best = z;
                bestDist = d;
            }
        }
        return SurfaceResult.valid(best);
    }

    /** Relocate helper: use floor when VALID, else keep hint (CI / unavailable). */
    public static float resolveOrHint(SurfaceResult result, float hintZ) {
        if (result != null && result.kind() == SurfaceResult.Kind.VALID) {
            return result.z();
        }
        return hintZ;
    }
}
