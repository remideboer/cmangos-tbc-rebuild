package org.tbc.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory accounts for tests. Same contract as JDBC. */
public final class MemoryAccountRepository implements AccountRepository {
    private final Map<Integer, AccountRow> byId = new LinkedHashMap<>();
    private final Map<String, Integer> byName = new HashMap<>();
    private int nextId = 1;

    @Override
    public List<AccountRow> list() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public AccountRow findById(int id) {
        return byId.get(id);
    }

    @Override
    public AccountRow findByUsername(String username) {
        Integer id = byName.get(username);
        return id == null ? null : byId.get(id);
    }

    @Override
    public int insert(String username, String vHex, String sHex, int gmlevel, int expansion) {
        int id = nextId++;
        AccountRow row = new AccountRow(id, username, gmlevel, expansion, vHex, sHex);
        byId.put(id, row);
        byName.put(username, id);
        return id;
    }

    @Override
    public void updateGmLevel(int id, int gmlevel) {
        AccountRow r = byId.get(id);
        byId.put(id, new AccountRow(r.id(), r.username(), gmlevel, r.expansion(), r.vHex(), r.sHex()));
    }

    @Override
    public void updateVerifier(int id, String vHex, String sHex) {
        AccountRow r = byId.get(id);
        byId.put(id, new AccountRow(r.id(), r.username(), r.gmlevel(), r.expansion(), vHex, sHex));
    }

    @Override
    public void delete(int id) {
        AccountRow r = byId.remove(id);
        if (r != null) {
            byName.remove(r.username());
        }
    }
}
