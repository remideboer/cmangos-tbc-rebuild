package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.AreaTrigger;
import org.tbc.world.content.ObjectMgr.ZoneWeather;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * SQL load of the small world tables: areatrigger_teleport, game_weather and battlemaster_entry.
 * Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class WorldTableLoader {
    private static final Logger log = LoggerFactory.getLogger(WorldTableLoader.class);
    private final ObjectMgr m;

    private WorldTableLoader(ObjectMgr m) {
        this.m = m;
    }

    /** areatrigger_teleport; a missing table is ignored. */
    static void areaTriggers(ObjectMgr m, Connection c) {
        new WorldTableLoader(m).loadAreaTriggers(c);
    }

    /** game_weather then battlemaster_entry, same failure handling as ObjectMgr.load. */
    static void weatherAndBattlemasters(ObjectMgr m, Connection c) {
        WorldTableLoader l = new WorldTableLoader(m);
        try {
            l.loadWeather(c);
        } catch (Exception e) {
            log.debug("game_weather load skipped: {}", e.getMessage());
        }
        try {
            l.loadBattleMasters(c);
        } catch (Exception e) {
            log.debug("battlemaster_entry load skipped: {}", e.getMessage());
        }
    }
    private void loadAreaTriggers(Connection c) {
        try {
            PreparedStatement ps = c.prepareStatement(
                    "SELECT id, target_map, target_position_x, target_position_y, target_position_z, target_orientation FROM areatrigger_teleport");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                m.areaTriggers.put(rs.getInt(1), new AreaTrigger(rs.getInt(1), rs.getInt(2),
                        rs.getFloat(3), rs.getFloat(4), rs.getFloat(5), rs.getFloat(6)));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadWeather(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT zone FROM game_weather");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int zone = rs.getInt(1);
                m.weather.put(zone, new ZoneWeather(zone, Content.WEATHER_STATE_FINE, 0f));
            }
        }
    }

    /** BattleGroundMgr::LoadBattleMastersEntry — entry → BattleGroundTypeId. */
    private void loadBattleMasters(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT entry, bg_template FROM battlemaster_entry");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.battleMasterBg.put(rs.getInt(1), rs.getInt(2));
            }
        }
    }
}
