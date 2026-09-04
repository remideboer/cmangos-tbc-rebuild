package org.tbc.world.ai;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbScriptStoreTest {
    @Test
    void loadWhenDeathCast7164ShouldExposeRow() throws Exception {
        DbScriptStore store = new DbScriptStore();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createDeathTable(st);
            st.execute("""
                    INSERT INTO dbscripts_on_creature_death (
                      id, delay, priority, command, datalong, datalong2, datalong3,
                      buddy_entry, search_radius, data_flags,
                      dataint, dataint2, dataint3, dataint4, datafloat,
                      x, y, z, o, speed, condition_id)
                    VALUES (6, 0, 0, 15, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            store.load(c);
        }
        List<DbScriptStore.Step> rows = store.scriptsFor(DbScriptStore.CREATURE_DEATH, 6);
        assertEquals(1, rows.size());
        assertEquals(DbScriptStore.COMMAND_CAST_SPELL, rows.get(0).command());
        assertEquals(7164, rows.get(0).datalong());
    }

    @Test
    void loadWhenNullConnectionShouldNotThrow() {
        assertDoesNotThrow(() -> new DbScriptStore().load(null));
    }

    @Test
    void loadWhenMissingTableShouldStayEmpty() throws Exception {
        DbScriptStore store = new DbScriptStore();
        try (Connection c = open()) {
            store.load(c);
        }
        assertTrue(store.scriptsFor(DbScriptStore.CREATURE_DEATH, 6).isEmpty());
    }

    private static Connection open() throws Exception {
        return DriverManager.getConnection("jdbc:h2:mem:dbs_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    }

    private static void createDeathTable(Statement st) throws Exception {
        st.execute("""
                CREATE TABLE dbscripts_on_creature_death (
                  id INT, delay INT, priority INT, command INT,
                  datalong INT, datalong2 INT, datalong3 INT,
                  buddy_entry INT, search_radius INT, data_flags INT,
                  dataint INT, dataint2 INT, dataint3 INT, dataint4 INT,
                  datafloat FLOAT, x FLOAT, y FLOAT, z FLOAT, o FLOAT, speed FLOAT,
                  condition_id INT
                )
                """);
    }
}
