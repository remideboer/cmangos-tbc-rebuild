package org.tbc.world.loot;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Group;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** Group loot / need-before-greed. Layout: spec/03-protocol/packets/loot.md */
public final class GroupLoot {
    public static final int METHOD_GROUP_LOOT = 3;
    public static final int METHOD_NEED_BEFORE_GREED = 4;
    public static final int TIMEOUT_MS = 60_000;
    public static final int VOTE_MASK = 0x07;
    public static final int ROLL_PASS = 0;
    public static final int ROLL_NEED = 1;
    public static final int ROLL_GREED = 2;

    private GroupLoot() {}

    public static void setMethod(Group g, int method, long looter, int threshold) {
        g.lootMethod = method;
        g.looterGuid = looter;
        g.lootThreshold = threshold;
    }

    public static boolean rolling(int method) {
        return method == METHOD_GROUP_LOOT || method == METHOD_NEED_BEFORE_GREED;
    }

    public static void start(Group g, long lootGuid, int slot, int itemId) {
        g.rollLootGuid = lootGuid;
        g.rollSlot = slot;
        g.rollItemId = itemId;
        g.rollVotes.clear();
        byte[] start = encodeStart(lootGuid, slot, itemId);
        for (Player m : g.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_LOOT_START_ROLL, start);
            }
        }
    }

    public static void vote(Player p, long lootGuid, int slot, int rollType) {
        Group g = p.group;
        if (g == null || g.rollLootGuid != lootGuid || g.rollSlot != slot) {
            return;
        }
        g.rollVotes.put(p.guid, rollType);
        byte[] roll = encodeVote(lootGuid, slot, g.rollItemId, p.guid, rollType);
        for (Player m : g.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_LOOT_ROLL, roll);
            }
        }
        if (g.rollVotes.size() < g.members.size()) {
            return;
        }
        Player winner = winner(g);
        int type = g.rollVotes.getOrDefault(winner.guid, ROLL_GREED);
        byte[] won = encodeWon(g.rollLootGuid, g.rollSlot, g.rollItemId, winner.guid, type);
        for (Player m : g.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_LOOT_ROLL_WON, won);
            }
        }
        g.rollLootGuid = 0;
        g.rollVotes.clear();
    }

    static Player winner(Group g) {
        for (Player m : g.members) {
            if (g.rollVotes.getOrDefault(m.guid, ROLL_PASS) == ROLL_NEED) {
                return m;
            }
        }
        for (Player m : g.members) {
            if (g.rollVotes.getOrDefault(m.guid, ROLL_GREED) == ROLL_GREED) {
                return m;
            }
        }
        return g.members.get(0);
    }

    static byte[] encodeStart(long lootGuid, int slot, int itemId) {
        WowBuffer b = new WowBuffer(32);
        b.putU64(lootGuid);
        b.putU32(slot);
        b.putU32(itemId);
        b.putU32(0);
        b.putU32(0);
        b.putU32(TIMEOUT_MS);
        b.putU8(VOTE_MASK);
        return b.array();
    }

    static byte[] encodeVote(long lootGuid, int slot, int itemId, long player, int rollType) {
        WowBuffer b = new WowBuffer(40);
        b.putU64(lootGuid);
        b.putU32(slot);
        b.putU64(player);
        b.putU32(itemId);
        b.putU32(0);
        b.putU32(0);
        b.putU8(rollType == ROLL_PASS ? 128 : 100);
        b.putU8(rollType);
        b.putU8(0);
        return b.array();
    }

    static byte[] encodeWon(long lootGuid, int slot, int itemId, long winner, int rollType) {
        WowBuffer b = new WowBuffer(40);
        b.putU64(lootGuid);
        b.putU32(slot);
        b.putU32(itemId);
        b.putU32(0);
        b.putU32(0);
        b.putU64(winner);
        b.putU8(100);
        b.putU8(rollType);
        return b.array();
    }
}
