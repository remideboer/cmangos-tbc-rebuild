package org.tbc.world.pvp;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

/** Outdoor PvP from spec/05-domain/outdoor-pvp.md. World-state ids are spec ids. */
public final class OutdoorPvp {
    public int silithyst;
    public int halaaGuards;
    public int halaaGy;
    public long terokkarLockMs;

    public void deliverSilithyst(Player p, int n) {
        silithyst = Math.min(PvpObjectives.SILITHYST_MAX, silithyst + n);
        if (silithyst >= PvpObjectives.SILITHYST_MAX) {
            p.auras.add(new Unit.Aura(PvpObjectives.SILITHYST_WIN, 0, 1));
        }
    }

    public void lockTerokkar(Player p) {
        terokkarLockMs = PvpObjectives.TIMER_TF_LOCK_MS;
        p.auras.add(new Unit.Aura(PvpObjectives.TEROKKAR_BLESSING, 0, 1));
    }

    public void captureHalaa(Player p) {
        halaaGuards = PvpObjectives.HALAA_GUARDS;
        halaaGy = PvpObjectives.HALAA_GY;
        p.auras.add(new Unit.Aura(PvpObjectives.HALAA_BUFF, 0, 1));
    }
}
