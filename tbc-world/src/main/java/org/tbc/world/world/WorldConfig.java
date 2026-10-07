package org.tbc.world.world;

import org.tbc.common.Conf;

/**
 * World tunables from mangosd.conf (CMaNGOS World::LoadConfigSettings subset).
 * Say/yell range are the 2.4.3 constants (25 / 300 yd); {@code inboundOpcodeTrace} is the lab opcode log.
 */
public record WorldConfig(String motd, int realmId, int instantLogout, int maxOverspeedPings, double sayRange,
                          double yellRange, int saveIntervalMs, boolean inboundOpcodeTrace) {
    private static final String DEFAULT_MOTD = "Welcome to the 8606 rebuild.";

    /** Defaults used by {@link World#inMemory()} and when a key is absent from the conf. */
    public static WorldConfig inMemory() {
        return new WorldConfig(DEFAULT_MOTD, 1, 3, 2, 25, 300, 900_000, false);
    }

    public static WorldConfig fromConf(Conf conf) {
        return new WorldConfig(
                conf.get("Motd", DEFAULT_MOTD),
                conf.getInt("RealmID", 1),
                conf.getInt("InstantLogout", 3),
                conf.getInt("MaxOverspeedPings", 2),
                25,
                300,
                conf.getInt("PlayerSave.Interval", 900_000),
                conf.getBool("LogInboundOpcodes", false));
    }
}
