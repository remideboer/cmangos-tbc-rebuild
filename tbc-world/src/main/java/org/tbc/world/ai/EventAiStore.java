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

/** creature_ai_scripts. spec/05-domain/scripting-plugin-contract.md */
public final class EventAiStore {
    private static final Logger log = LoggerFactory.getLogger(EventAiStore.class);
    private static final String SELECT = "SELECT id, creature_id, event_type, event_inverse_phase_mask, event_chance, event_flags, "
            + "event_param1, event_param2, event_param3, event_param4, event_param5, event_param6, "
            + "action1_type, action1_param1, action1_param2, action1_param3, "
            + "action2_type, action2_param1, action2_param2, action2_param3, "
            + "action3_type, action3_param1, action3_param2, action3_param3 "
            + "FROM creature_ai_scripts ORDER BY id";

    private final Map<Integer, List<EventAi.Script>> byEntry = new HashMap<>();
    private final Map<Integer, List<EventAi.Script>> byGuid = new HashMap<>();

    public void load(Connection conn) {
        byEntry.clear();
        byGuid.clear();
        if (conn == null) {
            log.warn("creature_ai_scripts skipped: no connection");
            return;
        }
        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(SELECT); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int flags = rs.getInt(6);
                if ((flags & EventAi.EFLAG_DEBUG_ONLY) != 0) {
                    continue;
                }
                EventAi.Script script = new EventAi.Script(
                        rs.getInt(3), rs.getInt(4), rs.getInt(5), flags,
                        rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12),
                        mapAction(rs.getInt(13), rs.getInt(14), rs.getInt(15), rs.getInt(16)),
                        mapAction(rs.getInt(17), rs.getInt(18), rs.getInt(19), rs.getInt(20)),
                        mapAction(rs.getInt(21), rs.getInt(22), rs.getInt(23), rs.getInt(24)));
                int keyField = rs.getInt(2);
                if (keyField > 0) {
                    byEntry.computeIfAbsent(keyField, k -> new ArrayList<>()).add(script);
                } else {
                    byGuid.computeIfAbsent(-keyField, k -> new ArrayList<>()).add(script);
                }
                n++;
            }
        } catch (SQLException e) {
            byEntry.clear();
            byGuid.clear();
            n = 0;
            log.warn("creature_ai_scripts load failed: {}", e.getMessage());
        }
        log.info("loaded {} CreatureEventAI scripts", n);
    }

    public List<EventAi.Script> scriptsFor(int entry, int spawnGuid) {
        if (spawnGuid > 0) {
            List<EventAi.Script> guidRows = byGuid.get(spawnGuid);
            if (guidRows != null && !guidRows.isEmpty()) {
                return List.copyOf(guidRows);
            }
        }
        List<EventAi.Script> entryRows = byEntry.get(entry);
        if (entryRows == null || entryRows.isEmpty()) {
            return List.of();
        }
        return List.copyOf(entryRows);
    }

    private static EventAi.Action mapAction(int type, int p1, int p2, int p3) {
        return switch (type) {
            case EventAi.ACTION_NONE,
                    EventAi.ACTION_SET_FACTION,
                    EventAi.ACTION_CAST,
                    EventAi.ACTION_THREAT_SINGLE,
                    EventAi.ACTION_THREAT_ALL_PCT,
                    EventAi.ACTION_SET_UNIT_FLAG,
                    EventAi.ACTION_REMOVE_UNIT_FLAG,
                    EventAi.ACTION_COMBAT_MOVEMENT,
                    EventAi.ACTION_SET_PHASE,
                    EventAi.ACTION_INC_PHASE,
                    EventAi.ACTION_EVADE,
                    EventAi.ACTION_RANDOM_PHASE,
                    EventAi.ACTION_RANDOM_PHASE_RANGE,
                    EventAi.ACTION_DIE -> new EventAi.Action(type, p1, p2, p3);
            default -> EventAi.Action.none();
        };
    }
}
