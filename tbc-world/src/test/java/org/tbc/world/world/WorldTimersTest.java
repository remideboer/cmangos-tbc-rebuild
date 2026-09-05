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

    @Test
    void advanceWhenCorpseIntervalElapsedShouldPassThenReset() {
        WorldTimers t = new WorldTimers();
        t.advance(WorldTimers.CORPSES_MS - 1);
        assertFalse(t.passed(WorldTimers.CORPSES));
        t.advance(1);
        assertTrue(t.passed(WorldTimers.CORPSES));
        t.reset(WorldTimers.CORPSES);
        assertFalse(t.passed(WorldTimers.CORPSES));
    }

    @Test
    void advanceWhenGroupsIntervalElapsedShouldPassThenReset() {
        WorldTimers t = new WorldTimers();
        t.advance(WorldTimers.GROUPS_MS - 1);
        assertFalse(t.passed(WorldTimers.GROUPS));
        t.advance(1);
        assertTrue(t.passed(WorldTimers.GROUPS));
        t.reset(WorldTimers.GROUPS);
        assertFalse(t.passed(WorldTimers.GROUPS));
    }

    @Test
    void advanceWhenDeleteCharsIntervalElapsedShouldPassThenReset() {
        WorldTimers t = new WorldTimers();
        t.advance(WorldTimers.DELETECHARS_MS - 1);
        assertFalse(t.passed(WorldTimers.DELETECHARS));
        t.advance(1);
        assertTrue(t.passed(WorldTimers.DELETECHARS));
        t.reset(WorldTimers.DELETECHARS);
        assertFalse(t.passed(WorldTimers.DELETECHARS));
    }
}
