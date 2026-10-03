package org.tbc.world.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * CMaNGOS PathFinder.calculate / ComputePathToRandomPoint.
 * When {@link NavQuery} is missing, returns {@link PathType#NOT_USING_PATH} shortcut
 * (in-memory CI). When query is present and findPath is empty → {@link PathType#NOPATH}.
 */
public final class PathFinder {
    public record Result(int type, List<Waypoint> points) {
        public boolean nopath() {
            return PathType.nopath(type);
        }

        public boolean usable() {
            return PathType.usable(type);
        }
    }

    private final NavQuery nav;
    /** When true, missing mesh is NOPATH (lab with mmap.enabled). CI false → shortcut. */
    private final boolean requireMesh;

    public PathFinder(NavQuery nav, boolean requireMesh) {
        this.nav = nav;
        this.requireMesh = requireMesh;
    }

    public static PathFinder straightLine() {
        return new PathFinder(null, false);
    }

    public static PathFinder requiring(NavQuery nav) {
        return new PathFinder(nav, true);
    }

    public Result calculate(int mapId, float sx, float sy, float sz,
            float dx, float dy, float dz, boolean forceDest, boolean straightLine) {
        if (nav == null || !nav.available(mapId)) {
            if (requireMesh) {
                return new Result(PathType.NOPATH, List.of());
            }
            return shortcut(dx, dy, dz);
        }
        List<Waypoint> path = nav.findPath(mapId, sx, sy, sz, dx, dy, dz, straightLine);
        if (path == null || path.isEmpty()) {
            if (forceDest) {
                return shortcut(dx, dy, dz);
            }
            return new Result(PathType.NOPATH, List.of());
        }
        return new Result(PathType.NORMAL, List.copyOf(path));
    }

    public Result randomPoint(int mapId, float hx, float hy, float hz, float radius,
            DoubleSupplier rng) {
        double a = rng == null ? 0 : rng.getAsDouble();
        double d = rng == null ? 1 : rng.getAsDouble();
        if (nav == null || !nav.available(mapId)) {
            if (requireMesh) {
                return new Result(PathType.NOPATH, List.of());
            }
            float x = hx + (float) (Math.cos(a * Math.PI * 2) * d * radius);
            float y = hy + (float) (Math.sin(a * Math.PI * 2) * d * radius);
            return shortcut(x, y, hz);
        }
        List<Waypoint> path = nav.randomPoint(mapId, hx, hy, hz, radius, a, d);
        if (path == null || path.isEmpty()) {
            return new Result(PathType.NOPATH, List.of());
        }
        return new Result(PathType.NORMAL, List.copyOf(path));
    }

    private static Result shortcut(float x, float y, float z) {
        List<Waypoint> pts = new ArrayList<>(1);
        pts.add(new Waypoint(x, y, z));
        return new Result(PathType.NOT_USING_PATH | PathType.SHORTCUT, pts);
    }
}
