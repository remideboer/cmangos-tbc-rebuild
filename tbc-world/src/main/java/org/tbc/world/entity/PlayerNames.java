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
}
