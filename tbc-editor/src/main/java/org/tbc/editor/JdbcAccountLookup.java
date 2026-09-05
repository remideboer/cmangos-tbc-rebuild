package org.tbc.editor;

import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** tbcrealmd.account identity only. No v/s/password. */
public final class JdbcAccountLookup implements AccountLookup {
    private static final String LIST = "SELECT id, username, expansion FROM account ORDER BY username";
    private static final String BY_ID = "SELECT id, username, expansion FROM account WHERE id = ?";

    private final DbPool db;

    public JdbcAccountLookup(DbPool db) {
        this.db = db;
    }

    @Override
    public List<AccountRef> list() {
        List<AccountRef> out = new ArrayList<>();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(LIST);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(row(rs));
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
        return out;
    }

    @Override
    public AccountRef findById(int id) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    private static AccountRef row(ResultSet rs) throws SQLException {
        return new AccountRef(rs.getInt(1), rs.getString(2), rs.getInt(3));
    }

    private static EditorException wrap(SQLException e) {
        return new EditorException("Database error: " + e.getMessage(), e);
    }
}
