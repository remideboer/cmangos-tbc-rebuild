package org.tbc.world.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Arathi Basin node timers / world states from battleground-ab.md.
 * Stables GO {@link PvpObjectives#AB_STABLES}.
 */
public final class AbBattlefield {
    public static final int TEAM_ALLIANCE = 0;
    public static final int TEAM_HORDE = 1;

    public static final int STATUS_NEUTRAL = 0;
    public static final int STATUS_ALLY_CONT = 1;
    public static final int STATUS_HORDE_CONT = 2;
    public static final int STATUS_ALLY_OCC = 3;
    public static final int STATUS_HORDE_OCC = 4;

    private int stablesStatus = STATUS_NEUTRAL;
    private long stablesReadyAt;
    private int assaultTeam = TEAM_ALLIANCE;
    private int ownedAlliance;
    private int ownedHorde;
    private int resourcesAlliance;
    private int resourcesHorde;
    private long nextResourceTickAt;
    private final ArrayDeque<int[]> pendingWs = new ArrayDeque<>();

    public int stablesStatus() {
        return stablesStatus;
    }

    public int ownedAlliance() {
        return ownedAlliance;
    }

    public int resourcesAlliance() {
        return resourcesAlliance;
    }

    /** Neutral → contested 60 s (TP-SL24-002). */
    public void assaultStables(int team, long nowMs) {
        if (team != TEAM_ALLIANCE && team != TEAM_HORDE) {
            return;
        }
        assaultTeam = team;
        stablesStatus = team == TEAM_ALLIANCE ? STATUS_ALLY_CONT : STATUS_HORDE_CONT;
        stablesReadyAt = nowMs + PvpObjectives.AB_CONTEST_MS;
        emit(team == TEAM_ALLIANCE
                ? PvpObjectives.WS_AB_STABLES_CONT_A
                : PvpObjectives.WS_AB_STABLES_CONT_H, 1);
    }

    /** Contested → occupied when {@code nowMs} reaches capture deadline. */
    public void advance(long nowMs) {
        if (stablesReadyAt > 0 && nowMs >= stablesReadyAt) {
            stablesReadyAt = 0;
            if (assaultTeam == TEAM_ALLIANCE) {
                stablesStatus = STATUS_ALLY_OCC;
                ownedAlliance++;
                emit(PvpObjectives.WS_AB_STABLES_OCC_A, 1);
                nextResourceTickAt = nowMs + PvpObjectives.abTickMs(ownedAlliance);
            } else {
                stablesStatus = STATUS_HORDE_OCC;
                ownedHorde++;
                emit(PvpObjectives.WS_AB_STABLES_OCC_H, 1);
                nextResourceTickAt = nowMs + PvpObjectives.abTickMs(ownedHorde);
            }
        }
        if (nextResourceTickAt > 0 && nowMs >= nextResourceTickAt) {
            int owned = ownedAlliance + ownedHorde;
            int interval = PvpObjectives.abTickMs(owned);
            if (ownedAlliance > 0) {
                resourcesAlliance = Math.min(2000, resourcesAlliance + 10);
                emit(PvpObjectives.WS_AB_RES_A, resourcesAlliance);
            }
            if (ownedHorde > 0) {
                resourcesHorde = Math.min(2000, resourcesHorde + 10);
                emit(PvpObjectives.WS_AB_RES_H, resourcesHorde);
            }
            nextResourceTickAt = interval > 0 ? nowMs + interval : 0;
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
