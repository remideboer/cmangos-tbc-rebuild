package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.loot.GroupLoot;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.List;

/** Corpse loot take, group loot method and rolls. Layout: spec/03-protocol/packets/loot.md */
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

    public static void autostoreLootItem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 1) {
            return;
        }
        int slot = in.getU8();
        Player p = s.player();
        Creature c = world.map(p.mapId, p.instanceId).creatures.get(p.lootGuid);
        List<ObjectMgr.StoredItem> stored =
                world.combat.takeItem(p, c, slot, world::nextItemGuid, world.objectMgr);
        if (stored.isEmpty()) {
            return;
        }
        Item it = stored.get(0).item();
        int added = 0;
        int total = 0;
        for (ObjectMgr.StoredItem st : stored) {
            added += st.added();
        }
        for (Item x : p.items.values()) {
            if (x.entry == it.entry) {
                total += x.count;
            }
        }
        boolean anyCreated = false;
        for (ObjectMgr.StoredItem st : stored) {
            if (st.created()) {
                anyCreated = true;
                var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(st.item(), p.guid));
                s.send(created.opcode(), created.payload());
                int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + st.item().slot * 2;
                var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
                s.send(inv.opcode(), inv.payload());
            } else {
                var stack = UpdateBuilder.maybeCompress(
                        UpdateBuilder.valuesItem(st.item(), UpdateFields.ITEM_FIELD_STACK_COUNT));
                s.send(stack.opcode(), stack.payload());
            }
        }
        s.send(Opcodes.SMSG_LOOT_REMOVED, world.combat.encodeLootRemoved(slot));
        int pushSlot = anyCreated ? stored.get(stored.size() - 1).item().slot : 0xFFFFFFFF;
        s.send(Opcodes.SMSG_ITEM_PUSH_RESULT,
                Content.encodePush(p, it, added, 0, total, pushSlot));
        world.content.itemAddedQuestCheck(p, world.map(p.mapId, p.instanceId), it.entry, added, s::send);
        maybeReleaseEmptyCorpse(s, world, p, c);
    }

    public static void lootMoney(WorldSession s, World world) {
        Player p = s.player();
        Creature c = world.map(p.mapId, p.instanceId).creatures.get(p.lootGuid);
        if (!world.combat.takeMoney(p, c)) {
            return;
        }
        s.send(Opcodes.SMSG_LOOT_CLEAR_MONEY, new byte[0]);
        maybeReleaseEmptyCorpse(s, world, p, c);
    }

    /**
     * Loot::SendItem / SendGold → IsLootedForAll → SendReleaseFor + ForceLootAnimationClientUpdate
     * (clear UNIT_DYNFLAG_LOOTABLE).
     */
    static void maybeReleaseEmptyCorpse(WorldSession s, World world, Player p, Creature c) {
        if (c == null || c.lootable) {
            return;
        }
        p.lootGuid = 0;
        s.send(Opcodes.SMSG_LOOT_RELEASE_RESPONSE, world.combat.encodeLootRelease(c.guid));
        world.sendLootableFlags(c, p.instanceId);
        // SetLootStatus(LOOTED) without skinning → ReduceCorpseDecayTimer (2 min).
        world.combat.reduceCorpseDecayTimer(c, world.nowMs());
    }

    public static void maybeStartRoll(Player p, Creature c, long guid) {
        if (c != null && p.group != null && GroupLoot.rolling(p.group.lootMethod)) {
            GroupLoot.start(p.group, guid, 0, Content.ITEM_WORN_SHORTSWORD);
        }
    }
}
