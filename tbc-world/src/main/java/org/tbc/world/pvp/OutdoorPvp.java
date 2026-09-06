package org.tbc.world.pvp;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

/** Outdoor PvP from spec/05-domain/outdoor-pvp.md. World-state ids are spec ids. */
public final class OutdoorPvp {
    public int silithyst;
    public int silithystAlliance;
    public int silithystHorde;
    public int halaaGuards;
    public int halaaGy;
    public int zmGy;
    public long terokkarLockMs;
    private boolean zmEastOwned;
    private boolean zmEastAlliance;
    private boolean zmWestOwned;
    private boolean zmWestAlliance;
    private final java.util.Set<Long> tfAlliance = new java.util.HashSet<>();
    private final java.util.Set<Long> tfHorde = new java.util.HashSet<>();
    private final java.util.ArrayDeque<int[]> pendingWs = new java.util.ArrayDeque<>();

    public void deliverSilithyst(Player p, int n) {
        deliverSilithyst(p, n, true);
    }

    /** Alliance delivery path (AT 4162). Emits WS 2313; at 200 applies zone buff 30754. */
    public void deliverSilithyst(Player p, int n, boolean alliance) {
        if (alliance) {
            silithystAlliance = Math.min(PvpObjectives.SILITHYST_MAX, silithystAlliance + n);
            silithyst = silithystAlliance;
            emit(PvpObjectives.WS_SILITHYST_A, silithystAlliance);
        } else {
            silithystHorde = Math.min(PvpObjectives.SILITHYST_MAX, silithystHorde + n);
            silithyst = silithystHorde;
            emit(PvpObjectives.WS_SILITHYST_H, silithystHorde);
        }
        if (silithyst >= PvpObjectives.SILITHYST_MAX) {
            p.auras.add(new Unit.Aura(PvpObjectives.SILITHYST_WIN, 0, 1));
        }
    }

    public void lockTerokkar(Player p) {
        lockTerokkar(p, true);
    }

    public void lockTerokkar(Player p, boolean alliance) {
        terokkarLockMs = PvpObjectives.TIMER_TF_LOCK_MS;
        p.auras.add(new Unit.Aura(PvpObjectives.TEROKKAR_BLESSING, 0, 1));
        if (alliance) {
            emit(PvpObjectives.WS_TF_LOCK_A, 1);
            emit(PvpObjectives.WS_TF_LOCK_H, 0);
        } else {
            emit(PvpObjectives.WS_TF_LOCK_A, 0);
            emit(PvpObjectives.WS_TF_LOCK_H, 1);
        }
    }

    /** Own a Terokkar tower GO; five Alliance or Horde towers lock the zone (TP-SL25-002). */
    public void captureTfTower(Player p, long goEntry, boolean alliance) {
        if (!PvpObjectives.isTfTower(goEntry)) {
            return;
        }
        if (alliance) {
            if (!tfAlliance.add(goEntry)) {
                return;
            }
            emit(PvpObjectives.WS_TF_COUNT_A, tfAlliance.size());
            if (tfAlliance.size() >= 5) {
                lockTerokkar(p, true);
            }
        } else {
            if (!tfHorde.add(goEntry)) {
                return;
            }
            emit(PvpObjectives.WS_TF_COUNT_H, tfHorde.size());
            if (tfHorde.size() >= 5) {
                lockTerokkar(p, false);
            }
        }
    }

    public void captureHalaa(Player p) {
        captureHalaa(p, true);
    }

    /** Banner GO 182210 — WS A/H/N 2673/2672/2671, 15 guards, GY 993, buff 33795 (TP-SL25-003). */
    public void captureHalaa(Player p, boolean alliance) {
        halaaGuards = PvpObjectives.HALAA_GUARDS;
        halaaGy = PvpObjectives.HALAA_GY;
        p.auras.add(new Unit.Aura(PvpObjectives.HALAA_BUFF, 0, 1));
        if (alliance) {
            emit(PvpObjectives.WS_HALAA_A, 1);
            emit(PvpObjectives.WS_HALAA_H, 0);
            emit(PvpObjectives.WS_HALAA_N, 0);
        } else {
            emit(PvpObjectives.WS_HALAA_A, 0);
            emit(PvpObjectives.WS_HALAA_H, 1);
            emit(PvpObjectives.WS_HALAA_N, 0);
        }
    }

    /** Eastern Plaguelands Northpass tower GO 181899 — WS A/H/N 2372/2373/2352. */
    public void captureNorthpass(boolean alliance) {
        emitEp(alliance, PvpObjectives.WS_EP_NORTHPASS_A, PvpObjectives.WS_EP_NORTHPASS_H,
                PvpObjectives.WS_EP_NORTHPASS_N);
    }

    /** Crownguard GO 182096 — WS 2378/2379/2355. */
    public void captureCrownguard(boolean alliance) {
        emitEp(alliance, PvpObjectives.WS_EP_CROWNGUARD_A, PvpObjectives.WS_EP_CROWNGUARD_H,
                PvpObjectives.WS_EP_CROWNGUARD_N);
    }

    /** Eastwall GO 182097 — WS 2354/2356/2361. */
    public void captureEastwall(boolean alliance) {
        emitEp(alliance, PvpObjectives.WS_EP_EASTWALL_A, PvpObjectives.WS_EP_EASTWALL_H,
                PvpObjectives.WS_EP_EASTWALL_N);
    }

    /** Plaguewood GO 182098 — WS 2370/2371/2353. */
    public void capturePlaguewood(boolean alliance) {
        emitEp(alliance, PvpObjectives.WS_EP_PLAGUEWOOD_A, PvpObjectives.WS_EP_PLAGUEWOOD_H,
                PvpObjectives.WS_EP_PLAGUEWOOD_N);
    }

    private void emitEp(boolean alliance, int wsA, int wsH, int wsN) {
        if (alliance) {
            emit(wsA, 1);
            emit(wsH, 0);
            emit(wsN, 0);
        } else {
            emit(wsA, 0);
            emit(wsH, 1);
            emit(wsN, 0);
        }
    }

    /** Zangarmarsh East beacon GO 182523 — UI WS A/H/N 2558/2559/2560 (TP-SL25-005). */
    public void captureZmEast(boolean alliance) {
        zmEastAlliance = alliance;
        zmEastOwned = true;
        if (alliance) {
            emit(PvpObjectives.WS_ZM_EAST_A, 1);
            emit(PvpObjectives.WS_ZM_EAST_H, 0);
            emit(PvpObjectives.WS_ZM_EAST_N, 0);
        } else {
            emit(PvpObjectives.WS_ZM_EAST_A, 0);
            emit(PvpObjectives.WS_ZM_EAST_H, 1);
            emit(PvpObjectives.WS_ZM_EAST_N, 0);
        }
    }

    /** Zangarmarsh West beacon GO 182522 — UI WS A/H/N 2555/2556/2557 (TP-SL25-009). */
    public void captureZmWest(boolean alliance) {
        zmWestAlliance = alliance;
        zmWestOwned = true;
        if (alliance) {
            emit(PvpObjectives.WS_ZM_WEST_A, 1);
            emit(PvpObjectives.WS_ZM_WEST_H, 0);
            emit(PvpObjectives.WS_ZM_WEST_N, 0);
        } else {
            emit(PvpObjectives.WS_ZM_WEST_A, 0);
            emit(PvpObjectives.WS_ZM_WEST_H, 1);
            emit(PvpObjectives.WS_ZM_WEST_N, 0);
        }
    }

    /**
     * Twin Spire GY via center banner GO 182529 — requires both beacons + battle standard
     * (32430 A / 32431 H). GY WS 2648/2649/2647, buff 33779, GY 969 (TP-SL25-010).
     */
    public boolean claimZmGraveyard(Player p, boolean alliance) {
        if (!zmEastOwned || !zmWestOwned) {
            return false;
        }
        if (alliance) {
            if (!zmEastAlliance || !zmWestAlliance) {
                return false;
            }
            if (p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.SPELL_BATTLE_STANDARD_A)) {
                return false;
            }
            p.auras.removeIf(a -> a.spellId() == PvpObjectives.SPELL_BATTLE_STANDARD_A);
            emit(PvpObjectives.WS_ZM_GY_A, 1);
            emit(PvpObjectives.WS_ZM_GY_H, 0);
            emit(PvpObjectives.WS_ZM_GY_N, 0);
        } else {
            if (zmEastAlliance || zmWestAlliance) {
                return false;
            }
            if (p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.SPELL_BATTLE_STANDARD_H)) {
                return false;
            }
            p.auras.removeIf(a -> a.spellId() == PvpObjectives.SPELL_BATTLE_STANDARD_H);
            emit(PvpObjectives.WS_ZM_GY_A, 0);
            emit(PvpObjectives.WS_ZM_GY_H, 1);
            emit(PvpObjectives.WS_ZM_GY_N, 0);
        }
        zmGy = PvpObjectives.ZM_GY;
        p.auras.add(new Unit.Aura(PvpObjectives.ZM_TWIN_SPIRE_BLESSING, 0, 1));
        return true;
    }

    public java.util.List<int[]> drainWorldStates() {
        java.util.List<int[]> out = new java.util.ArrayList<>(pendingWs.size());
        while (!pendingWs.isEmpty()) {
            out.add(pendingWs.poll());
        }
        return out;
    }

    private void emit(int field, int value) {
        pendingWs.add(new int[] {field, value});
    }
}
