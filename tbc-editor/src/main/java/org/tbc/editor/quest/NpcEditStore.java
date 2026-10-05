package org.tbc.editor.quest;

import org.tbc.world.content.ObjectMgr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Writes dragged positions and shared template name, display, and gear. Verifies each row. */
public final class NpcEditStore {
    public record Result(int updated, int inserted, int templates) {
        public int spawns() {
            return updated + inserted;
        }
    }

    public interface Sql {
        boolean exists(String sql, int id) throws SQLException;

        void update(String sql, Object... args) throws SQLException;

        float[] floats(String sql, int id) throws SQLException;
    }

    private static final int MYSQL_BAD_FIELD = 1054;
    private static final int SPAWN_MASK = 1;

    private NpcEditStore() {}

    public static Result save(Connection connection, NpcEditSession session,
                              IntFunction<ObjectMgr.Spawn> spawnOf, IntConsumer markDb) throws SQLException {
        if (connection == null) {
            throw new SQLException("No world database.");
        }
        return save(new JdbcSql(connection), session, spawnOf, markDb);
    }

    public static Result save(Sql sql, NpcEditSession session, IntFunction<ObjectMgr.Spawn> spawnOf,
                              IntConsumer markDb) throws SQLException {
        if (sql == null || session == null) {
            return new Result(0, 0, 0);
        }
        IntFunction<ObjectMgr.Spawn> spawns = spawnOf == null ? guid -> null : spawnOf;
        IntConsumer marked = markDb == null ? guid -> {} : markDb;
        int updated = 0;
        int inserted = 0;
        int templates = 0;
        for (NpcEditSession.Pose pose : session.moved()) {
            if (sql.exists("SELECT guid FROM creature WHERE guid = ?", pose.guid())) {
                sql.update("UPDATE creature SET position_x = ?, position_y = ?, position_z = ? WHERE guid = ?",
                        pose.x(), pose.y(), pose.z(), pose.guid());
                verifyPosition(sql, pose);
                updated++;
                continue;
            }
            ObjectMgr.Spawn spawn = spawns.apply(pose.guid());
            if (spawn == null) {
                throw new SQLException("creature guid " + pose.guid()
                        + " is not in the world database and has no in-memory spawn to insert");
            }
            sql.update(
                    "INSERT INTO creature (guid, id, map, spawnMask, position_x, position_y, position_z, orientation, "
                            + "spawntimesecsmin, spawntimesecsmax, spawndist, MovementType) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    pose.guid(), spawn.entry(), spawn.map(), SPAWN_MASK,
                    pose.x(), pose.y(), pose.z(), spawn.o(),
                    spawn.respawnMinSecs(), spawn.respawnMaxSecs(), spawn.spawnDist(), spawn.movementType());
            verifyPosition(sql, pose);
            marked.accept(pose.guid());
            inserted++;
        }
        for (NpcEditSession.Look look : session.changedLooks()) {
            if (!sql.exists("SELECT Entry FROM creature_template WHERE Entry = ?", look.entry())) {
                throw new SQLException(
                        "creature_template entry " + look.entry() + " is not in the world database");
            }
            try {
                sql.update(
                        "UPDATE creature_template SET Name = ?, DisplayId1 = ?, EquipmentTemplateId = ?, CreatureType = ?, Faction = ? WHERE Entry = ?",
                        look.name(), look.displayId(), look.equipmentId(), look.creatureType(), look.faction(),
                        look.entry());
            } catch (SQLException displayColumn) {
                if (displayColumn.getErrorCode() != MYSQL_BAD_FIELD) {
                    throw displayColumn;
                }
                sql.update(
                        "UPDATE creature_template SET Name = ?, ModelId1 = ?, EquipmentTemplateId = ?, CreatureType = ?, Faction = ? WHERE Entry = ?",
                        look.name(), look.displayId(), look.equipmentId(), look.creatureType(), look.faction(),
                        look.entry());
            }
            templates++;
        }
        return new Result(updated, inserted, templates);
    }

    private static void verifyPosition(Sql sql, NpcEditSession.Pose pose) throws SQLException {
        float[] got = sql.floats(
                "SELECT position_x, position_y, position_z FROM creature WHERE guid = ?", pose.guid());
        if (got == null || got.length < 3 || drifted(pose, got)) {
            throw new SQLException("creature guid " + pose.guid() + " did not keep the saved position");
        }
    }

    private static boolean drifted(NpcEditSession.Pose pose, float[] got) {
        return Math.abs(pose.x() - got[0]) > NpcEditSession.POSITION_EPS
                || Math.abs(pose.y() - got[1]) > NpcEditSession.POSITION_EPS
                || Math.abs(pose.z() - got[2]) > NpcEditSession.POSITION_EPS;
    }

    private static final class JdbcSql implements Sql {
        private final Connection connection;

        private JdbcSql(Connection connection) {
            this.connection = connection;
        }

        @Override
        public boolean exists(String sql, int id) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        @Override
        public void update(String sql, Object... args) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bind(ps, args);
                ps.executeUpdate();
            }
        }

        @Override
        public float[] floats(String sql, int id) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new float[] {rs.getFloat(1), rs.getFloat(2), rs.getFloat(3)};
                }
            }
        }

        private static void bind(PreparedStatement ps, Object... args) throws SQLException {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                int idx = i + 1;
                if (arg instanceof Float f) {
                    ps.setFloat(idx, f);
                } else if (arg instanceof Integer n) {
                    ps.setInt(idx, n);
                } else if (arg instanceof String s) {
                    ps.setString(idx, s);
                } else if (arg == null) {
                    ps.setObject(idx, null);
                } else {
                    throw new SQLException("unsupported bind " + arg.getClass().getName());
                }
            }
        }
    }
}
