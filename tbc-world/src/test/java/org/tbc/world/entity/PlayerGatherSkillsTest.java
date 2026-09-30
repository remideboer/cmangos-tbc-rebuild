package org.tbc.world.entity;

import org.tbc.world.content.Content;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-166 — CMaNGOS UpdateGatherSkill / UpdateFishingSkill.
 */
class PlayerGatherSkillsTest {
    @Test
    void updateGatherSkillWhenHerbalismOrangeShouldGain() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_HERBALISM, 1, 75, 1);
        // red 1 → 5; orange below yellow(30); force success
        assertTrue(p.updateGatherSkill(Content.SKILL_HERBALISM, 1, 1, 1, () -> 1));
        assertEquals(2, p.skillValue(Content.SKILL_HERBALISM));
    }

    @Test
    void updateGatherSkillWhenUnknownSkillShouldNoOp() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_BLACKSMITHING, 1, 75, 1);
        assertFalse(p.updateGatherSkill(Content.SKILL_BLACKSMITHING, 1, 1, 1, () -> 1));
    }

    @Test
    void updateGatherSkillWhenSkinningHighSkillShouldStepDownChance() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_SKINNING, 75, 300, 1);
        // chance with steps: still orange vs red 0 → yellow band at red+25=25, value 75 is green/grey
        // grey at red+100=100; value 75 is green → 250 tenths; >> (75/75)=1 → 125; roll 126 misses
        assertFalse(p.updateGatherSkill(Content.SKILL_SKINNING, 75, 0, 1, () -> 126));
        assertEquals(75, p.skillValue(Content.SKILL_SKINNING));
        assertTrue(p.updateGatherSkill(Content.SKILL_SKINNING, 75, 0, 1, () -> 1));
        assertEquals(76, p.skillValue(Content.SKILL_SKINNING));
    }

    @Test
    void updateFishingSkillWhenStepsMetShouldRaiseSkill() {
        Player p = new Player();
        p.learnSkill(Content.SKILL_FISHING, 1, 75, 1);
        assertTrue(p.updateFishingSkill(() -> 1));
        assertEquals(2, p.skillValue(Content.SKILL_FISHING));
        assertEquals(0, p.fishingSteps);
    }

    @Test
    void fishingStepsNeededToLevelUpShouldMatchCmangosBands() {
        assertEquals(1, Player.fishingStepsNeededToLevelUp(1));
        assertEquals(2, Player.fishingStepsNeededToLevelUp(88));
        assertEquals(10, Player.fishingStepsNeededToLevelUp(310));
    }
}
