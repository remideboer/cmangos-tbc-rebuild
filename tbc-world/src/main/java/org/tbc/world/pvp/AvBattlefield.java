package org.tbc.world.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Alterac Valley assault timers from battleground-av.md (TP-SL24-001). */
public final class AvBattlefield {
    public static final int TEAM_ALLIANCE = 0;
    public static final int NODE_SNOWFALL = 3;

    private long captureReadyAt;
    private int assaultTeam = TEAM_ALLIANCE;
    private boolean snowfallFirstClaim;
    private int reinforcementsAlliance = 600;
    private final ArrayDeque<int[]> pendingWs = new ArrayDeque<>();

    public long captureReadyAt() {
        return captureReadyAt;
    }

    public int captureDurationMs() {
        return PvpObjectives.avCaptureMs(snowfallFirstClaim);
    }

    public int reinforcementsAlliance() {
        return reinforcementsAlliance;
    }

    /**
     * Assault a GY banner. Snowfall first claim uses 300000 ms; others 240000 ms.
     */
    public void assaultGraveyard(int nodeId, int team, boolean snowfallWasNeutral, long nowMs) {
        assaultTeam = team;
        snowfallFirstClaim = nodeId == NODE_SNOWFALL && snowfallWasNeutral;
        captureReadyAt = nowMs + captureDurationMs();
    }

    public void advance(long nowMs) {
        if (captureReadyAt > 0 && nowMs >= captureReadyAt) {
            captureReadyAt = 0;
            emit(PvpObjectives.WS_AV_SCORE_A, reinforcementsAlliance);
            emit(PvpObjectives.WS_AV_SCORE_H, 600);
        }
    }

    public void onPlayerDeath(int defendingTeam) {
        if (defendingTeam == TEAM_ALLIANCE) {
            reinforcementsAlliance = Math.max(0, reinforcementsAlliance - 1);
            emit(PvpObjectives.WS_AV_SCORE_A, reinforcementsAlliance);
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
