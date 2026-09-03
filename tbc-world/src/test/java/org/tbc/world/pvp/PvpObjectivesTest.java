package org.tbc.world.pvp;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpObjectivesTest {
    @Test
    void tpSl24AvCaptureTimer() {
        assertEquals(240_000, PvpObjectives.avCaptureMs(false));
        assertEquals(300_000, PvpObjectives.avCaptureMs(true));
    }

    @Test
    void tpSl24AbStablesTick() {
        assertEquals(60_000, PvpObjectives.AB_CONTEST_MS);
        assertEquals(180087, PvpObjectives.AB_STABLES);
        assertEquals(12_000, PvpObjectives.abTickMs(1));
        assertEquals(1_000, PvpObjectives.abTickMs(5));
        assertEquals(1776, PvpObjectives.WS_AB_RES_A);
        assertEquals(1777, PvpObjectives.WS_AB_RES_H);
    }

    @Test
    void tpSl24EyFlagPoints() {
        assertEquals(75, PvpObjectives.eyFlagPoints(1));
        assertEquals(500, PvpObjectives.eyFlagPoints(4));
        assertEquals(34976, PvpObjectives.EY_FLAG_AURA);
    }

    @Test
    void tpSl24AbBlacksmithWs() {
        assertEquals(1782, PvpObjectives.WS_AB_BLACKSMITH_A);
        assertEquals(1783, PvpObjectives.WS_AB_BLACKSMITH_H);
    }

    @Test
    void tpSl25SilithystCap() {
        Player p = new Player();
        OutdoorPvp zone = new OutdoorPvp();
        zone.deliverSilithyst(p, 200);
        assertEquals(PvpObjectives.SILITHYST_MAX, zone.silithyst);
        assertEquals(2313, PvpObjectives.WS_SILITHYST_A);
        assertEquals(2314, PvpObjectives.WS_SILITHYST_H);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SILITHYST_WIN));
    }

    @Test
    void tpSl25TerokkarLock() {
        Player p = new Player();
        OutdoorPvp zone = new OutdoorPvp();
        zone.lockTerokkar(p);
        assertEquals(PvpObjectives.TIMER_TF_LOCK_MS, zone.terokkarLockMs);
        assertEquals(2767, PvpObjectives.WS_TF_LOCK_A);
        assertEquals(2768, PvpObjectives.WS_TF_LOCK_H);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.TEROKKAR_BLESSING));
    }

    @Test
    void tpSl25HalaaBanner() {
        Player p = new Player();
        OutdoorPvp zone = new OutdoorPvp();
        zone.captureHalaa(p);
        assertEquals(15, zone.halaaGuards);
        assertEquals(993, zone.halaaGy);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.HALAA_BUFF));
    }
}
