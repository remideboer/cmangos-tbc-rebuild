package org.tbc.admin;

import org.tbc.common.Bn;
import org.tbc.common.Srp6;

import java.util.List;
import java.util.Locale;

/** Account CRUD rules. Passwords become SRP6 v/s; never stored as text. */
public final class AccountService {
    public static final int MAX_ACCOUNT_STR = 16;
    public static final int GM_PLAYER = 0;
    public static final int GM_MODERATOR = 1;
    public static final int GM_GAMEMASTER = 2;
    public static final int GM_ADMINISTRATOR = 3;

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public List<AccountRow> list() {
        return repo.list();
    }

    public AccountRow create(String username, String password, String confirm, int gmlevel, int expansion) {
        String name = normalizeUsername(username);
        requirePassword(password, confirm);
        requireGmLevel(gmlevel);
        requireExpansion(expansion);
        if (repo.findByUsername(name) != null) {
            throw new AccountException("Username already exists.");
        }
        VerifierHex v = verifier(name, password);
        int id = repo.insert(name, v.vHex(), v.sHex(), gmlevel, expansion);
        return new AccountRow(id, name, gmlevel, expansion, v.vHex(), v.sHex());
    }

    public AccountRow setRole(int id, int gmlevel) {
        requireGmLevel(gmlevel);
        AccountRow row = requireAccount(id);
        repo.updateGmLevel(id, gmlevel);
        return new AccountRow(row.id(), row.username(), gmlevel, row.expansion(), row.vHex(), row.sHex());
    }

    public AccountRow setPassword(int id, String password, String confirm) {
        AccountRow row = requireAccount(id);
        requirePassword(password, confirm);
        VerifierHex v = verifier(row.username(), password);
        repo.updateVerifier(id, v.vHex(), v.sHex());
        return new AccountRow(row.id(), row.username(), row.gmlevel(), row.expansion(), v.vHex(), v.sHex());
    }

    public void delete(int id) {
        requireAccount(id);
        repo.delete(id);
    }

    public static String roleName(int gmlevel) {
        return switch (gmlevel) {
            case GM_PLAYER -> "Player";
            case GM_MODERATOR -> "Moderator";
            case GM_GAMEMASTER -> "Gamemaster";
            case GM_ADMINISTRATOR -> "Administrator";
            default -> "Unknown";
        };
    }

    static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AccountException("Username is required.");
        }
        String name = username.trim().toUpperCase(Locale.ROOT);
        if (name.length() > MAX_ACCOUNT_STR) {
            throw new AccountException("Username is too long.");
        }
        return name;
    }

    private static void requirePassword(String password, String confirm) {
        if (password == null || password.isEmpty()) {
            throw new AccountException("Password is required.");
        }
        if (password.length() > MAX_ACCOUNT_STR) {
            throw new AccountException("Password is too long.");
        }
        if (confirm == null || !password.equals(confirm)) {
            throw new AccountException("Passwords do not match.");
        }
    }

    private static void requireGmLevel(int gmlevel) {
        if (gmlevel < GM_PLAYER || gmlevel > GM_ADMINISTRATOR) {
            throw new AccountException("Role must be Player through Administrator.");
        }
    }

    private static void requireExpansion(int expansion) {
        if (expansion < 0 || expansion > 1) {
            throw new AccountException("Expansion must be 0 or 1.");
        }
    }

    private AccountRow requireAccount(int id) {
        AccountRow row = repo.findById(id);
        if (row == null) {
            throw new AccountException("Account not found.");
        }
        return row;
    }

    private static VerifierHex verifier(String username, String password) {
        Srp6.Verifier v = Srp6.makeVerifier(username, password);
        return new VerifierHex(Bn.leToBeHex(v.vLe()), Bn.leToBeHex(v.salt()));
    }

    private record VerifierHex(String vHex, String sHex) {}
}
