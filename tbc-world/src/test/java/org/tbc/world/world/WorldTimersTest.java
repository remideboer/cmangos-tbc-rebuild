package org.tbc.world.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTimersTest {
    @Test
    void advanceWhenAuctionIntervalElapsedShouldPassThenReset() {
        WorldTimers t = new WorldTimers();
        t.advance(WorldTimers.AUCTIONS_MS - 1);
        assertFalse(t.passed(WorldTimers.AUCTIONS));
        t.advance(1);
        assertTrue(t.passed(WorldTimers.AUCTIONS));
        t.reset(WorldTimers.AUCTIONS);
        assertFalse(t.passed(WorldTimers.AUCTIONS));
    }

    @Test
    void advanceWhenWeatherIntervalElapsedShouldPassThenReset() {
        WorldTimers t = new WorldTimers();
        t.advance(WorldTimers.CHANGE_WEATHER_MS - 1);
        assertFalse(t.weatherPassed());
        t.advance(1);
        assertTrue(t.weatherPassed());
        t.resetWeather();
        assertFalse(t.weatherPassed());
    }
}
