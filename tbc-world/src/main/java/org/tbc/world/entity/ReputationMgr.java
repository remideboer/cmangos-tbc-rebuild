package org.tbc.world.entity;

import org.tbc.common.WowBuffer;

/**
 * CMaNGOS ReputationMgr — 128 ReputationListID slots for {@code SMSG_INITIALIZE_FACTIONS}.
 * This increment seeds Alliance Stormwind only (Faction.dbc 72).
 */
public final class ReputationMgr {
    public static final int SLOTS = 128;
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
    /** Faction.dbc 72 Stormwind reputationListID. */
    public static final int LIST_STORMWIND = 19;
    /** Faction.dbc 72 ReputationFlags for Alliance race indices (VISIBLE|PEACE_FORCED). */
    public static final int STORMWIND_ALLIANCE_FLAGS = FLAG_VISIBLE | FLAG_PEACE_FORCED;
    /** ChrStatic.team Alliance / Faction.dbc Stormwind parent. */
    public static final int TEAM_ALLIANCE = 469;

    private final boolean[] occupied = new boolean[SLOTS];
    private final int[] flags = new int[SLOTS];
    private final int[] standing = new int[SLOTS];

    /** Player::Create — Stormwind visible for Alliance (Faction.dbc ReputationFlags 17). */
    public void seedCreateDefaults(int team) {
        if (team != TEAM_ALLIANCE) {
            return;
        }
        put(LIST_STORMWIND, STORMWIND_ALLIANCE_FLAGS);
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
