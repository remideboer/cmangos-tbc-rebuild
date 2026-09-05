package org.tbc.world.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSaveTimerTest {
    @Test
    void jitteredFirstSaveMsWhenDefaultIntervalShouldStayBetweenHalfAndThreeHalves() {
        int interval = 900_000;
        int lo = interval / 2;
        int hi = (int) (interval * 3L / 2);
        for (int roll = 0; roll < 64; roll++) {
            int delay = World.jitteredFirstSaveMs(interval, roll);
            assertTrue(delay >= lo && delay <= hi, () -> "delay=" + delay);
        }
        assertEquals(lo, World.jitteredFirstSaveMs(interval, 0));
        assertEquals(hi, World.jitteredFirstSaveMs(interval, hi - lo));
        assertEquals(lo, World.jitteredFirstSaveMs(interval, -(hi - lo + 1)));
    }

    @Test
    void jitteredFirstSaveMsWhenIntervalZeroShouldReturnZero() {
        assertEquals(0, World.jitteredFirstSaveMs(0, 7));
    }
}
