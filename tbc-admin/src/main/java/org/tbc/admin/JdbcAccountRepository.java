package org.tbc.admin;

import org.tbc.common.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** tbcrealmd.account. Parameterized SQL only. */
public final class JdbcAccountRepository implements AccountRepository {
    private final DbPool db;

    public JdbcAccountRepository(DbPool db) {
        this.db = db;
    }

    @Override
    public List<AccountRow> list() {
        List<AccountRow> out = new ArrayList<>();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, gmlevel, expansion, v, s FROM account ORDER BY id");
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
    public AccountRow findById(int id) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, gmlevel, expansion, v, s FROM account WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public AccountRow findByUsername(String username) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, gmlevel, expansion, v, s FROM account WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public int insert(String username, String vHex, String sHex, int gmlevel, int expansion) {
        try (Connection c = db.get()) {
            int id = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO account (username, v, s, joindate, expansion, gmlevel) VALUES (?, ?, ?, NOW(), ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, vHex);
                ps.setString(3, sHex);
                ps.setInt(4, expansion);
                ps.setInt(5, gmlevel);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        id = keys.getInt(1);
                    }
                }
            }
            if (id == 0) {
                try (PreparedStatement q = c.prepareStatement("SELECT id FROM account WHERE username = ?")) {
                    q.setString(1, username);
                    try (ResultSet rs = q.executeQuery()) {
                        if (rs.next()) {
                            id = rs.getInt(1);
                        }
                    }
                }
            }
            if (id == 0) {
                throw new AccountException("Create failed.");
            }
            fillRealmCharacters(c);
            return id;
        } catch (AccountException e) {
            throw e;
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public void updateGmLevel(int id, int gmlevel) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement("UPDATE account SET gmlevel = ? WHERE id = ?")) {
            ps.setInt(1, gmlevel);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public void updateVerifier(int id, String vHex, String sHex) {
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement("UPDATE account SET v = ?, s = ? WHERE id = ?")) {
            ps.setString(1, vHex);
            ps.setString(2, sHex);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    @Override
    public void delete(int id) {
        try (Connection c = db.get()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM account_banned WHERE account_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM realmcharacters WHERE acctid = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM account WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    private static void fillRealmCharacters(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO realmcharacters (realmid, acctid, numchars) "
                        + "SELECT realmlist.id, account.id, 0 FROM realmlist, account "
                        + "LEFT JOIN realmcharacters ON acctid = account.id WHERE acctid IS NULL")) {
            ps.executeUpdate();
        }
    }

    private static AccountRow row(ResultSet rs) throws SQLException {
        String v = rs.getString("v");
        String s = rs.getString("s");
        return new AccountRow(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getInt("gmlevel"),
                rs.getInt("expansion"),
                v == null ? "" : v,
                s == null ? "" : s);
    }

    private static AccountException wrap(SQLException e) {
        return new AccountException("Database error: " + e.getMessage(), e);
    }
}
