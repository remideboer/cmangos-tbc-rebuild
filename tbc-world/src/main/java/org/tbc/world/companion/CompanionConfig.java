package org.tbc.world.companion;

/**
 * Optional offline-character companion (gated Vision-Out carve-out).
 * Default off — not required for Vision-In slices.
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
        return new CompanionConfig(false, 1);
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
