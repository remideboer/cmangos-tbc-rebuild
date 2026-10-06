package org.tbc.world.companion;

/**
 * Offline-character companion (Vision-Out carve-out). Default on; opt out with {@code withEnabled(false)}.
 */
public final class CompanionConfig {
    private static volatile CompanionConfig instance = defaults();

    private final boolean enabled;
    private final int maxCompanions;

    public CompanionConfig(boolean enabled, int maxCompanions) {
        this.enabled = enabled;
        this.maxCompanions = Math.max(1, maxCompanions);
    }

    public static CompanionConfig get() {
        return instance;
    }

    public static void set(CompanionConfig config) {
        instance = config == null ? defaults() : config;
    }

    public static void reset() {
        instance = defaults();
    }

    public static CompanionConfig defaults() {
        return new CompanionConfig(true, 1);
    }

    public CompanionConfig withEnabled(boolean on) {
        return new CompanionConfig(on, maxCompanions);
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxCompanions() {
        return maxCompanions;
    }
}
