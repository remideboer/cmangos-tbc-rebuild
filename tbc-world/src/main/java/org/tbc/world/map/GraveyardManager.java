package org.tbc.world.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.DbPool;
import org.tbc.world.session.DeathHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Closest spirit healer. CMaNGOS GraveyardManager + world_safe_locs / game_graveyard_zone.
 */
public final class GraveyardManager {
    private static final Logger log = LoggerFactory.getLogger(GraveyardManager.class);
    public static final int TEAM_BOTH = 0;
    public static final int HORDE = 67;
    public static final int ALLIANCE = 469;
    public static final int AREALINK = 0;
    public static final int MAPLINK = 1;
    public static final int DEFAULT_ALLIANCE = 4;
    public static final int DEFAULT_HORDE = 10;

    public record Loc(int id, int map, float x, float y, float z, float o) {}

    private record Link(int locId, int team) {}

    private final Map<Integer, Loc> locs = new HashMap<>();
    private final Map<Integer, List<Link>> links = new HashMap<>();

    public static GraveyardManager seeded() {
        GraveyardManager g = new GraveyardManager();
        g.addLoc(new Loc(DEFAULT_ALLIANCE, DeathHandler.GY_ELWYNN_MAP, DeathHandler.GY_ELWYNN_X,
                DeathHandler.GY_ELWYNN_Y, DeathHandler.GY_ELWYNN_Z, 0f));
        g.addLoc(new Loc(DEFAULT_HORDE, 1, -618.518f, -4251.67f, 38.718f, 0f));
        g.addLink(DEFAULT_ALLIANCE, 0, MAPLINK, ALLIANCE);
        g.addLink(DEFAULT_HORDE, 1, MAPLINK, HORDE);
        g.addLoc(new Loc(100, 0, -6220f, 330f, 383f, 0f));
        g.addLink(100, 0, MAPLINK, ALLIANCE);
        return g;
    }

    public void addLoc(Loc loc) {
        locs.put(loc.id, loc);
    }

    public void addLink(int locId, int ghostLoc, int linkKind, int team) {
        links.computeIfAbsent(key(ghostLoc, linkKind), k -> new ArrayList<>()).add(new Link(locId, team));
    }

    public void load(DbPool world) {
        if (world == null) {
            return;
        }
        try (Connection c = world.get()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, map, x, y, z, o FROM world_safe_locs");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addLoc(new Loc(rs.getInt(1), rs.getInt(2), rs.getFloat(3), rs.getFloat(4), rs.getFloat(5),
                            rs.getFloat(6)));
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, ghost_loc, link_kind, faction FROM game_graveyard_zone");
                    ResultSet rs = ps.executeQuery()) {
                Map<Integer, List<Link>> loaded = new HashMap<>();
                while (rs.next()) {
                    int locId = rs.getInt(1);
                    int ghostLoc = rs.getInt(2);
                    int kind = rs.getInt(3);
                    int team = rs.getInt(4);
                    loaded.computeIfAbsent(key(ghostLoc, kind), k -> new ArrayList<>())
                            .add(new Link(locId, team));
                }
                if (!loaded.isEmpty()) {
                    links.clear();
                    links.putAll(loaded);
                }
            }
        } catch (Exception e) {
            log.warn("graveyard load failed: {}", e.getMessage());
        }
    }

    public Loc closest(int mapId, float x, float y, float z, int team, int areaId) {
        Loc found = null;
        if (areaId != 0) {
            found = closestIn(key(areaId, AREALINK), x, y, z, mapId, team);
        }
        if (found == null) {
            found = closestIn(key(mapId, MAPLINK), x, y, z, mapId, team);
        }
        if (found == null) {
            found = locs.get(team == HORDE ? DEFAULT_HORDE : DEFAULT_ALLIANCE);
        }
        return found;
    }

    private Loc closestIn(int linkKey, float x, float y, float z, int mapId, int team) {
        List<Link> list = links.get(linkKey);
        if (list == null) {
            return null;
        }
        Loc near = null;
        double nearDist = Double.MAX_VALUE;
        Loc far = null;
        for (Link link : list) {
            if (link.team != TEAM_BOTH && link.team != team && team != TEAM_BOTH) {
                continue;
            }
            Loc entry = locs.get(link.locId);
            if (entry == null) {
                continue;
            }
            if (entry.map != mapId) {
                if (far == null) {
                    far = entry;
                }
                continue;
            }
            double d = dist2(entry.x - x, entry.y - y, entry.z - z);
            if (d < nearDist) {
                nearDist = d;
                near = entry;
            }
        }
        return near != null ? near : far;
    }

    private static double dist2(float dx, float dy, float dz) {
        return dx * dx + dy * dy + dz * dz;
    }

    private static int key(int locationId, int linkKind) {
        return locationId | (linkKind << 31);
    }
}
