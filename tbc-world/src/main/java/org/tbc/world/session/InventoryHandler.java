package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.DurabilityCosts;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.AuraSlots;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;

import java.util.Arrays;
import java.util.function.BiConsumer;

/** Bag 0 swap. Layout: spec/03-protocol/packets/inventory.md */
public final class InventoryHandler {
    private InventoryHandler() {}

    /**
     * Official 8606 client sends {@link Player#INVENTORY_SLOT_BAG_0} (255) for paper-doll / backpack.
     * Server storage uses bag 0; leave real bag indices (1–4) unchanged.
     */
    static int bagIndex(int bag) {
        return bag == Player.INVENTORY_SLOT_BAG_0 ? 0 : bag;
    }

    /** Slot 0 is Battle Stance on warriors; ON_EQUIP / unequip must push later slots and attack-power mods. */
    private static int[] withAuras(Player p, int... fields) {
        int[] extra = AuraSlots.paperDollAuraFields(p);
        // POS/NEG schools 1..6 (holy..arcane) — createUnit omits zeros; VALUES must clear $SPH after unequip.
        int spellDone = 12;
        int[] all = Arrays.copyOf(fields, fields.length + extra.length + 1 + spellDone);
        System.arraycopy(extra, 0, all, fields.length, extra.length);
        int i = fields.length + extra.length;
        all[i++] = UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS;
        for (int school = 1; school <= 6; school++) {
            all[i++] = UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + school;
            all[i++] = UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG + school;
        }
        return all;
    }

    /** After bag0 slot moves: refresh paper-doll visuals for any equipment slots touched. */
    private static int[] withEquipVisuals(Player p, int srcSlot, int dstSlot, int... fields) {
        boolean srcEquip = srcSlot >= 0 && srcSlot < Player.EQUIPMENT_SLOT_END;
        boolean dstEquip = dstSlot >= 0 && dstSlot < Player.EQUIPMENT_SLOT_END;
        if (srcEquip) {
            p.setVisibleItemSlot(srcSlot, p.itemAt(0, srcSlot));
        }
        if (dstEquip) {
            p.setVisibleItemSlot(dstSlot, p.itemAt(0, dstSlot));
        }
        if (!srcEquip && !dstEquip) {
            return fields;
        }
        p.refreshSheath();
        int extra = (srcEquip ? 1 : 0) + (dstEquip ? 1 : 0) + 1;
        int[] all = Arrays.copyOf(fields, fields.length + extra);
        int i = fields.length;
        if (srcEquip) {
            all[i++] = Player.visibleItemEntryField(srcSlot);
        }
        if (dstEquip) {
            all[i++] = Player.visibleItemEntryField(dstSlot);
        }
        all[i] = UpdateFields.UNIT_FIELD_BYTES_2;
        return all;
    }

    public static void swapInvItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int src = in.getU8();
        int dst = in.getU8();
        if (src == dst) {
            return;
        }
        Item a = p.itemAt(0, src);
        Item b = p.itemAt(0, dst);
        if (tryMergeStacks(s, world, a, b, src, dst)) {
            return;
        }
        if (a != null) {
            a.slot = dst;
        }
        if (b != null) {
            b.slot = src;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + src * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        p.setGuid(srcField, b == null ? 0 : UpdateBuilder.itemGuid(b));
        p.setGuid(dstField, a == null ? 0 : UpdateBuilder.itemGuid(a));
        if (world != null) {
            world.objectMgr.applyEquippedMelee(p);
        }
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, withAuras(p, withEquipVisuals(p, src, dst,
                        srcField, srcField + 1, dstField, dstField + 1,
                        UpdateFields.UNIT_FIELD_MINDAMAGE, UpdateFields.UNIT_FIELD_MAXDAMAGE,
                        UpdateFields.UNIT_FIELD_BASEATTACKTIME,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_MAXPOWER1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 2,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 3,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 4,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 5,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23,
                        UpdateFields.PLAYER_SHIELD_BLOCK,
                        UpdateFields.UNIT_FIELD_AURA))));
        s.send(pkt.opcode(), pkt.payload());
    }

    /**
     * CMaNGOS Player::SwapItem merge/fill when both stacks share an entry under max stack.
     * @return true when the swap was fully handled as a merge
     */
    private static boolean tryMergeStacks(WorldSession s, World world, Item src, Item dst,
                                          int srcSlot, int dstSlot) {
        if (src == null || dst == null || src.entry != dst.entry || world == null) {
            return false;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(src.entry);
        if (t == null || t.stackable <= 1) {
            return false;
        }
        int max = t.stackable;
        if (src.count + dst.count <= max) {
            dst.count += src.count;
            Player p = s.player();
            p.items.remove(Guid.low(src.guid));
            int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
            p.setGuid(srcField, 0);
            var destroyed = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, srcField, srcField + 1));
            s.send(destroyed.opcode(), destroyed.payload());
            var stack = UpdateBuilder.maybeCompress(
                    UpdateBuilder.valuesItem(dst, UpdateFields.ITEM_FIELD_STACK_COUNT));
            s.send(stack.opcode(), stack.payload());
            return true;
        }
        if (dst.count < max) {
            int move = max - dst.count;
            dst.count = max;
            src.count -= move;
            var dstStack = UpdateBuilder.maybeCompress(
                    UpdateBuilder.valuesItem(dst, UpdateFields.ITEM_FIELD_STACK_COUNT));
            s.send(dstStack.opcode(), dstStack.payload());
            var srcStack = UpdateBuilder.maybeCompress(
                    UpdateBuilder.valuesItem(src, UpdateFields.ITEM_FIELD_STACK_COUNT));
            s.send(srcStack.opcode(), srcStack.payload());
            return true;
        }
        return false;
    }
    public static void swapItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int dstBag = bagIndex(in.getU8());
        int dstSlot = in.getU8();
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        if (srcBag == dstBag && srcSlot == dstSlot) {
            return;
        }
        Item a = p.itemAt(srcBag, srcSlot);
        Item b = p.itemAt(dstBag, dstSlot);
        if (srcBag == 0 && dstBag == 0 && tryMergeStacks(s, world, a, b, srcSlot, dstSlot)) {
            return;
        }
        if (a != null) {
            a.bag = dstBag;
            a.slot = dstSlot;
        }
        if (b != null) {
            b.bag = srcBag;
            b.slot = srcSlot;
        }
        if (srcBag != 0 || dstBag != 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dstSlot * 2;
        p.setGuid(srcField, b == null ? 0 : UpdateBuilder.itemGuid(b));
        p.setGuid(dstField, a == null ? 0 : UpdateBuilder.itemGuid(a));
        if (world != null) {
            world.objectMgr.applyEquippedMelee(p);
        }
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, withAuras(p, withEquipVisuals(p, srcSlot, dstSlot,
                        srcField, srcField + 1, dstField, dstField + 1,
                        UpdateFields.UNIT_FIELD_MINDAMAGE, UpdateFields.UNIT_FIELD_MAXDAMAGE,
                        UpdateFields.UNIT_FIELD_BASEATTACKTIME,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_MAXPOWER1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 2,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 3,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 4,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 5,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23,
                        UpdateFields.PLAYER_SHIELD_BLOCK,
                        UpdateFields.UNIT_FIELD_AURA))));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** bag, slot, count (0 = whole stack). Layout: spec/03-protocol/packets/inventory.md */
    public static void destroyItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 3) {
            return;
        }
        int bag = bagIndex(in.getU8());
        int slot = in.getU8();
        in.getU8();
        Item it = p.itemAt(bag, slot);
        if (it == null) {
            return;
        }
        p.items.remove((int) it.guid);
        int destroyedSlot = it.slot;
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        p.setGuid(field, 0);
        if (world != null) {
            world.objectMgr.applyEquippedMelee(p);
        }
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, withAuras(p, withEquipVisuals(p, destroyedSlot, -1,
                        field, field + 1,
                        UpdateFields.UNIT_FIELD_MINDAMAGE, UpdateFields.UNIT_FIELD_MAXDAMAGE,
                        UpdateFields.UNIT_FIELD_BASEATTACKTIME,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_MAXPOWER1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 2,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 3,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 4,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 5,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23,
                        UpdateFields.PLAYER_SHIELD_BLOCK,
                        UpdateFields.UNIT_FIELD_AURA))));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot, dstbag, dstslot, count. count 0 or same pos: ignore. inventory.md */
    public static void splitItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 5) {
            return;
        }
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        int dstBag = bagIndex(in.getU8());
        int dstSlot = in.getU8();
        int count = in.getU8();
        if (count == 0 || srcBag != 0 || dstBag != 0 || srcSlot == dstSlot) {
            return;
        }
        Item src = p.itemAt(srcBag, srcSlot);
        if (src == null || src.count <= count || p.itemAt(dstBag, dstSlot) != null) {
            return;
        }
        src.count -= count;
        Item split = new Item(world.nextItemGuid(), src.entry);
        split.ownerGuid = Guid.low(p.guid);
        split.bag = dstBag;
        split.slot = dstSlot;
        split.count = count;
        p.items.put((int) split.guid, split);
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dstSlot * 2;
        p.setGuid(dstField, UpdateBuilder.itemGuid(split));
        var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(split, p.guid));
        s.send(created.opcode(), created.payload());
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** guid raw banker. Layout: spec/03-protocol/packets/inventory.md */
    public static void bankerActivate(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc)) {
            return;
        }
        sendShowBank(npc, s::send);
    }

    public static void sendShowBank(Creature c, BiConsumer<Integer, byte[]> send) {
        if ((c.npcFlags & Content.UNIT_NPC_FLAG_BANKER) == 0) {
            return;
        }
        WowBuffer shown = new WowBuffer(8);
        shown.putU64(c.guid);
        send.accept(Opcodes.SMSG_SHOW_BANK, shown.array());
    }

    /** guid raw banker. CMaNGOS HandleBuyBankSlotOpcode. inventory.md */
    public static void buyBankSlot(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (npc == null || Content.outOfRange(p, npc) || (npc.npcFlags & Content.UNIT_NPC_FLAG_BANKER) == 0) {
            return;
        }
        int slot = p.bankBagSlotCount() + 1;
        if (slot > Player.BANK_SLOT_BAG_END - Player.BANK_SLOT_BAG_START) {
            return;
        }
        int price = Content.BANK_BAG_SLOT_PRICES[slot];
        if (p.money < price) {
            sendBuyBankSlotResult(s, Content.ERR_BANKSLOT_INSUFFICIENT_FUNDS);
            return;
        }
        p.setBankBagSlotCount(slot);
        p.setMoney(p.money - price);
        sendBuyBankSlotResult(s, Content.ERR_BANKSLOT_OK);
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                p, UpdateFields.PLAYER_BYTES_2, UpdateFields.PLAYER_FIELD_COINAGE));
        s.send(pkt.opcode(), pkt.payload());
    }

    private static void sendBuyBankSlotResult(WorldSession s, int result) {
        WowBuffer out = new WowBuffer(4);
        out.putU32(result);
        s.send(Opcodes.SMSG_BUY_BANK_SLOT_RESULT, out.array());
    }

    /** srcbag, srcslot. Always inventory → bank. inventory.md */
    public static void autobankItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        if (srcBag != 0 || srcSlot >= Player.BANK_SLOT_ITEM_START) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        int dst = p.firstFreeBankSlot();
        if (it == null || dst < 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        it.slot = dst;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot. Bank pos → inventory; else → bank. inventory.md */
    public static void autostoreBankItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        if (srcBag != 0) {
            return;
        }
        int dst;
        if (srcSlot >= Player.BANK_SLOT_ITEM_START && srcSlot < Player.BANK_SLOT_ITEM_END) {
            dst = p.firstFreeBagSlot();
        } else {
            dst = p.firstFreeBankSlot();
        }
        Item it = p.itemAt(srcBag, srcSlot);
        if (it == null || dst < 0) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dst * 2;
        it.slot = dst;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, srcField, srcField + 1, dstField, dstField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** srcbag, srcslot. CanEquipItem then equip or swap. inventory.md */
    public static void autoequipItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        if (srcBag != 0) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        if (it == null) {
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        int invType = t != null ? t.inventoryType : it.inventoryType;
        int dest = world.objectMgr.destEquipSlot(p, invType);
        if (dest < 0 || dest == srcSlot) {
            return;
        }
        Item occupied = p.itemAt(0, dest);
        it.slot = dest;
        if (occupied != null) {
            occupied.slot = srcSlot;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dest * 2;
        p.setGuid(srcField, occupied == null ? 0 : UpdateBuilder.itemGuid(occupied));
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        world.objectMgr.applyEquippedMelee(p);
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, withAuras(p, withEquipVisuals(p, srcSlot, dest,
                        srcField, srcField + 1, dstField, dstField + 1,
                        UpdateFields.UNIT_FIELD_MINDAMAGE, UpdateFields.UNIT_FIELD_MAXDAMAGE,
                        UpdateFields.UNIT_FIELD_BASEATTACKTIME,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_MAXPOWER1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 2,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 3,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 4,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 5,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23,
                        UpdateFields.PLAYER_SHIELD_BLOCK,
                        UpdateFields.UNIT_FIELD_AURA))));
        s.send(pkt.opcode(), pkt.payload());
        if (dest >= Player.INVENTORY_SLOT_BAG_START && dest < Player.INVENTORY_SLOT_BAG_END) {
            WowBuffer opened = new WowBuffer(8);
            opened.putU64(UpdateBuilder.itemGuid(it));
            s.send(Opcodes.SMSG_OPEN_CONTAINER, opened.array());
        }
    }

    /** srcbag, srcslot, dstbag. Store into a free slot of dstbag. inventory.md */
    public static void autostoreBagItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 3) {
            return;
        }
        int srcBag = bagIndex(in.getU8());
        int srcSlot = in.getU8();
        int dstBag = bagIndex(in.getU8());
        if (srcBag != 0 || dstBag != 0) {
            return;
        }
        Item it = p.itemAt(srcBag, srcSlot);
        int dest = p.firstFreeBagSlot();
        if (it == null || dest < 0 || dest == srcSlot) {
            return;
        }
        int srcField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + srcSlot * 2;
        int dstField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + dest * 2;
        it.slot = dest;
        p.setGuid(srcField, 0);
        p.setGuid(dstField, UpdateBuilder.itemGuid(it));
        world.objectMgr.applyEquippedMelee(p);
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, withAuras(p, withEquipVisuals(p, srcSlot, dest,
                        srcField, srcField + 1, dstField, dstField + 1,
                        UpdateFields.UNIT_FIELD_MINDAMAGE, UpdateFields.UNIT_FIELD_MAXDAMAGE,
                        UpdateFields.UNIT_FIELD_BASEATTACKTIME,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_MAXPOWER1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 1,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 2,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 3,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 4,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 5,
                        UpdateFields.UNIT_FIELD_RESISTANCES + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19,
                        UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23,
                        UpdateFields.PLAYER_SHIELD_BLOCK,
                        UpdateFields.UNIT_FIELD_AURA))));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** item uint32; 0 = clear. PLAYER_AMMO_ID. inventory.md */
    public static void setAmmo(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int item = in.getU32();
        p.setInt(UpdateFields.PLAYER_AMMO_ID, item);
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_AMMO_ID));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** bag, slot. PageText → SMSG_READ_ITEM_OK raw guid. inventory.md */
    public static void readItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int bag = bagIndex(in.getU8());
        int slot = in.getU8();
        Item it = p.itemAt(bag, slot);
        if (it == null) {
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        if (t == null || t.pageText == 0) {
            return;
        }
        WowBuffer ok = new WowBuffer(8);
        ok.putU64(UpdateBuilder.itemGuid(it));
        s.send(Opcodes.SMSG_READ_ITEM_OK, ok.array());
    }

    /** gift_bag, gift_slot, item_bag, item_slot. Consume 1 wrapper; ITEM_DYNFLAG_WRAPPED. inventory.md */
    public static void wrapItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        if (p.channeling) {
            return;
        }
        int giftBag = bagIndex(in.getU8());
        int giftSlot = in.getU8();
        int itemBag = bagIndex(in.getU8());
        int itemSlot = in.getU8();
        if (giftBag != 0 || itemBag != 0) {
            return;
        }
        Item paper = p.itemAt(giftBag, giftSlot);
        Item gift = p.itemAt(itemBag, itemSlot);
        if (paper == null || gift == null || paper == gift) {
            return;
        }
        if (itemSlot < Player.INVENTORY_SLOT_BAG_START) {
            return;
        }
        if (gift.count != 1) {
            return;
        }
        if ((gift.flags & Content.ITEM_DYNFLAG_WRAPPED) != 0) {
            return;
        }
        if (gift.inventoryType == 18) {
            return;
        }
        if (gift.soulbound) {
            return;
        }
        ObjectMgr.ItemTemplate giftTpl = world.objectMgr.items.get(gift.entry);
        if (giftTpl != null && giftTpl.maxCount > 0) {
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(paper.entry);
        if (t == null || (t.flags & Content.ITEM_FLAG_IS_WRAPPER) == 0 || t.stackable <= 1) {
            return;
        }
        int paperField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + giftSlot * 2;
        int itemField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + itemSlot * 2;
        if (paper.count > 1) {
            paper.count--;
            p.setGuid(paperField, UpdateBuilder.itemGuid(paper));
        } else {
            p.items.remove((int) paper.guid);
            p.setGuid(paperField, 0);
        }
        gift.flags = Content.ITEM_DYNFLAG_WRAPPED;
        p.setGuid(itemField, UpdateBuilder.itemGuid(gift));
        var pkt = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p, paperField, paperField + 1, itemField, itemField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** eslot uint32, bag 0 equipment. Clears TEMP_ENCHANTMENT_SLOT. No reply. inventory.md */
    public static void cancelTempEnchantment(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 4) {
            return;
        }
        int eslot = in.getU32();
        if (eslot >= Player.INVENTORY_SLOT_BAG_START) {
            return;
        }
        Item it = p.itemAt(0, eslot);
        if (it == null || it.tempEnchant == 0) {
            return;
        }
        it.tempEnchant = 0;
    }

    public static final int BUYBACK_SLOT_START = 74;
    public static final int BUYBACK_SLOT_END = 86;
    public static final int SELL_ERR_CANT_FIND_ITEM = 1;
    public static final int SELL_ERR_CANT_SELL_ITEM = 2;
    public static final int SELL_ERR_CANT_FIND_VENDOR = 3;
    public static final int TEMP_ENCHANTMENT_SLOT = 1;
    public static final int BONUS_ENCHANTMENT_SLOT = 5;
    public static final int ENCHANT_SLOT_FIELDS = 3;
    public static final int META_GEM_SKYFIRE = 25890;
    /** loot.md clientLootType for item open / pickpocket / skin. */
    public static final int LOOT_PICKPOCKETING = 2;
    /** ItemPrototype.h ITEM_SPELLTRIGGER_ON_USE. */
    public static final int ITEM_SPELLTRIGGER_ON_USE = 0;
    /** ItemPrototype.h INVTYPE_NON_EQUIP. */
    public static final int INVTYPE_NON_EQUIP = 0;

    /** bag, slot. Non-wrapped → SMSG_LOOT_RESPONSE pickpocketing. inventory.md */
    public static void openItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 2) {
            return;
        }
        int bag = bagIndex(in.getU8());
        int slot = in.getU8();
        Item it = p.itemAt(bag, slot);
        if (it == null) {
            return;
        }
        if ((it.flags & Content.ITEM_DYNFLAG_WRAPPED) != 0) {
            it.flags = 0;
            return;
        }
        WowBuffer loot = new WowBuffer(16);
        loot.putU64(UpdateBuilder.itemGuid(it));
        loot.putU8(LOOT_PICKPOCKETING);
        loot.putU32(0);
        loot.putU8(0);
        s.send(Opcodes.SMSG_LOOT_RESPONSE, loot.array());
    }

    /**
     * CMSG_USE_ITEM: bag, slot, spell_index, cast_count, raw item GUID, SpellCastTargets.
     * SpellHandler.cpp HandleUseItemOpcode → Player::CastItemUseSpell → Spell::cast → TakeCastItem.
     */
    public static void useItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() < 12) {
            return;
        }
        int bag = bagIndex(in.getU8());
        int slot = in.getU8();
        int spellIndex = in.getU8();
        int castCount = in.getU8();
        long itemGuid = in.getU64();
        Item it = p.itemAt(bag, slot);
        if (it == null || UpdateBuilder.itemGuid(it) != itemGuid) {
            return;
        }
        ObjectMgr.ItemTemplate proto = world.objectMgr.items.get(it.entry);
        if (proto == null) {
            return;
        }
        if (proto.inventoryType != INVTYPE_NON_EQUIP && it.slot >= Player.EQUIPMENT_SLOT_END) {
            return;
        }
        for (int i = 0; i < proto.spellId.length; i++) {
            int spellId = proto.spellId[i];
            if (spellId == 0 || proto.spellTrigger[i] != ITEM_SPELLTRIGGER_ON_USE || i != spellIndex) {
                continue;
            }
            final int spellSlot = i;
            WowBuffer targets = new WowBuffer(in.remainingBytes());
            world.spells.castFromItem(p, world.map(p.mapId, p.instanceId), world.nowMs(), spellId, castCount,
                    targets, s::send, () -> {
                        if (spellId == SpellEngine.HEARTHSTONE) {
                            // NearTeleportTo homebind — MSG_MOVE_TELEPORT_ACK / SMSG_NEW_WORLD (World.teleport).
                            world.teleport(p, p.bindMap, p.bindX, p.bindY, p.bindZ, p.o);
                        }
                        takeCastItem(s, world, it, proto, spellSlot);
                    });
            return;
        }
    }

    /**
     * Spell::TakeCastItem — negative SpellCharges means expendable; after use, DestroyItemCount 1.
     * Hearthstone charges 0 → no destroy. Food/drink charges −1 → consume one from the stack.
     */
    static void takeCastItem(WorldSession s, World world, Item it, ObjectMgr.ItemTemplate proto, int spellIndex) {
        if (it == null || proto == null || spellIndex < 0 || spellIndex >= proto.spellCharges.length) {
            return;
        }
        // Only expendable (negative) charges remove the item; positive-charge items later.
        if (proto.spellCharges[spellIndex] >= 0) {
            return;
        }
        Player p = s.player();
        if (it.count > 1) {
            it.count--;
            var pkt = UpdateBuilder.maybeCompress(
                    UpdateBuilder.valuesItem(it, UpdateFields.ITEM_FIELD_STACK_COUNT));
            s.send(pkt.opcode(), pkt.payload());
            return;
        }
        p.items.remove(Guid.low(it.guid));
        int field = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        p.setGuid(field, 0);
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, field, field + 1));
        s.send(pkt.opcode(), pkt.payload());
        if (world != null) {
            world.objectMgr.applyEquippedMelee(p);
        }
    }

    public static void sellItem(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        if (p == null || in.remaining() < 16) {
            return;
        }
        long vendorGuid = in.getU64();
        long itemGuid = in.getU64();
        int count = in.remaining() > 0 ? in.getU8() & 0xFF : 0;
        if (itemGuid == 0) {
            return;
        }
        Creature vendor = Content.creature(world.map(p.mapId, p.instanceId), vendorGuid);
        if (vendor == null || Content.outOfRange(p, vendor)
                || (vendor.npcFlags & Content.UNIT_NPC_FLAG_VENDOR) == 0) {
            sendSellError(s, 0, itemGuid, SELL_ERR_CANT_FIND_VENDOR);
            return;
        }
        Item it = p.items.get(Guid.low(itemGuid));
        if (it == null) {
            return;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        if (t == null) {
            sendSellError(s, vendor.guid, itemGuid, SELL_ERR_CANT_FIND_ITEM);
            return;
        }
        if (t.sellPrice <= 0) {
            sendSellError(s, vendor.guid, itemGuid, SELL_ERR_CANT_SELL_ITEM);
            return;
        }
        int sellCount = count == 0 ? Math.max(1, it.count) : count;
        if (sellCount > it.count) {
            sendSellError(s, vendor.guid, itemGuid, SELL_ERR_CANT_SELL_ITEM);
            return;
        }
        int money = t.sellPrice * sellCount;
        int invSlot = it.slot;
        int invField = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + invSlot * 2;
        if (sellCount < it.count) {
            it.count -= sellCount;
            Item sold = new Item(world.nextItemGuid(), it.entry);
            sold.ownerGuid = it.ownerGuid;
            sold.count = sellCount;
            sold.displayId = it.displayId;
            sold.inventoryType = it.inventoryType;
            sold.quality = it.quality;
            sold.delay = it.delay;
            sold.dmgMin = it.dmgMin;
            sold.dmgMax = it.dmgMax;
            addToBuyback(p, sold, money);
            var created = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(sold, p.guid));
            s.send(created.opcode(), created.payload());
        } else {
            p.items.remove(Guid.low(it.guid));
            p.setGuid(invField, 0);
            addToBuyback(p, it, money);
        }
        p.setMoney(p.money + money);
        p.dirty = true;
        int priceField = UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1;
        int backField = UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1;
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                p, invField, invField + 1, UpdateFields.PLAYER_FIELD_COINAGE, priceField, backField, backField + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    private static void addToBuyback(Player p, Item it, int money) {
        it.slot = BUYBACK_SLOT_START;
        it.bag = 0;
        p.buyback.put(BUYBACK_SLOT_START, it);
        p.setInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, money);
        p.setGuid(UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1, UpdateBuilder.itemGuid(it));
    }

    private static void sendSellError(WorldSession s, long vendorGuid, long itemGuid, int result) {
        WowBuffer err = new WowBuffer(17);
        err.putU64(vendorGuid);
        err.putU64(itemGuid);
        err.putU8(result);
        s.send(Opcodes.SMSG_SELL_ITEM, err.array());
    }

    public static void buybackItem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        if (in.remaining() >= 8) {
            in.getU64();
        }
        int slot = in.remaining() >= 4 ? in.getU32() : BUYBACK_SLOT_START;
        Item it = p.buyback.remove(slot);
        if (it == null) {
            return;
        }
        int bag = p.firstFreeBagSlot();
        it.slot = bag < 0 ? 23 : bag;
        p.items.put((int) it.guid, it);
        p.setGuid(UpdateFields.PLAYER_FIELD_VENDORBUYBACK_SLOT_1, 0);
        p.setInt(UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, 0);
        int inv = UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2;
        p.setGuid(inv, UpdateBuilder.itemGuid(it));
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                p, UpdateFields.PLAYER_FIELD_BUYBACK_PRICE_1, inv, inv + 1));
        s.send(pkt.opcode(), pkt.payload());
    }

    public static void repairItem(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 16) {
            return;
        }
        Player p = s.player();
        long npcGuid = in.getU64();
        long itemGuid = in.getU64();
        if (in.remaining() >= 1) {
            in.getU8();
        }
        Creature npc = Content.creature(world.map(p.mapId, p.instanceId), npcGuid);
        if (npc == null || Content.outOfRange(p, npc)
                || (npc.npcFlags & Content.UNIT_NPC_FLAG_REPAIR) == 0) {
            return;
        }
        boolean charged = false;
        if (itemGuid != 0) {
            Item it = p.items.get(Guid.low(itemGuid));
            charged = repairOne(s, world, p, it, true);
        } else {
            for (Item it : p.items.values()) {
                if (!repairableSlot(it)) {
                    continue;
                }
                charged |= repairOne(s, world, p, it, true);
            }
        }
        if (charged) {
            var pkt = UpdateBuilder.maybeCompress(
                    UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
            s.send(pkt.opcode(), pkt.payload());
        }
    }

    static boolean repairableSlot(Item it) {
        if (it.bag >= Player.INVENTORY_SLOT_BAG_START && it.bag < Player.INVENTORY_SLOT_BAG_END) {
            return true;
        }
        return it.bag == 0 && it.slot < Player.BANK_SLOT_ITEM_START;
    }

    static boolean repairOne(WorldSession s, World world, Player p, Item it, boolean cost) {
        if (it == null) {
            return false;
        }
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(it.entry);
        int max = it.maxDurability;
        if (max <= 0 && t != null) {
            max = t.maxDurability;
        }
        if (max <= 0) {
            return false;
        }
        int lost = max - it.durability;
        if (cost && lost > 0) {
            if (t == null) {
                return false;
            }
            int copper = DurabilityCosts.repairCopper(
                    lost, t.itemLevel, t.itemClass, t.subClass, t.quality, 1.0f);
            if (copper <= 0) {
                return false;
            }
            if (p.money < copper) {
                return false;
            }
            p.setMoney(p.money - copper);
        }
        if (it.durability == max) {
            return cost && lost > 0;
        }
        it.durability = max;
        if (it.maxDurability <= 0) {
            it.maxDurability = max;
        }
        var itemUpd = UpdateBuilder.maybeCompress(
                UpdateBuilder.valuesItem(it, UpdateFields.ITEM_FIELD_DURABILITY));
        s.send(itemUpd.opcode(), itemUpd.payload());
        return cost && lost > 0;
    }

    public static void socketGems(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long itemGuid = in.remaining() >= 8 ? in.getU64() : 0;
        long gem0 = in.remaining() >= 8 ? in.getU64() : 0;
        if (in.remaining() >= 8) {
            in.getU64();
        }
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Item it = p.items.get((int) itemGuid);
        if (it == null) {
            return;
        }
        Item gem = gem0 != 0 ? p.items.get((int) gem0) : null;
        if (gem != null && gem.entry == META_GEM_SKYFIRE) {
            return;
        }
        if (gem != null) {
            p.items.remove((int) gem.guid);
        }
        it.enchant = 1;
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
        s.send(pkt.opcode(), pkt.payload());
    }
}
