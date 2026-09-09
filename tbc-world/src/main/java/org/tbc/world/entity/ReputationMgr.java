package org.tbc.world.entity;

import org.tbc.common.WowBuffer;

/**
 * CMaNGOS ReputationMgr — 128 ReputationListID slots for {@code SMSG_INITIALIZE_FACTIONS}.
 * Seeds Alliance Stormwind (Faction.dbc 72) and Booty Bay (21).
 */
public final class ReputationMgr {
    public static final int SLOTS = 128;
    /** ReputationMgr.cpp PointsInRank / ReputationToRank. */
    private static final int[] POINTS_IN_RANK = {36000, 3000, 3000, 3000, 6000, 12000, 21000, 1000};
    private static final int REPUTATION_CAP = 42999;
    /** SharedDefines.h REP_HATED. */
    public static final int REP_HATED = 0;
    /** ReputationMgr.h FACTION_FLAG_VISIBLE. */
    public static final int FLAG_VISIBLE = 0x01;
    /** ReputationMgr.h FACTION_FLAG_AT_WAR. */
    public static final int FLAG_AT_WAR = 0x02;
    /** ReputationMgr.h FACTION_FLAG_HIDDEN. */
    public static final int FLAG_HIDDEN = 0x04;
    /** ReputationMgr.h FACTION_FLAG_INVISIBLE_FORCED. */
    public static final int FLAG_INVISIBLE_FORCED = 0x08;
    /** ReputationMgr.h FACTION_FLAG_PEACE_FORCED. */
    public static final int FLAG_PEACE_FORCED = 0x10;
    /** ReputationMgr.h FACTION_FLAG_INACTIVE. */
    public static final int FLAG_INACTIVE = 0x20;
    /** Faction.dbc 21 Booty Bay reputationListID. */
    public static final int LIST_BOOTY_BAY = 1;
    /** Faction.dbc 72 Stormwind reputationListID. */
    public static final int LIST_STORMWIND = 19;
    /** Faction.dbc 72 ReputationFlags for Alliance race indices (VISIBLE|PEACE_FORCED). */
    public static final int STORMWIND_ALLIANCE_FLAGS = FLAG_VISIBLE | FLAG_PEACE_FORCED;
    /** ChrStatic.team Alliance / Faction.dbc Stormwind parent. */
    public static final int TEAM_ALLIANCE = 469;

    private final boolean[] occupied = new boolean[SLOTS];
    private final int[] flags = new int[SLOTS];
    private final int[] standing = new int[SLOTS];

    /**
     * Player::Create / ReputationMgr::Initialize subset.
     * Booty Bay flags 0 (Faction.dbc); Stormwind VISIBLE|PEACE_FORCED for Alliance.
     */
    public void seedCreateDefaults(int team) {
        put(LIST_BOOTY_BAY, 0);
        if (team == TEAM_ALLIANCE) {
            put(LIST_STORMWIND, STORMWIND_ALLIANCE_FLAGS);
        }
    }

    public void put(int listId, int factionFlags) {
        if (listId < 0 || listId >= SLOTS) {
            return;
        }
        occupied[listId] = true;
        flags[listId] = factionFlags;
    }

    /** ReputationMgr::SetInactive — no SMSG; next login burst carries the flag. */
    public void setInactive(int listId, boolean inactive) {
        if (listId < 0 || listId >= SLOTS || !occupied[listId]) {
            return;
        }
        int f = flags[listId];
        if (inactive && ((f & (FLAG_INVISIBLE_FORCED | FLAG_HIDDEN)) != 0 || (f & FLAG_VISIBLE) == 0)) {
            return;
        }
        if (((f & FLAG_INACTIVE) != 0) == inactive) {
            return;
        }
        flags[listId] = inactive ? f | FLAG_INACTIVE : f & ~FLAG_INACTIVE;
    }

    /** ReputationMgr::SetAtWar — no SMSG; next login burst carries the flag. */
    public void setAtWar(int listId, boolean atWar) {
        if (listId < 0 || listId >= SLOTS || !occupied[listId]) {
            return;
        }
        int f = flags[listId];
        if ((f & (FLAG_INVISIBLE_FORCED | FLAG_HIDDEN)) != 0) {
            return;
        }
        if (atWar && (f & FLAG_PEACE_FORCED) != 0 && reputationToRank(standing[listId]) > REP_HATED) {
            return;
        }
        if (((f & FLAG_AT_WAR) != 0) && atWar) {
            return;
        }
        flags[listId] = atWar ? f | FLAG_AT_WAR : f & ~FLAG_AT_WAR;
    }

    /** FactionState.Standing overlay (ReputationMgr::SetAtWar uses this, not base+standing). */
    public void setStanding(int listId, int amount) {
        if (listId < 0 || listId >= SLOTS || !occupied[listId]) {
            return;
        }
        standing[listId] = amount;
    }

    static int reputationToRank(int standing) {
        int limit = REPUTATION_CAP + 1;
        for (int i = POINTS_IN_RANK.length - 1; i >= 0; i--) {
            limit -= POINTS_IN_RANK[i];
            if (standing >= limit) {
                return i;
            }
        }
        return REP_HATED;
    }

    public int flags(int listId) {
        if (listId < 0 || listId >= SLOTS) {
            return 0;
        }
        return flags[listId];
    }

    public void copyFrom(ReputationMgr src) {
        System.arraycopy(src.occupied, 0, occupied, 0, SLOTS);
        System.arraycopy(src.flags, 0, flags, 0, SLOTS);
        System.arraycopy(src.standing, 0, standing, 0, SLOTS);
    }

    /** ReputationMgr::SendInitialReputations — count 0x80 then 128 × (flags u8, standing u32). */
    public void writeInitial(WowBuffer out) {
        out.putU32(SLOTS);
        for (int i = 0; i < SLOTS; i++) {
            out.putU8(flags[i]);
            out.putU32(standing[i]);
        }
    }
}
