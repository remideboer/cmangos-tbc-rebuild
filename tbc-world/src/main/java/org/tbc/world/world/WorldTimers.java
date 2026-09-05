package org.tbc.world.world;

/**
 * World.h WorldTimers + IntervalTimer. Intervals: spec/05-domain/world-loop.md
 * AH-bot / metrics / Warden are Vision-Out.
 */
public final class WorldTimers {
    public static final int AUCTIONS = 0;
    public static final int UPTIME = 1;
    public static final int CORPSES = 2;
    public static final int EVENTS = 3;
    public static final int DELETECHARS = 4;
    public static final int GROUPS = 6;
    public static final int COUNT = 9;

    /** WUPDATE_AUCTIONS. */
    public static final int AUCTIONS_MS = 60_000;
    /** WUPDATE_CORPSES. */
    public static final int CORPSES_MS = 20 * 60_000;
    /** WUPDATE_DELETECHARS. */
    public static final int DELETECHARS_MS = 24 * 60 * 60_000;
    /** WUPDATE_GROUPS. */
    public static final int GROUPS_MS = 1_000;
    /** CONFIG_UINT32_INTERVAL_CHANGEWEATHER default. */
    public static final int CHANGE_WEATHER_MS = 10 * 60_000;

    private final IntervalTimer[] timers = new IntervalTimer[COUNT];
    private final IntervalTimer weather = new IntervalTimer();

    public WorldTimers() {
        for (int i = 0; i < COUNT; i++) {
            timers[i] = new IntervalTimer();
        }
        timers[AUCTIONS].setInterval(AUCTIONS_MS);
        timers[CORPSES].setInterval(CORPSES_MS);
        timers[EVENTS].setInterval(AUCTIONS_MS);
        timers[DELETECHARS].setInterval(DELETECHARS_MS);
        timers[GROUPS].setInterval(GROUPS_MS);
        weather.setInterval(CHANGE_WEATHER_MS);
    }

    public void advance(int diff) {
        for (IntervalTimer t : timers) {
            if (t.current() >= 0) {
                t.update(diff);
            } else {
                t.setCurrent(0);
            }
        }
        if (weather.current() >= 0) {
            weather.update(diff);
        } else {
            weather.setCurrent(0);
        }
    }

    public boolean passed(int id) {
        return timers[id].passed();
    }

    public void reset(int id) {
        timers[id].reset();
    }

    public void setInterval(int id, int ms) {
        timers[id].setInterval(ms);
    }

    public boolean weatherPassed() {
        return weather.passed();
    }

    public void resetWeather() {
        weather.reset();
    }

    static final class IntervalTimer {
        private int interval;
        private int current;

        void update(int diff) {
            current += diff;
            if (current < 0) {
                current = 0;
            }
        }

        boolean passed() {
            return current >= interval;
        }

        void reset() {
            if (current >= interval) {
                current -= interval;
            }
        }

        void setCurrent(int v) {
            current = v;
        }

        void setInterval(int v) {
            interval = v;
        }

        int current() {
            return current;
        }
    }
}
