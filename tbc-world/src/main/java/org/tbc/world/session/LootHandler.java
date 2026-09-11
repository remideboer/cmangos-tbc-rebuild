package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.loot.GroupLoot;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

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
        Item it = world.combat.takeItem(p, c, slot, world.nextItemGuid());
        if (it == null) {
            return;
        }
        int total = 0;
        for (Item x : p.items.values()) {
            if (x.entry == it.entry) {
                total += x.count;
            }
        }
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        s.send(created.opcode(), created.payload());
        var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        s.send(inv.opcode(), inv.payload());
        s.send(Opcodes.SMSG_LOOT_REMOVED, world.combat.encodeLootRemoved(slot));
        s.send(Opcodes.SMSG_ITEM_PUSH_RESULT, Content.encodeLootPush(p, it, total));
        world.content.itemAddedQuestCheck(p, it.entry, it.count, s::send);
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
    }

    public static void maybeStartRoll(Player p, Creature c, long guid) {
        if (c != null && p.group != null && GroupLoot.rolling(p.group.lootMethod)) {
            GroupLoot.start(p.group, guid, 0, Content.ITEM_WORN_SHORTSWORD);
        }
    }
}
