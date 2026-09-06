package org.tbc.world.pvp;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Eye of the Storm flag score + tower ticks from battleground-ey.md. */
public final class EyBattlefield {
    private int towersAlliance;
    private int resourcesAlliance;
    private long nextResourceTickAt;
    private final ArrayDeque<int[]> pendingWs = new ArrayDeque<>();

    public int towersAlliance() {
        return towersAlliance;
    }

    public int resourcesAlliance() {
        return resourcesAlliance;
    }

    public void setTowersOwned(int count) {
        towersAlliance = Math.max(0, Math.min(4, count));
        emit(PvpObjectives.WS_EY_TOWERS_A, towersAlliance);
        if (towersAlliance == 0) {
            nextResourceTickAt = 0;
        }
    }

    /** BattleGroundEY::UpdateResources — every 2000 ms add eyTickPoints. */
    public void advance(long nowMs) {
        if (towersAlliance <= 0) {
            return;
        }
        if (nextResourceTickAt == 0) {
            nextResourceTickAt = nowMs + PvpObjectives.EY_TICK_MS;
            return;
        }
        if (nowMs < nextResourceTickAt) {
            return;
        }
        int pts = PvpObjectives.eyTickPoints(towersAlliance);
        resourcesAlliance = Math.min(2000, resourcesAlliance + pts);
        emit(PvpObjectives.WS_EY_RES_A, resourcesAlliance);
        nextResourceTickAt = nowMs + PvpObjectives.EY_TICK_MS;
    }

    /** Carrier scores at an owned tower AT — points by tower count. */
    public int scoreFlagAtOwnedTower(Player carrier) {
        if (carrier == null || towersAlliance <= 0) {
            return 0;
        }
        boolean hasFlag = carrier.auras.removeIf(a -> a.spellId() == PvpObjectives.EY_FLAG_AURA);
        if (!hasFlag) {
            return 0;
        }
        int pts = PvpObjectives.eyFlagPoints(towersAlliance);
        resourcesAlliance = Math.min(2000, resourcesAlliance + pts);
        emit(PvpObjectives.WS_EY_RES_A, resourcesAlliance);
        return pts;
    }

    public void pickupFlag(Player p) {
        if (p == null) {
            return;
        }
        if (p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.EY_FLAG_AURA)) {
            p.auras.add(new Unit.Aura(PvpObjectives.EY_FLAG_AURA, 0, 1));
        }
    }

    public List<int[]> drainWorldStates() {
        List<int[]> out = new ArrayList<>(pendingWs.size());
        while (!pendingWs.isEmpty()) {
            out.add(pendingWs.poll());
        }
        return out;
    }

    private void emit(int field, int value) {
        pendingWs.add(new int[] {field, value});
    }
}
