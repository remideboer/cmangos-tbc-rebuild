package org.tbc.editor;

import java.util.List;

public interface CharacterRepository {
    List<CharacterSummary> listAll();

    List<CharacterSummary> listByAccount(int accountId);

    CharacterSummary find(long guid);

    void put(CharacterSummary row);

    void remove(long guid);
}
