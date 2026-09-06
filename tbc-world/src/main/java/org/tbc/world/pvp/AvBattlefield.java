package org.tbc.world.pvp;

import org.tbc.common.WowBuffer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Alterac Valley assault timers from battleground-av.md (TP-SL24-001). */
public final class AvBattlefield {
    public static final int TEAM_ALLIANCE = 0;
    public static final int TEAM_HORDE = 1;
    public static final int NODE_SNOWFALL = 3;
    /** battleground-av.md Vanndar / Drek'Thar. */
    public static final int NPC_VANNDAR = 11948;
    public static final int NPC_DREKTHAR = 11946;
    /** BuildPvpLogDataPacket WINNER_HORDE / ALLIANCE. */
    public static final int WINNER_HORDE = 0;
    public static final int WINNER_ALLIANCE = 1;

    private long captureReadyAt;
    private int assaultTeam = TEAM_ALLIANCE;
    private boolean snowfallFirstClaim;
    private int reinforcementsAlliance = 600;
    private int reinforcementsHorde = 600;
    private int mineTeam = -1;
    private long nextMineTickAt;
    private boolean ended;
    private int winner = 2;
    private final ArrayDeque<int[]> pendingWs = new ArrayDeque<>();

    public boolean ended() {
        return ended;
    }

    public int winner() {
        return winner;
    }

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
        if (ended) {
            return;
        }
        if (defendingTeam == TEAM_ALLIANCE) {
            reinforcementsAlliance = Math.max(0, reinforcementsAlliance - 1);
            emit(PvpObjectives.WS_AV_SCORE_A, reinforcementsAlliance);
        } else if (defendingTeam == TEAM_HORDE) {
            reinforcementsHorde = Math.max(0, reinforcementsHorde - 1);
            emit(PvpObjectives.WS_AV_SCORE_H, reinforcementsHorde);
        }
    }

    /**
     * BattleGroundAV::HandleKillUnit generals — EndBattleGround for opposing team.
     * @return true if the match just ended
     */
    public boolean onGeneralKilled(int entry) {
        if (ended) {
            return false;
        }
        if (entry == NPC_VANNDAR) {
            ended = true;
            winner = WINNER_HORDE;
            return true;
        }
        if (entry == NPC_DREKTHAR) {
            ended = true;
            winner = WINNER_ALLIANCE;
            return true;
        }
        return false;
    }

    /** MSG_PVP_LOG_DATA after EndBattleGround — type BG, ended, winner, empty scores. */
    public byte[] endedPvpLogPayload() {
        WowBuffer log = new WowBuffer(8);
        log.putU8(0);
        log.putU8(1);
        log.putU8(winner);
        log.putU32(0);
        return log.array();
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
