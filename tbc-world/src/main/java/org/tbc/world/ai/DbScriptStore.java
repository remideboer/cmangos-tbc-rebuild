package org.tbc.world.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** dbscripts_on_*. spec/05-domain/scripting-plugin-contract.md */
public final class DbScriptStore {
    private static final Logger log = LoggerFactory.getLogger(DbScriptStore.class);

    public static final int QUEST_END = 0;
    public static final int QUEST_START = 1;
    public static final int SPELL = 2;
    public static final int GAMEOBJECT = 3;
    public static final int GAMEOBJECT_TEMPLATE = 4;
    public static final int EVENT = 5;
    public static final int GOSSIP = 6;
    public static final int CREATURE_DEATH = 7;
    public static final int CREATURE_MOVEMENT = 8;
    public static final int RELAY = 9;

    public static final int COMMAND_CAST_SPELL = 15;
    public static final int COMMAND_TERMINATE_SCRIPT = 31;

    static final String[] TABLES = {
            "dbscripts_on_quest_end",
            "dbscripts_on_quest_start",
            "dbscripts_on_spell",
            "dbscripts_on_go_use",
            "dbscripts_on_go_template_use",
            "dbscripts_on_event",
            "dbscripts_on_gossip",
            "dbscripts_on_creature_death",
            "dbscripts_on_creature_movement",
            "dbscripts_on_relay"
    };

    private static final String SELECT = "SELECT id, delay, command, datalong, datalong2, datalong3, buddy_entry, "
            + "search_radius, data_flags, dataint, dataint2, dataint3, dataint4, datafloat, x, y, z, o, speed, "
            + "condition_id FROM %s ORDER BY priority";

    public record Step(int type, int id, int delay, int command, int datalong, int datalong2, int datalong3,
                       int buddyEntry, int searchRadius, int dataFlags, int dataint, int dataint2, int dataint3,
                       int dataint4, float datafloat, float x, float y, float z, float o, float speed, int conditionId) {
    }

    private final Map<Long, List<Step>> byTypeId = new HashMap<>();

    public void load(Connection conn) {
        byTypeId.clear();
        if (conn == null) {
            log.warn("dbscripts_on_* skipped: no connection");
            return;
        }
        for (int type = 0; type < TABLES.length; type++) {
            loadTable(conn, type);
        }
    }

    public void add(Step step) {
        byTypeId.computeIfAbsent(key(step.type(), step.id()), k -> new ArrayList<>()).add(step);
    }

    public List<Step> scriptsFor(int type, int id) {
        List<Step> rows = byTypeId.get(key(type, id));
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rows);
    }

    public static Step castSpell(int type, int id, int delay, int spellId) {
        return new Step(type, id, delay, COMMAND_CAST_SPELL, spellId, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0f, 0, 0, 0, 0, 0f, 0);
    }

    public static Step terminate(int type, int id, int delay) {
        return new Step(type, id, delay, COMMAND_TERMINATE_SCRIPT, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0f, 0, 0, 0, 0, 0f, 0);
    }

    private void loadTable(Connection conn, int type) {
        String table = TABLES[type];
        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(SELECT.formatted(table)); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                add(new Step(type, rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),
                        rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12),
                        rs.getInt(13), rs.getFloat(14), rs.getFloat(15), rs.getFloat(16), rs.getFloat(17),
                        rs.getFloat(18), rs.getFloat(19), rs.getInt(20)));
                n++;
            }
        } catch (SQLException e) {
            log.warn("{} load failed: {}", table, e.getMessage());
            return;
        }
        log.info("loaded {} script definitions from table {}", n, table);
    }

    private static long key(int type, int id) {
        return ((long) type << 32) | (id & 0xFFFFFFFFL);
    }
}
