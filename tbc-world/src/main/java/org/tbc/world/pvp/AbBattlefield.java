package org.tbc.world.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Arathi Basin node timers / world states from battleground-ab.md.
 */
public final class AbBattlefield {
    public static final int TEAM_ALLIANCE = 0;
    public static final int TEAM_HORDE = 1;

    public static final int NODE_STABLES = 0;
    public static final int NODE_BLACKSMITH = 1;

    public static final int STATUS_NEUTRAL = 0;
    public static final int STATUS_ALLY_CONT = 1;
    public static final int STATUS_HORDE_CONT = 2;
    public static final int STATUS_ALLY_OCC = 3;
    public static final int STATUS_HORDE_OCC = 4;

    private static final int[] WS_OCC_A = {
            PvpObjectives.WS_AB_STABLES_OCC_A,
            PvpObjectives.WS_AB_BLACKSMITH_A
    };
    private static final int[] WS_OCC_H = {
            PvpObjectives.WS_AB_STABLES_OCC_H,
            PvpObjectives.WS_AB_BLACKSMITH_H
    };
    private static final int[] WS_CONT_A = {
            PvpObjectives.WS_AB_STABLES_CONT_A,
            PvpObjectives.WS_AB_BLACKSMITH_CONT_A
    };
    private static final int[] WS_CONT_H = {
            PvpObjectives.WS_AB_STABLES_CONT_H,
            PvpObjectives.WS_AB_BLACKSMITH_CONT_H
    };

    private final int[] status = new int[5];
    private final long[] readyAt = new long[5];
    private final int[] assaultTeam = new int[5];
    private int ownedAlliance;
    private int ownedHorde;
    private int resourcesAlliance;
    private int resourcesHorde;
    private long nextResourceTickAt;
    private final ArrayDeque<int[]> pendingWs = new ArrayDeque<>();

    public int stablesStatus() {
        return status[NODE_STABLES];
    }

    public int blacksmithStatus() {
        return status[NODE_BLACKSMITH];
    }

    public int nodeStatus(int node) {
        return node >= 0 && node < status.length ? status[node] : STATUS_NEUTRAL;
    }

    public int ownedAlliance() {
        return ownedAlliance;
    }

    public int resourcesAlliance() {
        return resourcesAlliance;
    }

    /** Neutral → contested 60 s (TP-SL24-002 stables / TP-SL24-004 blacksmith). */
    public void assaultStables(int team, long nowMs) {
        assaultNode(NODE_STABLES, team, nowMs);
    }

    public void assaultBlacksmith(int team, long nowMs) {
        assaultNode(NODE_BLACKSMITH, team, nowMs);
    }

    public void assaultNode(int node, int team, long nowMs) {
        if (node < 0 || node >= WS_OCC_A.length) {
            return;
        }
        if (team != TEAM_ALLIANCE && team != TEAM_HORDE) {
            return;
        }
        assaultTeam[node] = team;
        status[node] = team == TEAM_ALLIANCE ? STATUS_ALLY_CONT : STATUS_HORDE_CONT;
        readyAt[node] = nowMs + PvpObjectives.AB_CONTEST_MS;
        emit(team == TEAM_ALLIANCE ? WS_CONT_A[node] : WS_CONT_H[node], 1);
    }

    /** Contested → occupied when {@code nowMs} reaches capture deadline. */
    public void advance(long nowMs) {
        for (int node = 0; node < WS_OCC_A.length; node++) {
            if (readyAt[node] > 0 && nowMs >= readyAt[node]) {
                readyAt[node] = 0;
                if (assaultTeam[node] == TEAM_ALLIANCE) {
                    status[node] = STATUS_ALLY_OCC;
                    ownedAlliance++;
                    emit(WS_OCC_A[node], 1);
                    nextResourceTickAt = nowMs + PvpObjectives.abTickMs(ownedAlliance + ownedHorde);
                } else {
                    status[node] = STATUS_HORDE_OCC;
                    ownedHorde++;
                    emit(WS_OCC_H[node], 1);
                    nextResourceTickAt = nowMs + PvpObjectives.abTickMs(ownedAlliance + ownedHorde);
                }
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
