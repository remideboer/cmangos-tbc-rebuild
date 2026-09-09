package org.tbc.world.entity;

import org.tbc.common.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerNamesTest {
    @Test
    void normalizeWhenLowercaseShouldCapitalizeFirst() {
        assertEquals("Newname", PlayerNames.normalize("newname"));
    }

    @Test
    void normalizeWhenEmptyShouldReturnNull() {
        assertNull(PlayerNames.normalize(""));
        assertNull(PlayerNames.normalize(null));
    }

    @Test
    void normalizeWhenLongerThanInternalMaxShouldReturnNull() {
        assertNull(PlayerNames.normalize("abcdefghijklmnop"));
    }

    @Test
    void checkWhenOneLetterShouldTooShort() {
        assertEquals(Codes.CHAR_NAME_TOO_SHORT, PlayerNames.check("A"));
    }

    @Test
    void checkWhenThirteenLettersShouldTooLong() {
        assertEquals(Codes.CHAR_NAME_TOO_LONG, PlayerNames.check("Thirteenchars"));
    }
}
