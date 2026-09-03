package org.tbc.world.pvp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Alterac Valley assault timers from battleground-av.md (TP-SL24-001). */
public final class AvBattlefield {
    public static final int TEAM_ALLIANCE = 0;
    public static final int TEAM_HORDE = 1;
    public static final int NODE_SNOWFALL = 3;

    private long captureReadyAt;
    private int assaultTeam = TEAM_ALLIANCE;
    private boolean snowfallFirstClaim;
    private int reinforcementsAlliance = 600;
    private int reinforcementsHorde = 600;
    private int mineTeam = -1;
    private long nextMineTickAt;
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

    /** Irondeep/Coldtooth claim — +1 reinforcements every 45000 ms (battleground-av.md). */
    public void claimMine(int team, long nowMs) {
        mineTeam = team;
        nextMineTickAt = nowMs + PvpObjectives.AV_MINE_TICK_MS;
    }

    public void advance(long nowMs) {
        if (captureReadyAt > 0 && nowMs >= captureReadyAt) {
            captureReadyAt = 0;
            emit(PvpObjectives.WS_AV_SCORE_A, reinforcementsAlliance);
            emit(PvpObjectives.WS_AV_SCORE_H, reinforcementsHorde);
        }
        if (nextMineTickAt > 0 && nowMs >= nextMineTickAt) {
            if (mineTeam == TEAM_ALLIANCE) {
                reinforcementsAlliance++;
                emit(PvpObjectives.WS_AV_SCORE_A, reinforcementsAlliance);
            } else if (mineTeam == TEAM_HORDE) {
                reinforcementsHorde++;
                emit(PvpObjectives.WS_AV_SCORE_H, reinforcementsHorde);
            }
            nextMineTickAt = nowMs + PvpObjectives.AV_MINE_TICK_MS;
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
