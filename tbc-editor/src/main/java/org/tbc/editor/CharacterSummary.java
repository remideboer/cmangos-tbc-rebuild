package org.tbc.editor;

/** List row. Not a live Player graph. */
public record CharacterSummary(
        long guid,
        int accountId,
        String accountName,
        String name,
        int race,
        int clazz,
        int gender,
        int level,
        int map,
        boolean online) {}
