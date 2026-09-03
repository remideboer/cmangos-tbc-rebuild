package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.loot.GroupLoot;
import org.tbc.world.net.wow8606.Opcodes;

/** Group loot method and rolls. Layout: spec/03-protocol/packets/loot.md */
public final class LootHandler {
    private LootHandler() {}

    public static void lootMethod(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (p.group == null || p.group.leaderGuid != p.guid || in.remaining() < 16) {
            return;
        }
        int method = in.getU32();
        long looter = in.getU64();
        int threshold = in.getU32();
        GroupLoot.setMethod(p.group, method, looter, threshold);
        for (Player m : p.group.members) {
            if (m.session != null) {
                m.session.send(Opcodes.SMSG_GROUP_LIST, p.group.listFor(m));
            }
        }
    }

    public static void lootRoll(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long lootGuid = in.remaining() >= 8 ? in.getU64() : 0;
        int slot = in.remaining() >= 4 ? in.getU32() : 0;
        int type = in.remaining() > 0 ? in.getU8() : GroupLoot.ROLL_PASS;
        GroupLoot.vote(p, lootGuid, slot, type);
    }

    public static void maybeStartRoll(Player p, Creature c, long guid) {
        if (c != null && p.group != null && GroupLoot.rolling(p.group.lootMethod)) {
            GroupLoot.start(p.group, guid, 0, Content.ITEM_WORN_SHORTSWORD);
        }
    }
}
