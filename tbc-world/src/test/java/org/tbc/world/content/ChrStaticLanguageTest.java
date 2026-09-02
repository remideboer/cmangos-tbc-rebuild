package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChrStaticLanguageTest {
    @Test
    void allianceFactionAndRacialSkills() {
        assertArrayEquals(new int[]{ChrStatic.SKILL_LANG_COMMON}, ChrStatic.languageSkills(1));
        assertArrayEquals(new int[]{ChrStatic.SKILL_LANG_COMMON, ChrStatic.SKILL_LANG_GNOMISH},
                ChrStatic.languageSkills(7));
        assertArrayEquals(new int[]{ChrStatic.SKILL_LANG_COMMON, ChrStatic.SKILL_LANG_DWARVEN},
                ChrStatic.languageSkills(3));
        assertArrayEquals(new int[]{ChrStatic.SPELL_LANG_COMMON, ChrStatic.SPELL_LANG_GNOMISH},
                ChrStatic.languageSpells(7));
        assertEquals(ChrStatic.SKILL_LANG_ORCISH, ChrStatic.languageSkills(2)[0]);
        assertEquals(ChrStatic.SPELL_LANG_ORCISH, ChrStatic.languageSpells(2)[0]);
        assertEquals(1, ChrStatic.languageSpells(1).length);
        assertTrue(ChrStatic.isLanguageSkill(ChrStatic.SKILL_LANG_COMMON));
        assertTrue(ChrStatic.isLanguageSkill(ChrStatic.SKILL_LANG_GNOMISH));
    }
}
