package org.tbc.world.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL08-035 — PathFinder wall / NOPATH / shortcut when mesh absent. */
class PathFinderTest {

    @Test
    void calculateWhenNoNavShouldShortcutNotUsingPath() {
        PathFinder pf = PathFinder.straightLine();
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 10, 0, 0, false, false);
        assertTrue(r.usable());
        assertEquals(PathType.NOT_USING_PATH | PathType.SHORTCUT, r.type());
        assertEquals(10f, r.points().get(0).x(), 1e-4f);
    }

    @Test
    void calculateWhenMeshRequiredAndMissingShouldNopath() {
        PathFinder pf = new PathFinder(null, true);
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 10, 0, 0, false, false);
        assertTrue(r.nopath());
        assertTrue(r.points().isEmpty());
    }

    @Test
    void calculateWhenWallBetweenShouldRouteAround() {
        GridNavQuery grid = GridNavQuery.wallAtX(5, -2, 2, -20, 20);
        PathFinder pf = PathFinder.requiring(grid);
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 10, 0, 0, false, false);
        assertEquals(PathType.NORMAL, r.type());
        assertFalse(r.points().isEmpty());
        for (Waypoint p : r.points()) {
            assertFalse(Math.round(p.x()) == 5 && Math.round(p.y()) >= -2 && Math.round(p.y()) <= 2);
        }
        Waypoint last = r.points().get(r.points().size() - 1);
        assertEquals(10f, last.x(), 0.01f);
    }

    @Test
    void calculateWhenDisconnectedShouldNopath() {
        NavQuery island = new GridNavQuery(-2, 2, -2, 2, (x, y) -> x <= 2);
        // dest outside walkable box
        PathFinder pf = PathFinder.requiring(island);
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 50, 0, 0, false, false);
        assertTrue(r.nopath());
    }

    @Test
    void calculateWhenLedgeBlockedShouldNopath() {
        NavQuery ledge = new GridNavQuery(0, 10, 0, 0, (x, y) -> x < 5);
        PathFinder pf = PathFinder.requiring(ledge);
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 9, 0, 0, false, false);
        assertTrue(r.nopath());
    }

    @Test
    void calculateForceDestWhenNopathShouldShortcut() {
        NavQuery none = new NavQuery() {
            @Override
            public boolean available(int mapId) {
                return true;
            }

            @Override
            public List<Waypoint> findPath(int mapId, float sx, float sy, float sz,
                    float dx, float dy, float dz, boolean straightLine) {
                return List.of();
            }

            @Override
            public List<Waypoint> randomPoint(int mapId, float hx, float hy, float hz, float radius,
                    double angle01, double dist01) {
                return List.of();
            }
        };
        PathFinder pf = PathFinder.requiring(none);
        PathFinder.Result r = pf.calculate(0, 0, 0, 0, 3, 4, 5, true, false);
        assertTrue(r.usable());
        assertEquals(3f, r.points().get(0).x(), 1e-4f);
    }
}
