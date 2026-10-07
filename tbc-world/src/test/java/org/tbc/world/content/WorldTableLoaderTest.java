package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** areatrigger_teleport, game_weather and battlemaster_entry loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class WorldTableLoaderTest {
    @Test
    void loadWhenTablesPresentShouldIndexTriggersWeatherAndBattlemasters() throws Exception {
        String url = "jdbc:h2:mem:world_table_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "world-table-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE areatrigger_teleport (id INT, target_map INT, target_position_x FLOAT, "
                        + "target_position_y FLOAT, target_position_z FLOAT, target_orientation FLOAT)");
                st.execute("INSERT INTO areatrigger_teleport VALUES (2230, 389, 0.797643, -8.23429, -15.5288, 0)");
                st.execute("CREATE TABLE game_weather (zone INT)");
                st.execute("INSERT INTO game_weather VALUES (" + Content.ZONE_ELWYNN + ")");
                st.execute("CREATE TABLE battlemaster_entry (entry INT, bg_template INT)");
                st.execute("INSERT INTO battlemaster_entry VALUES (2302, 2)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                WorldTableLoader.areaTriggers(m, c);
                WorldTableLoader.weatherAndBattlemasters(m, c);
            }
            assertEquals(389, m.areaTriggers.get(2230).map());
            assertEquals(Content.WEATHER_STATE_FINE, m.weather.get(Content.ZONE_ELWYNN).state());
            assertEquals(2, m.battleMasterBgType(2302));
        }
    }

    @Test
    void loadWhenNoTablesShouldLeaveMapsEmpty() throws Exception {
        String url = "jdbc:h2:mem:world_table_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "world-table-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                WorldTableLoader.areaTriggers(m, c);
                WorldTableLoader.weatherAndBattlemasters(m, c);
            }
            assertTrue(m.areaTriggers.isEmpty());
            assertTrue(m.weather.isEmpty());
            assertEquals(0, m.battleMasterBgType(2302));
        }
    }
}
