package org.tbc.world.classless;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Copper reset ladder: free first, then 1g × 2^(n−1). Token path stubbed empty. */
class HeroStatResetCostTest {

    @Test
    void nextCopperCostWhenResetCountZeroShouldBeFree() {
        assertEquals(0, HeroStatResetCost.nextCopperCost(0));
    }

    @Test
    void nextCopperCostWhenEscalatingShouldDoubleGold() {
        assertEquals(10_000, HeroStatResetCost.nextCopperCost(1));
        assertEquals(20_000, HeroStatResetCost.nextCopperCost(2));
        assertEquals(40_000, HeroStatResetCost.nextCopperCost(3));
        assertEquals(80_000, HeroStatResetCost.nextCopperCost(4));
    }

    @Test
    void tokenCostsWhenV1ShouldBeEmpty() {
        assertTrue(HeroStatResetCost.tokenCosts().isEmpty());
    }
}
