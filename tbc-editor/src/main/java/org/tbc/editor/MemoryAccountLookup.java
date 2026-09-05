package org.tbc.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory login lookup for tests. No passwords. */
public final class MemoryAccountLookup implements AccountLookup {
    private final Map<Integer, AccountRef> byId = new LinkedHashMap<>();

    public void put(AccountRef account) {
        byId.put(account.id(), account);
    }

    @Override
    public List<AccountRef> list() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public AccountRef findById(int id) {
        return byId.get(id);
    }
}
