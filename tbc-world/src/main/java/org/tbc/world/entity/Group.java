package org.tbc.world.entity;

import java.util.ArrayList;
import java.util.List;

public final class Group {
    public long id;
    public long leaderGuid;
    public boolean raid;
    public int lootMethod;
    public int lootThreshold = 2;
    public long looterGuid;
    public int difficulty;
    public final List<Player> members = new ArrayList<>();
    public final long[] icons = new long[8];
    public int instanceId;
    public int bindMap;

    public boolean contains(Player p) {
        return members.contains(p);
    }
}
