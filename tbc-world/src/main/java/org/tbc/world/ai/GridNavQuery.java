package org.tbc.world.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Injectable 2D occupancy grid for PathFinder tests (wall, ledge, disconnect).
 * Cell size 1 yard; blocked cells fail {@code walkable}.
 */
public final class GridNavQuery implements NavQuery {
    private final BiPredicate<Integer, Integer> walkable;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;

    public GridNavQuery(int minX, int maxX, int minY, int maxY, BiPredicate<Integer, Integer> walkable) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.walkable = walkable;
    }

    /** Vertical wall at x=wallX, y in [y0,y1] inclusive. */
    public static GridNavQuery wallAtX(int wallX, int y0, int y1, int min, int max) {
        return new GridNavQuery(min, max, min, max, (x, y) -> {
            if (x == wallX && y >= y0 && y <= y1) {
                return false;
            }
            return true;
        });
    }

    @Override
    public boolean available(int mapId) {
        return true;
    }

    @Override
    public List<Waypoint> findPath(int mapId, float sx, float sy, float sz,
            float dx, float dy, float dz, boolean straightLine) {
        int x0 = Math.round(sx);
        int y0 = Math.round(sy);
        int x1 = Math.round(dx);
        int y1 = Math.round(dy);
        if (straightLine && clearLine(x0, y0, x1, y1)) {
            return List.of(new Waypoint(dx, dy, dz));
        }
        List<int[]> cells = astar(x0, y0, x1, y1);
        if (cells.isEmpty()) {
            return List.of();
        }
        List<Waypoint> out = new ArrayList<>();
        for (int[] c : cells) {
            out.add(new Waypoint(c[0], c[1], dz));
        }
        if (out.isEmpty() || out.get(out.size() - 1).x() != dx || out.get(out.size() - 1).y() != dy) {
            out.add(new Waypoint(dx, dy, dz));
        }
        return out;
    }

    @Override
    public List<Waypoint> randomPoint(int mapId, float hx, float hy, float hz, float radius,
            double angle01, double dist01) {
        float x = hx + (float) (Math.cos(angle01 * Math.PI * 2) * dist01 * radius);
        float y = hy + (float) (Math.sin(angle01 * Math.PI * 2) * dist01 * radius);
        return findPath(mapId, hx, hy, hz, x, y, hz, false);
    }

    private boolean clearLine(int x0, int y0, int x1, int y1) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) {
            return walk(x0, y0);
        }
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            if (!walk(x, y)) {
                return false;
            }
        }
        return true;
    }

    private List<int[]> astar(int sx, int sy, int ex, int ey) {
        if (!walk(sx, sy) || !walk(ex, ey)) {
            return List.of();
        }
        record Node(int x, int y) {
        }
        PriorityQueue<int[]> open = new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));
        Map<Node, Node> came = new HashMap<>();
        Map<Node, Integer> g = new HashMap<>();
        Node start = new Node(sx, sy);
        g.put(start, 0);
        open.add(new int[]{sx, sy, Math.abs(ex - sx) + Math.abs(ey - sy)});
        Set<Node> closed = new HashSet<>();
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!open.isEmpty()) {
            int[] cur = open.poll();
            Node n = new Node(cur[0], cur[1]);
            if (!closed.add(n)) {
                continue;
            }
            if (n.x == ex && n.y == ey) {
                List<int[]> rev = new ArrayList<>();
                Node p = n;
                while (p != null && !(p.x == sx && p.y == sy)) {
                    rev.add(new int[]{p.x, p.y});
                    p = came.get(p);
                }
                List<int[]> out = new ArrayList<>();
                for (int i = rev.size() - 1; i >= 0; i--) {
                    out.add(rev.get(i));
                }
                return out;
            }
            int gc = g.getOrDefault(n, Integer.MAX_VALUE);
            for (int[] d : d4) {
                int nx = n.x + d[0];
                int ny = n.y + d[1];
                if (!walk(nx, ny)) {
                    continue;
                }
                Node nn = new Node(nx, ny);
                int ng = gc + 1;
                if (ng < g.getOrDefault(nn, Integer.MAX_VALUE)) {
                    g.put(nn, ng);
                    came.put(nn, n);
                    open.add(new int[]{nx, ny, ng + Math.abs(ex - nx) + Math.abs(ey - ny)});
                }
            }
        }
        return List.of();
    }

    private boolean walk(int x, int y) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            return false;
        }
        return walkable.test(x, y);
    }
}
