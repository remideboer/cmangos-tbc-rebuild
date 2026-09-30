package org.tbc.world.entity;

import org.tbc.world.content.Content;
import org.tbc.world.content.SkillLineAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-165 — CMaNGOS UpdateCraftSkill / UpdateSkillPro via SkillLineAbility.
 */
class PlayerCraftSkillsTest {
    @Test
    void skillGainChanceTenthsWhenOrangeYellowGreenGreyShouldMatchConfig() {
        assertEquals(1000, Player.skillGainChanceTenths(0, 40, 20, 1));
        assertEquals(750, Player.skillGainChanceTenths(1, 40, 20, 1));
        assertEquals(250, Player.skillGainChanceTenths(20, 40, 20, 1));
        assertEquals(0, Player.skillGainChanceTenths(40, 40, 20, 1));
    }

    @Test
    void updateSkillProWhenRollSucceedsShouldRaiseSkill() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        assertTrue(p.updateSkillPro(Content.SKILL_BLACKSMITHING, 1000, 1, () -> 1));
        assertEquals(2, p.skillValue(Content.SKILL_BLACKSMITHING));
    }

    @Test
    void updateSkillProWhenRollMissesOrGreyShouldNoOp() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        assertFalse(p.updateSkillPro(Content.SKILL_BLACKSMITHING, 750, 1, () -> 751));
        assertEquals(1, p.skillValue(Content.SKILL_BLACKSMITHING));
        assertFalse(p.updateSkillPro(Content.SKILL_BLACKSMITHING, 0, 1, () -> 1));
    }

    @Test
    void updateCraftSkillWhenRoughSharpeningStoneOrangeShouldGain() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        SkillLineAbility cat = SkillLineAbility.seeded();
        // value 1 >= min 1 → yellow 75%; force roll success
        assertTrue(p.updateCraftSkill(SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE, cat, () -> 1));
        assertEquals(2, p.skillValue(Content.SKILL_BLACKSMITHING));
    }

    @Test
    void updateCraftSkillWhenGreyOrUnknownSpellShouldNoOp() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_BLACKSMITHING, 40, 75, 1);
        SkillLineAbility cat = SkillLineAbility.seeded();
        assertFalse(p.updateCraftSkill(SkillLineAbility.SPELL_ROUGH_SHARPENING_STONE, cat, () -> 1));
        assertEquals(40, p.skillValue(Content.SKILL_BLACKSMITHING));
        assertFalse(p.updateCraftSkill(999999, cat, () -> 1));
    }
}
