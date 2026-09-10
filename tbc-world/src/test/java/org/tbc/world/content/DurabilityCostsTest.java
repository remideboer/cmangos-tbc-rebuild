package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilityCostsTest {
    @Test
    void repairCopperWhenWornShortswordLostTenShouldBeEight() {
        assertEquals(8, DurabilityCosts.repairCopper(
                10, 2, DurabilityCosts.ITEM_CLASS_WEAPON, 7, 1, 1.0f));
    }

    @Test
    void repairCopperWhenLostZeroShouldBeZero() {
        assertEquals(0, DurabilityCosts.repairCopper(
                0, 2, DurabilityCosts.ITEM_CLASS_WEAPON, 7, 1, 1.0f));
    }

    @Test
    void repairCopperWhenMissingItemLevelShouldBeZero() {
        assertEquals(0, DurabilityCosts.repairCopper(
                10, 999, DurabilityCosts.ITEM_CLASS_WEAPON, 7, 1, 1.0f));
    }

    @Test
    void repairCopperWhenQualityModTruncatesToZeroShouldBeOne() {
        assertEquals(1, DurabilityCosts.repairCopper(
                1, 2, DurabilityCosts.ITEM_CLASS_WEAPON, 7, 6, 1.0f));
    }
}
