package org.tbc.world.combat;

/** FactionTemplate.dbc helpers. spec/03-protocol/dbc-files.md; CMaNGOS DBCStructure.h */
public final class FactionTemplate {
    public static final int GROUP_PLAYER = 1;
    public static final int GROUP_ALLIANCE = 2;
    public static final int GROUP_HORDE = 4;
    public static final int GROUP_MONSTER = 8;

    /** DBC columns ID / faction / factionFlags. */
    public record Identity(int id, int faction, int flags) {}

    /** DBC columns ourMask / friendlyMask / hostileMask. */
    public record GroupMasks(int own, int friend, int enemy) {}

    /** DBC columns enemyFaction[4] / friendFaction[4]. Null arrays become empty length-4 slots. */
    public record FactionLists(int[] enemies, int[] friends) {
        public FactionLists {
            enemies = enemies == null ? new int[4] : enemies;
            friends = friends == null ? new int[4] : friends;
        }

        public static FactionLists empty() {
            return new FactionLists(new int[4], new int[4]);
        }
    }

    public final int id;
    public final int faction;
    public final int factionFlags;
    public final int factionGroupMask;
    public final int friendGroupMask;
    public final int enemyGroupMask;
    public final int[] enemyFaction;
    public final int[] friendFaction;

    public FactionTemplate(Identity identity, GroupMasks groups, FactionLists lists) {
        this.id = identity.id();
        this.faction = identity.faction();
        this.factionFlags = identity.flags();
        this.factionGroupMask = groups.own();
        this.friendGroupMask = groups.friend();
        this.enemyGroupMask = groups.enemy();
        this.enemyFaction = lists.enemies();
        this.friendFaction = lists.friends();
    }

    /** CMaNGOS ReputationRank; 8606 target frame: HOSTILE=red, NEUTRAL=yellow, FRIENDLY=green. */
    public static final int REP_HOSTILE = 1;
    public static final int REP_UNFRIENDLY = 2;
    public static final int REP_NEUTRAL = 3;
    public static final int REP_FRIENDLY = 4;

    /**
     * CMaNGOS GetFactionReaction / CGUnit_C::UnitReaction (dbc-files.md group + id lists).
     * This is what paints the 8606 target frame.
     */
    public int reactionTo(FactionTemplate other) {
        if (other == null) {
            return REP_NEUTRAL;
        }
        if ((other.factionGroupMask & enemyGroupMask) != 0) {
            return REP_HOSTILE;
        }
        if (enemyFaction[0] != 0 && other.faction != 0) {
            for (int id : enemyFaction) {
                if (id == other.faction) {
                    return REP_HOSTILE;
                }
            }
        }
        if ((other.factionGroupMask & friendGroupMask) != 0) {
            return REP_FRIENDLY;
        }
        if (friendFaction[0] != 0 && other.faction != 0) {
            for (int id : friendFaction) {
                if (id == other.faction) {
                    return REP_FRIENDLY;
                }
            }
        }
        if ((factionGroupMask & other.friendGroupMask) != 0) {
            return REP_FRIENDLY;
        }
        if (other.friendFaction[0] != 0 && faction != 0) {
            for (int id : other.friendFaction) {
                if (id == faction) {
                    return REP_FRIENDLY;
                }
            }
        }
        return REP_NEUTRAL;
    }

    public boolean isHostileTo(FactionTemplate other) {
        return reactionTo(other) < REP_UNFRIENDLY;
    }

    public boolean isHostileToPlayers() {
        return (enemyGroupMask & GROUP_PLAYER) != 0;
    }

    public boolean isNeutralToAll() {
        for (int id : enemyFaction) {
            if (id != 0) {
                return false;
            }
        }
        return enemyGroupMask == 0 && friendGroupMask == 0;
    }
}
