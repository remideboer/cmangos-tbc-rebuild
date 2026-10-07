package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** creature / gameobject spawns and event spawns loaded outside ObjectMgr (refactoring plan cycle 4.2). */
class SpawnLoaderTest {
    @Test
    void spawnQueriesShouldCoverAllMapsWithoutLimit() {
        for (String sql : SpawnLoader.creatureSpawnQueries()) {
            assertFalse(sql.contains("map IN (0, 1)"), sql);
            assertFalse(sql.toUpperCase().contains("LIMIT"), sql);
        }
        for (String sql : SpawnLoader.gameObjectSpawnQueries()) {
            assertFalse(sql.toUpperCase().contains("LIMIT"), sql);
        }
    }

    @Test
    void loadWhenPlainTablesShouldFallBackAndKeepEventRowsOutOfTheWorldList() throws Exception {
        String url = "jdbc:h2:mem:spawn_loader_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "spawn-loader-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("CREATE TABLE creature (guid INT, id INT, map INT, position_x FLOAT, position_y FLOAT, "
                        + "position_z FLOAT, orientation FLOAT)");
                st.execute("INSERT INTO creature VALUES (11, 6, 0, -8900, -100, 80, 0)");
                st.execute("INSERT INTO creature VALUES (12, 15274, 530, 10349.6, -6357.29, 33.4, 0)");
                st.execute("CREATE TABLE game_event_creature (guid INT, event INT)");
                st.execute("INSERT INTO game_event_creature VALUES (12, 1)");
                st.execute("CREATE TABLE gameobject (guid INT, id INT, map INT, position_x FLOAT, position_y FLOAT, "
                        + "position_z FLOAT, orientation FLOAT)");
                st.execute("INSERT INTO gameobject VALUES (21, 181582, 530, 10350, -6358, 33.4, 0)");
            }
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                SpawnLoader.load(m, c);
            }
            assertEquals(1, m.spawns.size(), "event-bound creature row is excluded from the base spawn list");
            assertEquals(6, m.spawns.get(0).entry());
            assertEquals(ObjectMgr.Spawn.DEFAULT_RESPAWN_SECS, m.spawns.get(0).respawnMinSecs());
            assertTrue(m.dbCreature(11));
            assertFalse(m.dbCreature(12));
            assertEquals(1, m.goSpawns.size());
            assertEquals(181582, m.goSpawns.get(0).entry());
            assertEquals(15274, m.eventCreatures.get(1).get(0).entry());
            assertTrue(m.eventGameObjects.isEmpty());
        }
    }

    @Test
    void loadWhenNoSpawnTablesShouldLeaveListsEmpty() throws Exception {
        String url = "jdbc:h2:mem:spawn_loader_empty_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "spawn-loader-empty-test")) {
            ObjectMgr m = new ObjectMgr();
            try (Connection c = worldDb.get()) {
                SpawnLoader.load(m, c);
            }
            assertTrue(m.spawns.isEmpty());
            assertTrue(m.goSpawns.isEmpty());
            assertTrue(m.eventCreatures.isEmpty());
        }
    }
}
