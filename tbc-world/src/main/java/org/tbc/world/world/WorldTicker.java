package org.tbc.world.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Ordered per-tick steps of the world loop (CMaNGOS World::Update body, one entry per concern). */
public final class WorldTicker {
    @FunctionalInterface
    public interface Step {
        void run(int diff);
    }

    private record Named(String name, Step step) {}

    private final List<Named> steps = new ArrayList<>();

    public WorldTicker step(String name, Step step) {
        steps.add(new Named(name, step));
        return this;
    }

    public WorldTicker step(String name, Runnable step) {
        return step(name, d -> step.run());
    }

    /** Run {@code action} once each time the {@link WorldTimers} interval {@code timer} has passed, then reset it. */
    public WorldTicker every(WorldTimers timers, int timer, String name, Runnable action) {
        return every(() -> timers.passed(timer), () -> timers.reset(timer), name, action);
    }

    /**
     * Interval step whose action returns the next interval (CMaNGOS GameEventMgr::Update): when the timer has
     * passed, run {@code action}, apply its result with {@link WorldTimers#setInterval}, then reset.
     */
    public WorldTicker everyWithNextInterval(WorldTimers timers, int timer, String name, IntSupplier action) {
        return step(name, d -> {
            if (timers.passed(timer)) {
                timers.setInterval(timer, action.getAsInt());
                timers.reset(timer);
            }
        });
    }

    /** Generic interval step: when {@code passed} reports true, {@code reset} runs first, then {@code action}. */
    public WorldTicker every(BooleanSupplier passed, Runnable reset, String name, Runnable action) {
        return step(name, d -> {
            if (passed.getAsBoolean()) {
                reset.run();
                action.run();
            }
        });
    }

    public void tick(int diff) {
        for (Named n : steps) {
            n.step.run(diff);
        }
    }

    public List<String> names() {
        List<String> out = new ArrayList<>(steps.size());
        for (Named n : steps) {
            out.add(n.name);
        }
        return out;
    }
}
