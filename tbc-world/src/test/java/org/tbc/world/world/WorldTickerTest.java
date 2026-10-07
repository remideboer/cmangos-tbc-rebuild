package org.tbc.world.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ordered per-tick steps carved from World.tick (refactoring plan cycle 3.2). */
class WorldTickerTest {
    @Test
    void tickShouldRunStepsInRegistrationOrderWithTheSameDiff() {
        List<String> ran = new ArrayList<>();
        WorldTicker t = new WorldTicker()
                .step("sessions", d -> ran.add("sessions:" + d))
                .step("spells", d -> ran.add("spells:" + d))
                .step("quests", () -> ran.add("quests"));
        t.tick(50);
        assertEquals(List.of("sessions:50", "spells:50", "quests"), ran);
        assertEquals(List.of("sessions", "spells", "quests"), t.names());
    }

    @Test
    void everyShouldRunOnlyWhenTimerPassedAndThenResetIt() {
        List<String> ran = new ArrayList<>();
        WorldTimers timers = new WorldTimers();
        WorldTicker t = new WorldTicker().every(timers, WorldTimers.GROUPS, "groups", () -> ran.add("groups"));
        timers.advance(WorldTimers.GROUPS_MS - 1);
        t.tick(0);
        assertEquals(List.of(), ran);
        timers.advance(1);
        t.tick(0);
        assertEquals(List.of("groups"), ran);
        t.tick(0);
        assertEquals(List.of("groups"), ran, "reset after firing, so it does not fire twice in a row");
    }

    @Test
    void everyWithNextIntervalShouldRescheduleWithTheReturnedInterval() {
        List<String> ran = new ArrayList<>();
        WorldTimers timers = new WorldTimers();
        timers.setInterval(WorldTimers.EVENTS, 100);
        WorldTicker t = new WorldTicker().everyWithNextInterval(timers, WorldTimers.EVENTS, "events", () -> {
            ran.add("events");
            return 300;
        });
        timers.advance(100);
        t.tick(0);
        assertEquals(List.of("events"), ran);
        // IntervalTimer.reset only subtracts when current >= interval: the elapsed 100 ms carry into the new 300.
        timers.advance(199);
        t.tick(0);
        assertEquals(List.of("events"), ran, "new 300 ms interval not yet passed");
        timers.advance(1);
        t.tick(0);
        assertEquals(List.of("events", "events"), ran);
    }

    @Test
    void everyWithSupplierShouldResetBeforeActionAndSkipWhenNotPassed() {
        List<String> ran = new ArrayList<>();
        boolean[] due = {false};
        WorldTicker t = new WorldTicker()
                .every(() -> due[0], () -> ran.add("reset"), "weather", () -> ran.add("weather"));
        t.tick(0);
        assertEquals(List.of(), ran);
        due[0] = true;
        t.tick(0);
        assertEquals(List.of("reset", "weather"), ran);
    }
}
