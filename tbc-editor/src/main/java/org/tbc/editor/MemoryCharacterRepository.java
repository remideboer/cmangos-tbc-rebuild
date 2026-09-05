package org.tbc.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory character list for tests. Same contract as JDBC. */
public final class MemoryCharacterRepository implements CharacterRepository {
    private final Map<Long, CharacterSummary> byGuid = new LinkedHashMap<>();

    @Override
    public List<CharacterSummary> listAll() {
        return new ArrayList<>(byGuid.values());
    }

    @Override
    public List<CharacterSummary> listByAccount(int accountId) {
        List<CharacterSummary> out = new ArrayList<>();
        for (CharacterSummary s : byGuid.values()) {
            if (s.accountId() == accountId) {
                out.add(s);
            }
        }
        return out;
    }

    @Override
    public CharacterSummary find(long guid) {
        return byGuid.get(guid);
    }

    @Override
    public void put(CharacterSummary row) {
        byGuid.put(row.guid(), row);
    }

    @Override
    public void remove(long guid) {
        byGuid.remove(guid);
    }
}
