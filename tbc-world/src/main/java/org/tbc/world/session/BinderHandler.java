package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.catalog.ItemCatalog;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.Terrain;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/** CMSG_BINDER_ACTIVATE. Layout: spec/03-protocol/packets/misc-player.md */
public final class BinderHandler {
    /** NPCHandler.cpp SendBindPoint spell 3286 Bind (EffectBind + EffectCreateItem 6948). */
    public static final int SPELL_BIND = 3286;

    private BinderHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_BINDER_ACTIVATE, BinderHandler::activate);
    }

    public static void activate(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (p == null) {
            return;
        }
        activate(p, world.map(p.mapId, p.instanceId), world.terrain, in, s::send,
                world::nextItemGuid, world.objectMgr);
    }

    public static void activate(Player p, GameMap map, Terrain terrain, WowBuffer in,
                                BiConsumer<Integer, byte[]> send, LongSupplier nextItemGuid,
                                ItemCatalog mgr) {
        if (!p.alive()) {
            return;
        }
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(map, guid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_INNKEEPER) == 0) {
            return;
        }
        sendBindPoint(p, npc, terrain, send, nextItemGuid, mgr);
    }

    /** NPCHandler.cpp SendBindPoint → CastSpell 3286 (EffectBind + EffectCreateItem). */
    static void sendBindPoint(Player p, Creature npc, Terrain terrain, BiConsumer<Integer, byte[]> send,
                              LongSupplier nextItemGuid, ItemCatalog mgr) {
        int areaId = terrain == null ? 0 : terrain.area(p.mapId, p.x, p.y);
        p.setHomebindToLocation(p.mapId, areaId, p.x, p.y, p.z);
        WowBuffer bind = new WowBuffer(20);
        bind.putFloat(p.bindX);
        bind.putFloat(p.bindY);
        bind.putFloat(p.bindZ);
        bind.putU32(p.bindMap);
        bind.putU32(areaId);
        send.accept(Opcodes.SMSG_BINDPOINTUPDATE, bind.array());
        WowBuffer bound = new WowBuffer(12);
        bound.putU64(npc.guid);
        bound.putU32(areaId);
        send.accept(Opcodes.SMSG_PLAYERBOUND, bound.array());
        WowBuffer bought = new WowBuffer(12);
        bought.putU64(npc.guid);
        bought.putU32(SPELL_BIND);
        send.accept(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED, bought.array());
        send.accept(Opcodes.SMSG_GOSSIP_COMPLETE, new byte[0]);
        createHearthstoneIfMissing(p, nextItemGuid, mgr, send);
    }

    /**
     * Spell 3286 EffectCreateItem — Hearthstone 6948 (MaxCount 1).
     * CMaNGOS DoCreateItem / CanStoreNewItem skips when unique already owned.
     */
    static void createHearthstoneIfMissing(Player p, LongSupplier nextItemGuid, ItemCatalog mgr,
                                           BiConsumer<Integer, byte[]> send) {
        if (p == null || nextItemGuid == null || send == null) {
            return;
        }
        if (hasItemEntry(p, Content.ITEM_HEARTHSTONE)) {
            return;
        }
        int slot = p.firstFreeBagSlot();
        if (slot < 0) {
            return;
        }
        long itemGuid = nextItemGuid.getAsLong();
        if (itemGuid == 0) {
            return;
        }
        Item it = new Item(itemGuid, Content.ITEM_HEARTHSTONE);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = slot;
        it.count = 1;
        if (mgr != null) {
            ObjectMgr.ItemTemplate t = mgr.item(Content.ITEM_HEARTHSTONE);
            if (t != null) {
                it.displayId = t.displayId;
                it.quality = t.quality;
            }
        }
        p.items.put(Guid.low(it.guid), it);
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + slot * 2, UpdateBuilder.itemGuid(it));
        p.dirty = true;
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        send.accept(created.opcode(), created.payload());
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        var inv = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        send.accept(inv.opcode(), inv.payload());
        // Push proto with ON_USE 8690 — same as LoginBurst.sendInventory item-query sync.
        if (mgr != null) {
            ObjectMgr.ItemTemplate proto = mgr.item(Content.ITEM_HEARTHSTONE);
            if (proto != null) {
                send.accept(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, QueryHandler.encodeItemQuery(proto));
            }
        }
        send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodeCreateItemPush(p, it));
    }

    /** Same layout as Content.encodePush(received=1) for spell create-item. */
    static byte[] encodeCreateItemPush(Player p, Item it) {
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(1);
        b.putU32(0);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(it.slot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(it.count);
        b.putU32(it.count);
        return b.array();
    }

    static boolean hasItemEntry(Player p, int entry) {
        for (Item it : p.items.values()) {
            if (it.entry == entry) {
                return true;
            }
        }
        return false;
    }
}
