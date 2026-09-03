package org.tbc.world.pvp;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpObjectivesTest {
    @Test
    void tpSl24AvAbEyTimers() {
        assertEquals(240_000, PvpObjectives.avCaptureMs(false));
        assertEquals(300_000, PvpObjectives.avCaptureMs(true));
        assertEquals(60_000, PvpObjectives.AB_CONTEST_MS);
        assertEquals(12_000, PvpObjectives.abTickMs(1));
        assertEquals(1_000, PvpObjectives.abTickMs(5));
        assertEquals(180087, PvpObjectives.AB_STABLES);
        assertEquals(75, PvpObjectives.eyFlagPoints(1));
        assertEquals(500, PvpObjectives.eyFlagPoints(4));
        assertEquals(34976, PvpObjectives.EY_FLAG_AURA);
    }

    @Test
    void tpSl25OutdoorPvp() {
        Player p = new Player();
        OutdoorPvp zone = new OutdoorPvp();
        zone.deliverSilithyst(p, 200);
        assertEquals(200, zone.silithyst);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SILITHYST_WIN));
        zone.lockTerokkar(p);
        assertEquals(PvpObjectives.TIMER_TF_LOCK_MS, zone.terokkarLockMs);
        assertEquals(2767, PvpObjectives.WS_TF_LOCK_A);
        assertEquals(2768, PvpObjectives.WS_TF_LOCK_H);
        zone.captureHalaa(p);
        assertEquals(15, zone.halaaGuards);
        assertEquals(993, zone.halaaGy);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.HALAA_BUFF));
    }
}
