package org.tbc.world.entity;

import org.tbc.common.Codes;

/** ObjectMgr.cpp normalizePlayerName / CheckPlayerName for 8606 character names. */
public final class PlayerNames {
    public static final int MAX_PLAYER_NAME = 12;
    public static final int MAX_INTERNAL_PLAYER_NAME = 15;
    public static final int MIN_PLAYER_NAME = 2;

    private PlayerNames() {}

    /**
     * Empty, or longer than {@link #MAX_INTERNAL_PLAYER_NAME}, fails (C++ returns false → CHAR_NAME_NO_NAME).
     * First character upper, rest lower.
     */
    public static String normalize(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.length() > MAX_INTERNAL_PLAYER_NAME) {
            return null;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        for (int i = 1; i < chars.length; i++) {
            chars[i] = Character.toLowerCase(chars[i]);
        }
        return new String(chars);
    }

    /** Util.h isCyrillicCharacter on the first code point. */
    public static boolean cyrillicFirst(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        int cp = name.codePointAt(0);
        if (cp >= 0x0410 && cp <= 0x044F) {
            return true;
        }
        return cp == 0x0401 || cp == 0x0451;
    }

    public static int check(String name) {
        if (name.length() > MAX_PLAYER_NAME) {
            return Codes.CHAR_NAME_TOO_LONG;
        }
        if (name.length() < MIN_PLAYER_NAME) {
            return Codes.CHAR_NAME_TOO_SHORT;
        }
        return Codes.CHAR_NAME_SUCCESS;
    }

    public static final int MAX_DECLINED_NAME_CASES = 5;

    /**
     * Util.cpp GetMainPartOfName — strip the Cyrillic case ending for {@code declension} 0–5.
     */
    public static String mainPart(String name, int declension) {
        if (name == null || name.isEmpty() || !cyrillicFirst(name) || declension > 5) {
            return name;
        }
        String[] ends = DROP_ENDS[declension];
        for (String end : ends) {
            if (name.endsWith(end)) {
                return name.substring(0, name.length() - end.length());
            }
        }
        return name;
    }

    /** ObjectMgr.cpp CheckDeclinedNames — each case shares the nominative main part. */
    public static boolean checkDeclinedNames(String stored, String[] cases) {
        if (stored == null || cases == null || cases.length != MAX_DECLINED_NAME_CASES) {
            return false;
        }
        String main = mainPart(stored, 0);
        for (int i = 0; i < MAX_DECLINED_NAME_CASES; i++) {
            if (cases[i] == null || !main.equals(mainPart(cases[i], i + 1))) {
                return false;
            }
        }
        return true;
    }

    private static final String[][] DROP_ENDS = {
            {"\u0430", "\u043E", "\u044F", "\u0435", "\u044C", "\u0439"},
            {"\u0430", "\u044F", "\u044B", "\u0438"},
            {"\u0435", "\u0443", "\u044E", "\u0438"},
            {"\u0443", "\u044E", "\u043E", "\u0435", "\u044C", "\u044F", "\u0430"},
            {"\u043E\u0439", "\u0451\u0439", "\u0435\u0439", "\u043E\u043C", "\u0451\u043C", "\u0435\u043C", "\u044E"},
            {"\u0435", "\u0438"}
    };
}
