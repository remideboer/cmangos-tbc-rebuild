package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;

/**
 * Resolves create-time map/zone for classless (and future custom starts).
 * Today: race default from {@link ObjectMgr#createForRace(int)}.
 * Later: create-UI zone override and {@link SideAffinity} without rewriting CharacterStore.
 */
public final class CreateSpawnResolver {
    private CreateSpawnResolver() {
    }

    /**
     * Future create-screen / account flag: Alliance / Horde / Neutral.
     * Not applied this cycle — {@link #resolve} ignores affinity.
     */
    public enum SideAffinity {
        RACE_DEFAULT,
        ALLIANCE,
        HORDE,
        NEUTRAL
    }

    public record SpawnChoice(int map, int zone, float x, float y, float z, float o) {
        static SpawnChoice from(ObjectMgr.CreateInfo ci) {
            return new SpawnChoice(ci.map(), ci.zone(), ci.x(), ci.y(), ci.z(), ci.o());
        }
    }

    /**
     * @param overrideZoneOrNull future create-UI zone id; null = race default starter
     */
    public static SpawnChoice resolve(ObjectMgr mgr, int race, Integer overrideZoneOrNull) {
        return resolve(mgr, race, overrideZoneOrNull, SideAffinity.RACE_DEFAULT);
    }

    /**
     * @param affinity reserved for neutral/faction override (unused until wired)
     */
    public static SpawnChoice resolve(ObjectMgr mgr, int race, Integer overrideZoneOrNull,
                                      SideAffinity affinity) {
        if (mgr == null) {
            return null;
        }
        // Future: if overrideZoneOrNull != null, look up a named hub / zone spawn table.
        if (overrideZoneOrNull != null) {
            // Not implemented — fall through to race default until create UI exists.
        }
        // Future: affinity ALLIANCE/HORDE/NEUTRAL may remap hub; RACE_DEFAULT uses race start.
        ObjectMgr.CreateInfo ci = mgr.createForRace(race);
        return ci == null ? null : SpawnChoice.from(ci);
    }
}
