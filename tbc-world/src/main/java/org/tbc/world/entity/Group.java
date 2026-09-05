package org.tbc.world.entity;

import org.tbc.common.WowBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Group {
    private static final AtomicInteger NEXT = new AtomicInteger(1);
    public static final int MAX_PARTY = 5;
    public static final int MEMBER_ONLINE = 0x01;

    public long id;
    public long guid;
    public long leaderGuid;
    public long leaderLastOnlineMs;
    public boolean raid;
    public int lootMethod;
    public int lootThreshold = 2;
    public long looterGuid;
    public int difficulty;
    public final List<Player> members = new ArrayList<>();
    public final long[] icons = new long[8];
    public int instanceId;
    public int bindMap;
    public long rollLootGuid;
    public int rollSlot;
    public int rollItemId;
    public final Map<Long, Integer> rollVotes = new HashMap<>();

    public Group() {
        id = NEXT.getAndIncrement();
        guid = Guid.HIGH_GROUP | (id & 0xFFFFFFFFL);
    }

    public boolean contains(Player p) {
        return members.contains(p);
    }

    /** SMSG_GROUP_LIST for one member; that member is omitted from the rows. */
    public byte[] listFor(Player recipient) {
        WowBuffer b = new WowBuffer(128);
        b.putU8(raid ? 1 : 0);
        b.putU8(0);
        b.putU8(0);
        b.putU8(0);
        b.putU64(guid);
        int others = Math.max(0, members.size() - 1);
        b.putU32(others);
        for (Player m : members) {
            if (m == recipient) {
                continue;
            }
            b.putCString(m.name);
            b.putU64(m.guid);
            b.putU8(m.session != null ? MEMBER_ONLINE : 0);
            b.putU8(0);
            b.putU8(0);
        }
        b.putU64(leaderGuid);
        if (others != 0) {
            b.putU8(lootMethod);
            b.putU64(looterGuid);
            b.putU8(lootThreshold);
            b.putU8(difficulty);
        }
        return b.array();
    }

    /** Left / no group: three raw uint64 zeros. */
    public static byte[] emptyList() {
        WowBuffer b = new WowBuffer(24);
        b.putU64(0);
        b.putU64(0);
        b.putU64(0);
        return b.array();
    }
}
