package org.tbc.editor;

import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** tbccharacters.characters list/search. Create/load/save/delete stay on CharacterStore. */
public final class JdbcCharacterRepository implements CharacterRepository {
    private static final String[] ALL = {
            "SELECT guid, account, name, race, `class`, gender, level, map, online FROM characters WHERE deleteDate IS NULL ORDER BY name",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters WHERE deleteDate IS NULL ORDER BY name",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters ORDER BY name"
    };
    private static final String[] BY_ACCOUNT = {
            "SELECT guid, account, name, race, `class`, gender, level, map, online FROM characters WHERE deleteDate IS NULL AND account = ? ORDER BY name",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters WHERE deleteDate IS NULL AND account = ? ORDER BY name",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters WHERE account = ? ORDER BY name"
    };
    private static final String[] BY_GUID = {
            "SELECT guid, account, name, race, `class`, gender, level, map, online FROM characters WHERE deleteDate IS NULL AND guid = ?",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters WHERE deleteDate IS NULL AND guid = ?",
            "SELECT guid, account, name, race, `class`, gender, level, map FROM characters WHERE guid = ?"
    };

    private final DbPool db;

    public JdbcCharacterRepository(DbPool db) {
        this.db = db;
    }

    @Override
    public List<CharacterSummary> listAll() {
        return query(ALL, null);
    }

    @Override
    public List<CharacterSummary> listByAccount(int accountId) {
        return query(BY_ACCOUNT, accountId);
    }

    @Override
    public CharacterSummary find(long guid) {
        List<CharacterSummary> rows = query(BY_GUID, guid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public void put(CharacterSummary row) {
        // CharacterStore owns INSERT/UPDATE of characters + children.
    }

    @Override
    public void remove(long guid) {
        // CharacterStore.delete owns the row.
    }

    private List<CharacterSummary> query(String[] sqls, Number bind) {
        SQLException last = null;
        for (int i = 0; i < sqls.length; i++) {
            boolean hasOnline = sqls[i].contains("online");
            try (Connection c = db.get();
                 PreparedStatement ps = c.prepareStatement(sqls[i])) {
                if (bind != null) {
                    if (bind instanceof Long l) {
                        ps.setLong(1, l);
                    } else {
                        ps.setInt(1, bind.intValue());
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<CharacterSummary> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(row(rs, hasOnline));
                    }
                    return out;
                }
            } catch (SQLException e) {
                last = e;
            }
        }
        throw wrap(last == null ? new SQLException("character list failed") : last);
    }

    private static CharacterSummary row(ResultSet rs, boolean hasOnline) throws SQLException {
        boolean online = hasOnline && rs.getInt(9) != 0;
        return new CharacterSummary(
                rs.getLong(1),
                rs.getInt(2),
                "",
                rs.getString(3),
                rs.getInt(4),
                rs.getInt(5),
                rs.getInt(6),
                rs.getInt(7),
                rs.getInt(8),
                online);
    }

    private static EditorException wrap(SQLException e) {
        return new EditorException("Database error: " + e.getMessage(), e);
    }
}
