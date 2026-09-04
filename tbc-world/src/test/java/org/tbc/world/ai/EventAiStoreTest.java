package org.tbc.world.ai;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventAiStoreTest {
    @Test
    void loadWhen10301RowShouldExposeAggroCast7164() throws Exception {
        EventAiStore store = new EventAiStore();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createTable(st);
            st.execute("""
                    INSERT INTO creature_ai_scripts (
                      id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags,
                      event_param1, event_param2, event_param3, event_param4, event_param5, event_param6,
                      action1_type, action1_param1, action1_param2, action1_param3,
                      action2_type, action2_param1, action2_param2, action2_param3,
                      action3_type, action3_param1, action3_param2, action3_param3)
                    VALUES (10301, 103, 4, 0, 100, 0, 0, 0, 0, 0, 0, 0, 11, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            store.load(c);
        }
        List<EventAi.Script> rows = store.scriptsFor(103, 0);
        assertEquals(1, rows.size());
        EventAi ai = new EventAi();
        ai.load(rows);
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164), casts);
    }

    @Test
    void loadWhenNullConnectionShouldNotThrow() {
        assertDoesNotThrow(() -> new EventAiStore().load(null));
    }

    @Test
    void spawnWhenStoreHas10301ShouldCast7164OnAggro() throws Exception {
        ObjectMgr mgr = new ObjectMgr();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createTable(st);
            st.execute("""
                    INSERT INTO creature_ai_scripts (
                      id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags,
                      event_param1, event_param2, event_param3, event_param4, event_param5, event_param6,
                      action1_type, action1_param1, action1_param2, action1_param3,
                      action2_type, action2_param1, action2_param2, action2_param3,
                      action3_type, action3_param1, action3_param2, action3_param3)
                    VALUES (10301, 103, 4, 0, 100, 0, 0, 0, 0, 0, 0, 0, 11, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            mgr.eventAiStore.load(c);
        }
        Creature c = mgr.spawnCreature(103, 0, 0, 0, 0, 0, null);
        List<Integer> casts = new ArrayList<>();
        c.eventAi.onAggro(c, player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164), casts);
        assertTrue(mgr.eventAiStore.scriptsFor(103, 0).size() > 0);
    }

    @Test
    void spawnWhenStoreEmptyAndEntry103ShouldKeepSeed() {
        ObjectMgr mgr = new ObjectMgr();
        Creature c = mgr.spawnCreature(103, 0, 0, 0, 0, 0, null);
        List<Integer> casts = new ArrayList<>();
        c.eventAi.onAggro(c, player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164), casts);
    }

    @Test
    void loadWhenGuidOverrideShouldWinOverEntry() throws Exception {
        EventAiStore store = new EventAiStore();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createTable(st);
            st.execute("""
                    INSERT INTO creature_ai_scripts (
                      id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags,
                      event_param1, event_param2, event_param3, event_param4, event_param5, event_param6,
                      action1_type, action1_param1, action1_param2, action1_param3,
                      action2_type, action2_param1, action2_param2, action2_param3,
                      action3_type, action3_param1, action3_param2, action3_param3)
                    VALUES (10301, 103, 4, 0, 100, 0, 0, 0, 0, 0, 0, 0, 11, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                           (50, -50, 4, 0, 100, 0, 0, 0, 0, 0, 0, 0, 11, 133, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            store.load(c);
        }
        EventAi guidAi = new EventAi();
        guidAi.load(store.scriptsFor(103, 50));
        List<Integer> casts = new ArrayList<>();
        guidAi.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(133), casts);
    }

    @Test
    void loadWhenMissingTableShouldStayEmpty() throws Exception {
        EventAiStore store = new EventAiStore();
        try (Connection c = open()) {
            store.load(c);
        }
        assertTrue(store.scriptsFor(103, 0).isEmpty());
    }

    @Test
    void loadWhenDebugOnlyShouldSkip() throws Exception {
        EventAiStore store = new EventAiStore();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createTable(st);
            st.execute("""
                    INSERT INTO creature_ai_scripts (
                      id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags,
                      event_param1, event_param2, event_param3, event_param4, event_param5, event_param6,
                      action1_type, action1_param1, action1_param2, action1_param3,
                      action2_type, action2_param1, action2_param2, action2_param3,
                      action3_type, action3_param1, action3_param2, action3_param3)
                    VALUES (1, 103, 4, 0, 100, 128, 0, 0, 0, 0, 0, 0, 11, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            store.load(c);
        }
        assertTrue(store.scriptsFor(103, 0).isEmpty());
    }

    @Test
    void loadWhenUnknownActionShouldBeNone() throws Exception {
        EventAiStore store = new EventAiStore();
        try (Connection c = open(); Statement st = c.createStatement()) {
            createTable(st);
            st.execute("""
                    INSERT INTO creature_ai_scripts (
                      id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags,
                      event_param1, event_param2, event_param3, event_param4, event_param5, event_param6,
                      action1_type, action1_param1, action1_param2, action1_param3,
                      action2_type, action2_param1, action2_param2, action2_param3,
                      action3_type, action3_param1, action3_param2, action3_param3)
                    VALUES (1, 103, 4, 0, 100, 0, 0, 0, 0, 0, 0, 0, 99, 7164, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    """);
            store.load(c);
        }
        EventAi ai = new EventAi();
        ai.load(store.scriptsFor(103, 0));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    private static Connection open() throws Exception {
        return DriverManager.getConnection("jdbc:h2:mem:eai_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    }

    private static void createTable(Statement st) throws Exception {
        st.execute("""
                CREATE TABLE creature_ai_scripts (
                  id INT, creature_id INT, event_type INT, event_inverse_phase_mask INT,
                  event_chance INT, event_flags INT,
                  event_param1 INT, event_param2 INT, event_param3 INT,
                  event_param4 INT, event_param5 INT, event_param6 INT,
                  action1_type INT, action1_param1 INT, action1_param2 INT, action1_param3 INT,
                  action2_type INT, action2_param1 INT, action2_param2 INT, action2_param3 INT,
                  action3_type INT, action3_param1 INT, action3_param2 INT, action3_param3 INT
                )
                """);
    }

    private static Creature creature() {
        Creature c = new Creature();
        c.guid = 2;
        c.applyTemplate(103, "Garrick Padfoot", 1, 7, 100, 1);
        return c;
    }

    private static Player player() {
        Player p = new Player();
        p.guid = 1;
        return p;
    }
}
