package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.Spawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * SQL load of creature / gameobject spawns (event rows excluded via the join) and the per-event spawn lists.
 * Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class SpawnLoader {
    private static final Logger log = LoggerFactory.getLogger(SpawnLoader.class);
    private final ObjectMgr m;

    private SpawnLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order and failure handling as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        SpawnLoader l = new SpawnLoader(m);
        try {
            l.loadSpawns(c);
        } catch (Exception e) {
            log.warn("creature spawn load failed: {}", e.getMessage());
        }
        try {
            l.loadGoSpawns(c);
        } catch (Exception e) {
            log.warn("gameobject spawn load failed: {}", e.getMessage());
        }
        try {
            l.loadEventCreatures(c);
        } catch (Exception e) {
            log.debug("game_event_creature load skipped: {}", e.getMessage());
        }
        try {
            l.loadEventGameObjects(c);
        } catch (Exception e) {
            log.debug("game_event_gameobject load skipped: {}", e.getMessage());
        }
    }
    /** Creature spawn SELECTs, first match wins. Event rows stay out via the join. */
    static java.util.List<String> creatureSpawnQueries() {
        String cols = "c.guid, c.id, c.map, c.position_x, c.position_y, c.position_z, c.orientation";
        String motionCols = cols + ", c.spawndist, c.MovementType";
        String respawnCols = motionCols + ", c.spawntimesecsmin, c.spawntimesecsmax";
        String join = " FROM creature c LEFT OUTER JOIN game_event_creature gec ON c.guid = gec.guid AND gec.`event` > 0";
        return java.util.List.of(
                "SELECT " + respawnCols + join + " WHERE gec.guid IS NULL",
                "SELECT " + motionCols + join + " WHERE gec.guid IS NULL",
                "SELECT " + cols + join + " WHERE gec.guid IS NULL",
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM creature");
    }

    /** Gameobject spawn SELECTs, first match wins. Event rows stay out via the join. */
    static java.util.List<String> gameObjectSpawnQueries() {
        String cols = "g.guid, g.id, g.map, g.position_x, g.position_y, g.position_z, g.orientation";
        String join = " FROM gameobject g LEFT OUTER JOIN game_event_gameobject geg ON g.guid = geg.guid AND geg.`event` > 0";
        return java.util.List.of(
                "SELECT " + cols + join + " WHERE geg.guid IS NULL",
                "SELECT guid, id, map, position_x, position_y, position_z, orientation FROM gameobject");
    }

    private void loadSpawns(Connection c) throws Exception {
        String[] sqls = creatureSpawnQueries().toArray(String[]::new);
        Exception last = null;
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                boolean motion = sql.contains("spawndist");
                boolean respawn = sql.contains("spawntimesecsmin");
                while (rs.next()) {
                    float spawnDist = motion ? rs.getFloat(8) : 0f;
                    int movementType = motion ? rs.getInt(9) : 0;
                    int respawnMin = respawn ? rs.getInt(10) : Spawn.DEFAULT_RESPAWN_SECS;
                    int respawnMax = respawn ? rs.getInt(11) : Spawn.DEFAULT_RESPAWN_SECS;
                    int guid = rs.getInt(1);
                    m.spawns.add(new Spawn(guid, rs.getInt(2), rs.getInt(3),
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), rs.getFloat(7),
                            spawnDist, movementType, respawnMin, respawnMax));
                    m.dbCreatureGuids.add(guid);
                }
                log.info("loaded {} creature spawns", m.spawns.size());
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void loadGoSpawns(Connection c) throws Exception {
        String[] sqls = gameObjectSpawnQueries().toArray(String[]::new);
        Exception last = null;
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    m.goSpawns.add(new Spawn(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), rs.getFloat(7)));
                }
                log.info("loaded {} gameobject spawns", m.goSpawns.size());
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void loadEventCreatures(Connection c) throws Exception {
        String sql = "SELECT gec.`event`, c.guid, c.id, c.map, c.position_x, c.position_y, c.position_z, c.orientation "
                + "FROM game_event_creature gec INNER JOIN creature c ON c.guid = gec.guid";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt(1);
                if (eventId <= 0) {
                    continue;
                }
                m.eventCreatures.computeIfAbsent(eventId, k -> new ArrayList<>()).add(
                        new Spawn(rs.getInt(2), rs.getInt(3), rs.getInt(4),
                                rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
            }
            log.info("loaded game_event_creature for {} events", m.eventCreatures.size());
        }
    }

    private void loadEventGameObjects(Connection c) throws Exception {
        String sql = "SELECT geg.`event`, g.guid, g.id, g.map, g.position_x, g.position_y, g.position_z, g.orientation "
                + "FROM game_event_gameobject geg INNER JOIN gameobject g ON g.guid = geg.guid";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt(1);
                if (eventId <= 0) {
                    continue;
                }
                m.eventGameObjects.computeIfAbsent(eventId, k -> new ArrayList<>()).add(
                        new Spawn(rs.getInt(2), rs.getInt(3), rs.getInt(4),
                                rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), rs.getFloat(8)));
            }
            log.info("loaded game_event_gameobject for {} events", m.eventGameObjects.size());
        }
    }
}
