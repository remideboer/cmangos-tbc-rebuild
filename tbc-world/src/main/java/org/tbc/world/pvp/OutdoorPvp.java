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
    public long terokkarLockMs;
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
        terokkarLockMs = PvpObjectives.TIMER_TF_LOCK_MS;
        p.auras.add(new Unit.Aura(PvpObjectives.TEROKKAR_BLESSING, 0, 1));
        emit(PvpObjectives.WS_TF_LOCK_A, 1);
        emit(PvpObjectives.WS_TF_LOCK_H, 0);
    }

    public void captureHalaa(Player p) {
        halaaGuards = PvpObjectives.HALAA_GUARDS;
        halaaGy = PvpObjectives.HALAA_GY;
        p.auras.add(new Unit.Aura(PvpObjectives.HALAA_BUFF, 0, 1));
    }

    /** Eastern Plaguelands Northpass tower GO 181899 — WS A/H/N 2372/2373/2352. */
    public void captureNorthpass(boolean alliance) {
        if (alliance) {
            emit(PvpObjectives.WS_EP_NORTHPASS_A, 1);
            emit(PvpObjectives.WS_EP_NORTHPASS_H, 0);
            emit(PvpObjectives.WS_EP_NORTHPASS_N, 0);
        } else {
            emit(PvpObjectives.WS_EP_NORTHPASS_A, 0);
            emit(PvpObjectives.WS_EP_NORTHPASS_H, 1);
            emit(PvpObjectives.WS_EP_NORTHPASS_N, 0);
        }
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
