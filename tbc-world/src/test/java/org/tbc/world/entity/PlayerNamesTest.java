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

    @Test
    void checkDeclinedNamesWhenIvanCasesMatchShouldPass() {
        String name = "\u0418\u0432\u0430\u043d";
        String[] cases = {name, name, name, name, name};
        assertTrue(PlayerNames.checkDeclinedNames(name, cases));
    }

    @Test
    void checkDeclinedNamesWhenCaseMainPartDiffersShouldFail() {
        String name = "\u0418\u0432\u0430\u043d";
        String other = "\u041f\u0451\u0442\u0440";
        assertFalse(PlayerNames.checkDeclinedNames(name, new String[]{name, other, name, name, name}));
    }
}
