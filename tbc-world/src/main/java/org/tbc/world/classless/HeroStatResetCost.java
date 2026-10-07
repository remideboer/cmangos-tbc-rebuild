package org.tbc.world.classless;

import java.util.Collections;
import java.util.List;

/**
 * Hero spent-stat reset pricing. First reset is free; then copper {@code 1g × 2^(n−1)}
 * where {@code n} is the successful reset count. Token costs are stubbed empty for v1
 * so a later item-id path can plug in without changing callers.
 */
public final class HeroStatResetCost {
    public static final int GOLD_COPPER = 10_000;

    public record TokenCost(int itemId, int count) {
    }

    private HeroStatResetCost() {
    }

    /**
     * Copper required for the next reset given how many resets have already succeeded.
     * {@code resetCount == 0} → free; otherwise {@code 10000 × 2^(resetCount−1)}, clamped.
     */
    public static int nextCopperCost(int resetCount) {
        if (resetCount <= 0) {
            return 0;
        }
        int shift = resetCount - 1;
        if (shift >= 31) {
            return Integer.MAX_VALUE;
        }
        long copper = (long) GOLD_COPPER << shift;
        return copper > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) copper;
    }

    /** Future token costs (item id + count). Empty in v1. */
    public static List<TokenCost> tokenCosts() {
        return Collections.emptyList();
    }
}
