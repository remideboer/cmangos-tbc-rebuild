package org.tbc.world.entity;

import org.tbc.common.Codes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNamesTest {
    @Test
    void cyrillicFirstWhenLatinShouldBeFalse() {
        assertFalse(PlayerNames.cyrillicFirst("Latinone"));
        assertFalse(PlayerNames.cyrillicFirst(""));
        assertFalse(PlayerNames.cyrillicFirst(null));
    }

    @Test
    void cyrillicFirstWhenRussianAShouldBeTrue() {
        assertTrue(PlayerNames.cyrillicFirst("Андрей"));
        assertTrue(PlayerNames.cyrillicFirst("Ёж"));
    }

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
