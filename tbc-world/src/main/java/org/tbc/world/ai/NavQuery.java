package org.tbc.world.ai;

import java.util.List;

/**
 * Navmesh query used by {@link PathFinder}. Production: Detour via {@code tbcnav}.
 * Tests inject a grid/wall double. Missing native → {@link #available()} false.
 */
public interface NavQuery {
    boolean available(int mapId);

    /**
     * @return waypoints from start to dest (excluding start), or empty if no path
     */
    List<Waypoint> findPath(int mapId, float sx, float sy, float sz,
            float dx, float dy, float dz, boolean straightLine);

    /** Random reachable point near home, or empty. */
    List<Waypoint> randomPoint(int mapId, float hx, float hy, float hz, float radius,
            double angle01, double dist01);
}
