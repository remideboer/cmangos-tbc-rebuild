package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL14-* from packet files, one criterion per method. */
class Slice14P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl14SwapInvItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Swapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item first = new Item(world.nextItemGuid(), 25);
        first.slot = src;
        p.items.put((int) first.guid, first);
        int dst = p.firstFreeBagSlot();
        Item second = new Item(world.nextItemGuid(), 159);
        second.slot = dst;
        p.items.put((int) first.guid, first);
        p.items.put((int) second.guid, second);
        p.setGuid(invSlotField(first.slot), UpdateBuilder.itemGuid(first));
        p.setGuid(invSlotField(second.slot), UpdateBuilder.itemGuid(second));
        int srcSlot = first.slot;
        int dstSlot = second.slot;

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(srcSlot);
        swap.putU8(dstSlot);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        long atSrc = guidAt(update, invSlotField(srcSlot));
        long atDst = guidAt(update, invSlotField(dstSlot));
        assertEquals(UpdateBuilder.itemGuid(second), atSrc);
        assertEquals(UpdateBuilder.itemGuid(first), atDst);
    }

    @Test
    void tpSl14SwapItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BagSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item first = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        first.slot = src;
        p.items.put((int) first.guid, first);
        int dst = p.firstFreeBagSlot();
        Item second = new Item(world.nextItemGuid(), 159);
        second.slot = dst;
        p.items.put((int) second.guid, second);
        p.setGuid(invSlotField(first.slot), UpdateBuilder.itemGuid(first));
        p.setGuid(invSlotField(second.slot), UpdateBuilder.itemGuid(second));
        int srcSlot = first.slot;
        int dstSlot = second.slot;

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dstSlot);
        swap.putU8(0);
        swap.putU8(srcSlot);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(UpdateBuilder.itemGuid(second), guidAt(update, invSlotField(srcSlot)));
        assertEquals(UpdateBuilder.itemGuid(first), guidAt(update, invSlotField(dstSlot)));
    }

    @Test
    void tpSl14DestroyItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Destroyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int slot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.slot = slot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(slot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(slot);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(slot)));
    }

    @Test
    void tpSl14SplitItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Splitter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item stack = new Item(world.nextItemGuid(), 25);
        stack.slot = src;
        stack.count = 2;
        p.items.put((int) stack.guid, stack);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(stack));
        int dst = p.firstFreeBagSlot();

        client.clear();
        WowBuffer split = new WowBuffer(5);
        split.putU8(0);
        split.putU8(src);
        split.putU8(0);
        split.putU8(dst);
        split.putU8(1);
        client.handle(world, Opcodes.CMSG_SPLIT_ITEM, split.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        Item splitOff = p.itemAt(0, dst);
        assertNotNull(splitOff);
        assertEquals(UpdateBuilder.itemGuid(splitOff), guidAt(update, invSlotField(dst)));
    }

    @Test
    void tpSl14ShowBank() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Banker", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature banker = find(world, Content.NPC_OLIVIA_BURNSIDE);
        assertNotNull(banker);
        p.relocate(banker.x, banker.y, banker.z, banker.o);
        client.clear();
        WowBuffer activate = new WowBuffer(8);
        activate.putU64(banker.guid);
        client.handle(world, Opcodes.CMSG_BANKER_ACTIVATE, activate.array());

        assertTrue(client.saw(Opcodes.SMSG_SHOW_BANK));
        WowBuffer shown = new WowBuffer(client.payload(Opcodes.SMSG_SHOW_BANK));
        assertEquals(banker.guid, shown.getU64());
    }

    @Test
    void tpSl14BuyBankSlot() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BagSlot", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature banker = find(world, Content.NPC_OLIVIA_BURNSIDE);
        assertNotNull(banker);
        p.relocate(banker.x, banker.y, banker.z, banker.o);
        p.setMoney(Content.BANK_BAG_SLOT_PRICES[1]);
        client.clear();
        WowBuffer buy = new WowBuffer(8);
        buy.putU64(banker.guid);
        client.handle(world, Opcodes.CMSG_BUY_BANK_SLOT, buy.array());

        assertTrue(client.saw(Opcodes.SMSG_BUY_BANK_SLOT_RESULT));
        assertEquals(Content.ERR_BANKSLOT_OK,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_BUY_BANK_SLOT_RESULT), 0));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0, intAt(update, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(1, (intAt(update, UpdateFields.PLAYER_BYTES_2) >> 16) & 0xFF);
    }

    @Test
    void tpSl14BuyBankSlotWhenBrokeShouldReturnInsufficientFunds() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Broke", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature banker = find(world, Content.NPC_OLIVIA_BURNSIDE);
        assertNotNull(banker);
        p.relocate(banker.x, banker.y, banker.z, banker.o);
        p.setMoney(Content.BANK_BAG_SLOT_PRICES[1] - 1);
        client.clear();
        WowBuffer buy = new WowBuffer(8);
        buy.putU64(banker.guid);
        client.handle(world, Opcodes.CMSG_BUY_BANK_SLOT, buy.array());

        assertTrue(client.saw(Opcodes.SMSG_BUY_BANK_SLOT_RESULT));
        assertEquals(Content.ERR_BANKSLOT_INSUFFICIENT_FUNDS,
                WowClientDouble.u32le(client.payload(Opcodes.SMSG_BUY_BANK_SLOT_RESULT), 0));
        assertFalse(client.saw(Opcodes.SMSG_UPDATE_OBJECT));
        assertFalse(client.saw(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertEquals(0, p.bankBagSlotCount());
        assertEquals(Content.BANK_BAG_SLOT_PRICES[1] - 1, p.money);
    }

    @Test
    void tpSl14BuyBankSlotWhenNotBankerShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "NoBank", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature vendor = find(world, Content.NPC_CORINA_STEELE);
        assertNotNull(vendor);
        p.relocate(vendor.x, vendor.y, vendor.z, vendor.o);
        p.setMoney(Content.BANK_BAG_SLOT_PRICES[1]);
        client.clear();
        WowBuffer buy = new WowBuffer(8);
        buy.putU64(vendor.guid);
        client.handle(world, Opcodes.CMSG_BUY_BANK_SLOT, buy.array());

        assertFalse(client.saw(Opcodes.SMSG_BUY_BANK_SLOT_RESULT));
        assertEquals(0, p.bankBagSlotCount());
    }

    @Test
    void tpSl14BuyBankSlotWhenAllSlotsBoughtShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "FullBank", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature banker = find(world, Content.NPC_OLIVIA_BURNSIDE);
        assertNotNull(banker);
        p.relocate(banker.x, banker.y, banker.z, banker.o);
        p.setBankBagSlotCount(Player.BANK_SLOT_BAG_END - Player.BANK_SLOT_BAG_START);
        p.setMoney(Content.BANK_BAG_SLOT_PRICES[7]);
        client.clear();
        WowBuffer buy = new WowBuffer(8);
        buy.putU64(banker.guid);
        client.handle(world, Opcodes.CMSG_BUY_BANK_SLOT, buy.array());

        assertFalse(client.saw(Opcodes.SMSG_BUY_BANK_SLOT_RESULT));
    }

    @Test
    void tpSl14BuyBankSlotWhenPayloadShortShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ShortBuy", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        client.handle(world, Opcodes.CMSG_BUY_BANK_SLOT, new byte[4]);
        assertFalse(client.saw(Opcodes.SMSG_BUY_BANK_SLOT_RESULT));
    }

    @Test
    void tpSl14AuctionExpireOnTick() {
        World world = World.inMemory();
        world.objectMgr.auctions.add(new ObjectMgr.Auction(
                99, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 0, "Expired"));
        assertTrue(world.objectMgr.auctions.stream().anyMatch(a -> a.id() == 99));

        world.tick(60_000);

        assertTrue(world.objectMgr.auctions.stream().noneMatch(a -> a.id() == 99));
        assertTrue(world.objectMgr.auctions.stream().anyMatch(a -> a.id() == 1));
    }

    @Test
    void tpSl14AuctionExpireWhenTimerNotDueShouldKeepListing() {
        World world = World.inMemory();
        world.objectMgr.auctions.add(new ObjectMgr.Auction(
                99, Content.ITEM_WORN_SHORTSWORD, 0, 100, 0, 0, "Expired"));
        world.tick(60_000 - 1);
        assertTrue(world.objectMgr.auctions.stream().anyMatch(a -> a.id() == 99));
    }

    @Test
    void tpSl14AutobankItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Depositor", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer bank = new WowBuffer(2);
        bank.putU8(0);
        bank.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOBANK_ITEM, bank.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(src)));
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(Player.BANK_SLOT_ITEM_START)));
    }

    @Test
    void tpSl14AutostoreBankItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Withdrawer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int bank = Player.BANK_SLOT_ITEM_START;
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.slot = bank;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(bank), UpdateBuilder.itemGuid(sword));
        int dest = p.firstFreeBagSlot();

        client.clear();
        WowBuffer store = new WowBuffer(2);
        store.putU8(0);
        store.putU8(bank);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BANK_ITEM, store.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(bank)));
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(dest)));
    }

    @Test
    void tpSl14AutostoreInventoryToBank() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Stasher", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), 25);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer store = new WowBuffer(2);
        store.putU8(0);
        store.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BANK_ITEM, store.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(src)));
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(Player.BANK_SLOT_ITEM_START)));
    }

    @Test
    void tpSl14AutoequipItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Equipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(Player.EQUIPMENT_SLOT_MAINHAND)));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemMods weapon half. Autoequip Worn Shortsword 25 must put
     * UNIT_FIELD_MINDAMAGE/MAXDAMAGE and UNIT_FIELD_BASEATTACKTIME on the self VALUES (inventory.md).
     */
    @Test
    void tpSl14EquipAppliesItemMods() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ModEquipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(1f, Float.intBitsToFloat(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MINDAMAGE)));
        assertEquals(3f, Float.intBitsToFloat(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXDAMAGE)));
        assertEquals(1900, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    /**
     * TP-SL14-013 — RemoveItem unarmed. Autostore of Worn Shortsword 25 must restore Unit
     * create MINDAMAGE/MAXDAMAGE 1–3 and BASEATTACKTIME 2000 (not the weapon delay 1900).
     */
    @Test
    void tpSl14UnequipReversesWeaponDamage() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "FistEquipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int mainhand = sword.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(mainhand);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(1f, Float.intBitsToFloat(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MINDAMAGE)));
        assertEquals(3f, Float.intBitsToFloat(client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXDAMAGE)));
        assertEquals(2000, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses. Autoequip Riverpaw Leather Vest 821
     * (stat_type1 STAMINA 7 / stat_value1 2, armor 65) must add those to the self VALUES:
     * human warrior create STA 22 / armor agi×2 40 (create-self.md).
     */
    @Test
    void tpSl14EquipAppliesStaminaAndArmor() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "StaEquipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(105, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::SwapItem → EquipItem → _ApplyItemMods. CMSG_SWAP_INV_ITEM
     * of Riverpaw Leather Vest 821 onto the chest slot must put create+gear STA 24 /
     * armor 105 on the self VALUES (same deltas as autoequip).
     */
    @Test
    void tpSl14SwapInvItemAppliesStaminaAndArmor() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "StaSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));
        int chest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(chest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(105, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_HIT_RATING.
     * CMSG_SWAP_INV_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * HIT 24 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE / CR_HIT_RANGED.
     */
    @Test
    void tpSl14SwapInvItemAppliesHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HitSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_DEFENSE_SKILL_RATING.
     * CMSG_SWAP_INV_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * DEFENSE 27 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL.
     */
    @Test
    void tpSl14SwapInvItemAppliesDefenseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DefSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(27, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_DODGE_RATING.
     * CMSG_SWAP_INV_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * DODGE 24 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE.
     */
    @Test
    void tpSl14SwapInvItemAppliesDodgeRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DodgeSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_PARRY_RATING.
     * CMSG_SWAP_INV_ITEM of Onslaught Chestguard 30976 onto the chest slot must write
     * PARRY 28 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY (Unit.h 3).
     */
    @Test
    void tpSl14SwapInvItemAppliesParryRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ParrySwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(28, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_BLOCK_RATING.
     * CMSG_SWAP_INV_ITEM of Onslaught Chestguard 30976 onto the chest slot must write
     * BLOCK 23 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK (Unit.h 4).
     */
    @Test
    void tpSl14SwapInvItemAppliesBlockRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BlockSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_CRIT_RATING.
     * CMSG_SWAP_INV_ITEM of Destroyer Breastplate 30118 onto the chest slot must write
     * CRIT 33 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED.
     */
    @Test
    void tpSl14SwapInvItemAppliesCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "CritSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_RESILIENCE_RATING.
     * CMSG_SWAP_INV_ITEM of Gladiator's Plate Chestpiece 24544 onto the chest slot must write
     * RESILIENCE 23 on self VALUES CR_CRIT_TAKEN_MELEE / RANGED / SPELL (Unit.h 14 / 15 / 16).
     */
    @Test
    void tpSl14SwapInvItemAppliesResilienceRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ResSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GLADIATORS_PLATE_CHESTPIECE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_HASTE_RATING.
     * CMSG_SWAP_INV_ITEM of Warharness of Reckless Fury 34215 onto the chest slot must write
     * HASTE 32 on self VALUES CR_HASTE_MELEE / CR_HASTE_RANGED (Unit.h 17 / 18).
     */
    @Test
    void tpSl14SwapInvItemAppliesHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HasteSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_WARHARNESS_OF_RECKLESS_FURY);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17));
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18));
    }

    /**
     * TP-SL14-013 — HandleSwapInvItem → EquipItem → _ApplyItemMods ITEM_MOD_EXPERTISE_RATING.
     * CMSG_SWAP_INV_ITEM of Gauntlets of Enforcement 32280 onto the hands slot must write
     * EXPERTISE 21 on self VALUES CR_EXPERTISE (Unit.h 23).
     */
    @Test
    void tpSl14SwapInvItemAppliesExpertiseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ExpertiseSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item gloves = new Item(world.nextItemGuid(), Content.ITEM_GAUNTLETS_OF_ENFORCEMENT);
        gloves.inventoryType = 10;
        gloves.slot = src;
        p.items.put((int) gloves.guid, gloves);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(gloves));
        int dest = world.objectMgr.destEquipSlot(p, 10);

        client.clear();
        WowBuffer swap = new WowBuffer(2);
        swap.putU8(src);
        swap.putU8(dest);
        client.handle(world, Opcodes.CMSG_SWAP_INV_ITEM, swap.array());
        assertEquals(21, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → Player::SwapItem → EquipItem → _ApplyItemMods.
     * CMSG_SWAP_ITEM of Riverpaw Leather Vest 821 onto the chest slot must put create+gear
     * STA 24 / armor 105 on the self VALUES (same deltas as CMSG_SWAP_INV_ITEM).
     */
    @Test
    void tpSl14SwapItemAppliesStaminaAndArmor() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "StaBagSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));
        int chest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(chest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(105, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_HIT_RATING.
     * CMSG_SWAP_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * HIT 24 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE / CR_HIT_RANGED.
     */
    @Test
    void tpSl14SwapItemAppliesHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HitBagSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_DEFENSE_SKILL_RATING.
     * CMSG_SWAP_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * DEFENSE 27 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL.
     */
    @Test
    void tpSl14SwapItemAppliesDefenseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DefBagSwapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(27, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_DODGE_RATING.
     * CMSG_SWAP_ITEM of Destroyer Chestguard 30113 onto the chest slot must write
     * DODGE 24 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE.
     */
    @Test
    void tpSl14SwapItemAppliesDodgeRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DodgeBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_PARRY_RATING.
     * CMSG_SWAP_ITEM of Onslaught Chestguard 30976 onto the chest slot must write
     * PARRY 28 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY (Unit.h 3).
     */
    @Test
    void tpSl14SwapItemAppliesParryRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ParryBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(28, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_BLOCK_RATING.
     * CMSG_SWAP_ITEM of Onslaught Chestguard 30976 onto the chest slot must write
     * BLOCK 23 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK (Unit.h 4).
     */
    @Test
    void tpSl14SwapItemAppliesBlockRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BlockBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_CRIT_RATING.
     * CMSG_SWAP_ITEM of Destroyer Breastplate 30118 onto the chest slot must write
     * CRIT 33 on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED.
     */
    @Test
    void tpSl14SwapItemAppliesCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "CritBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_RESILIENCE_RATING.
     * CMSG_SWAP_ITEM of Gladiator's Plate Chestpiece 24544 onto the chest slot must write
     * RESILIENCE 23 on self VALUES CR_CRIT_TAKEN_MELEE / RANGED / SPELL (Unit.h 14 / 15 / 16).
     */
    @Test
    void tpSl14SwapItemAppliesResilienceRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ResBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GLADIATORS_PLATE_CHESTPIECE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_HASTE_RATING.
     * CMSG_SWAP_ITEM of Warharness of Reckless Fury 34215 onto the chest slot must write
     * HASTE 32 on self VALUES CR_HASTE_MELEE / CR_HASTE_RANGED (Unit.h 17 / 18).
     */
    @Test
    void tpSl14SwapItemAppliesHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HasteBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_WARHARNESS_OF_RECKLESS_FURY);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));
        int dest = world.objectMgr.destEquipSlot(p, 5);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17));
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18));
    }

    /**
     * TP-SL14-013 — HandleSwapItem → EquipItem → _ApplyItemMods ITEM_MOD_EXPERTISE_RATING.
     * CMSG_SWAP_ITEM of Gauntlets of Enforcement 32280 onto the hands slot must write
     * EXPERTISE 21 on self VALUES CR_EXPERTISE (Unit.h 23).
     */
    @Test
    void tpSl14SwapItemAppliesExpertiseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ExpertiseBagSwap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item gloves = new Item(world.nextItemGuid(), Content.ITEM_GAUNTLETS_OF_ENFORCEMENT);
        gloves.inventoryType = 10;
        gloves.slot = src;
        p.items.put((int) gloves.guid, gloves);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(gloves));
        int dest = world.objectMgr.destEquipSlot(p, 10);

        client.clear();
        WowBuffer swap = new WowBuffer(4);
        swap.putU8(0);
        swap.putU8(dest);
        swap.putU8(0);
        swap.putU8(src);
        client.handle(world, Opcodes.CMSG_SWAP_ITEM, swap.array());
        assertEquals(21, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23));
    }

    /**
     * TP-SL14-013 — Player::UpdateStats STAT_STAMINA → Unit::UpdateMaxHealth. Autoequip
     * Riverpaw Leather Vest 821 (+2 STA) must raise self VALUES UNIT_FIELD_MAXHEALTH from
     * create 60 (20 + bonus(22)) to 80 (20 + bonus(24)).
     */
    @Test
    void tpSl14EquipAppliesStaminaToMaxHealth() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HpEquipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(80, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXHEALTH));
    }

    /**
     * TP-SL14-013 — Player::UpdateStats STAT_INTELLECT → Unit::UpdateMaxPower(POWER_MANA).
     * Autoequip Seer's Robe 2981 (+6 INT) on a human mage must raise self VALUES
     * UNIT_FIELD_MAXPOWER1 from create 165 (100 + bonus(23)) to 255 (100 + bonus(29)).
     */
    @Test
    void tpSl14EquipAppliesIntellectToMaxMana() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ManaEquip", 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item robe = new Item(world.nextItemGuid(), Content.ITEM_SEERS_ROBE);
        robe.inventoryType = 5;
        robe.slot = src;
        p.items.put((int) robe.guid, robe);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(robe));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(255, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER1));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses extra proto slots. Autoequip Tunic of Westfall
     * 2041 (stat_type1 AGILITY 3 / 11, stat_type2 STAMINA 7 / 5, armor 92) must add those
     * to the self VALUES: human warrior create AGI 20 / STA 22 / armor agi×2 40.
     */
    @Test
    void tpSl14EquipAppliesSecondStatSlot() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Westfall", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item tunic = new Item(world.nextItemGuid(), Content.ITEM_TUNIC_OF_WESTFALL);
        tunic.inventoryType = 5;
        tunic.slot = src;
        p.items.put((int) tunic.guid, tunic);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(tunic));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(31, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT1));
        assertEquals(27, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(154, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_STRENGTH. Autoequip Brackwater Vest
     * 3306 (stat_type1 STRENGTH 4 / 4, stat_type2 STAMINA 7 / 3, armor 162) must add those
     * to the self VALUES: human warrior create STR 23 / STA 22 / armor agi×2 40.
     */
    @Test
    void tpSl14EquipAppliesStrength() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Brackwater", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_BRACKWATER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(27, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(25, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(202, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_INTELLECT / ITEM_MOD_SPIRIT.
     * Autoequip Seer's Robe 2981 (stat_type1 INTELLECT 5 / 6, stat_type2 SPIRIT 6 / 3,
     * armor 35) must add those to the self VALUES: human warrior create INT 20 / SPI 20
     * / armor agi×2 40.
     */
    @Test
    void tpSl14EquipAppliesIntellectAndSpirit() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Seer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item robe = new Item(world.nextItemGuid(), Content.ITEM_SEERS_ROBE);
        robe.inventoryType = 5;
        robe.slot = src;
        p.items.put((int) robe.guid, robe);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(robe));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(26, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT4));
        assertEquals(75, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses extra proto slot 3. Autoequip Blackened
     * Defias Armor 10399 (STR 4 / AGI 3 / STA 11, armor 92) must add those to the self
     * VALUES: human warrior create STR 23 / AGI 20 / STA 22 / armor agi×2 40.
     */
    @Test
    void tpSl14EquipAppliesThirdStatSlot() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Defias", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_BLACKENED_DEFIAS_ARMOR);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(27, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT1));
        assertEquals(33, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(138, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses extra proto slot 4. Autoequip Lightforge
     * Breastplate 16726 (STR 13 / STA 21 / INT 16 / SPI 8, armor 657) must add those to
     * the self VALUES: human warrior create STR 23 / STA 22 / INT 20 / SPI 20 / armor agi×2 40.
     */
    @Test
    void tpSl14EquipAppliesFourthStatSlot() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Lightforge", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_LIGHTFORGE_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(36, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(43, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(36, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(28, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT4));
        assertEquals(697, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses FireRes → UNIT_MOD_RESISTANCE_FIRE.
     * Autoequip Lawbringer Chestguard 16853 (+10 Fire Resistance) must write that on
     * self VALUES UNIT_FIELD_RESISTANCES+2 (school fire).
     */
    @Test
    void tpSl14EquipAppliesFireResistance() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Lawbringer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_LAWBRINGER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(10, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES + 2));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses NatureRes → UNIT_MOD_RESISTANCE_NATURE.
     * Autoequip Living Breastplate 15059 (+5 Nature Resistance) must write that on
     * self VALUES UNIT_FIELD_RESISTANCES+3 (school nature).
     */
    @Test
    void tpSl14EquipAppliesNatureResistance() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Living", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_LIVING_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(5, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES + 3));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses FrostRes → UNIT_MOD_RESISTANCE_FROST.
     * Autoequip Icebane Breastplate 22669 (+42 Frost Resistance) must write that on
     * self VALUES UNIT_FIELD_RESISTANCES+4 (school frost).
     */
    @Test
    void tpSl14EquipAppliesFrostResistance() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Icebane", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ICEBANE_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(42, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES + 4));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ShadowRes → UNIT_MOD_RESISTANCE_SHADOW.
     * Autoequip Shadesteel Greaves 32404 (+72 Shadow Resistance) must write that on
     * self VALUES UNIT_FIELD_RESISTANCES+5 (school shadow).
     */
    @Test
    void tpSl14EquipAppliesShadowResistance() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Shadesteel", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item legs = new Item(world.nextItemGuid(), Content.ITEM_SHADESTEEL_GREAVES);
        legs.inventoryType = 7;
        legs.slot = src;
        p.items.put((int) legs.guid, legs);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(legs));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(72, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES + 5));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ArcaneRes → UNIT_MOD_RESISTANCE_ARCANE.
     * Autoequip Soulcloth Vest 21865 (+45 Arcane Resistance) must write that on
     * self VALUES UNIT_FIELD_RESISTANCES+6 (school arcane).
     */
    @Test
    void tpSl14EquipAppliesArcaneResistance() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Soulcloth", 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_SOULCLOTH_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(45, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES + 6));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses fifth ItemStat slot (stat_type5).
     * Autoequip Blade of Hanna 2801 (+11 Spirit in slot 5) must write create SPI 20+11
     * on self VALUES UNIT_FIELD_STAT4.
     */
    @Test
    void tpSl14EquipAppliesFifthItemStat() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Hanna", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_BLADE_OF_HANNA);
        sword.inventoryType = 17;
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(31, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT4));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HIT_RATING (stat_type6 on
     * Destroyer Chestguard 30113). Autoequip must write HIT 24 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE / CR_HIT_RANGED (Unit.h 5 / 6).
     */
    @Test
    void tpSl14EquipAppliesHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Destroyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_DEFENSE_SKILL_RATING.
     * Autoequip Destroyer Chestguard 30113 (+27 defense) must write 27 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL (Unit.h 1).
     */
    @Test
    void tpSl14EquipAppliesDefenseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Defender", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(27, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_DODGE_RATING.
     * Autoequip Destroyer Chestguard 30113 (+24 dodge) must write 24 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE (Unit.h 2).
     */
    @Test
    void tpSl14EquipAppliesDodgeRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Dodger", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_PARRY_RATING (ItemPrototype.h 14).
     * Autoequip Onslaught Chestguard 30976 (+28 parry) must write 28 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY (Unit.h 3).
     */
    @Test
    void tpSl14EquipAppliesParryRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Parrier", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(28, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_BLOCK_RATING (ItemPrototype.h 15).
     * Autoequip Onslaught Chestguard 30976 (+23 shield block rating) must write 23 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK (Unit.h 4).
     */
    @Test
    void tpSl14EquipAppliesBlockRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Blocker", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_CRIT_RATING (ItemPrototype.h 32).
     * Autoequip Destroyer Breastplate 30118 (+33 crit) must write 33 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED (Unit.h 8 / 9).
     */
    @Test
    void tpSl14EquipAppliesCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Critter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_RESILIENCE_RATING (ItemPrototype.h 35).
     * Autoequip Gladiator's Plate Chestpiece 24544 (+23 resilience) must write 23 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_TAKEN_MELEE / RANGED / SPELL (Unit.h 14 / 15 / 16).
     */
    @Test
    void tpSl14EquipAppliesResilienceRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Resilient", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GLADIATORS_PLATE_CHESTPIECE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HASTE_RATING (ItemPrototype.h 36).
     * Autoequip Warharness of Reckless Fury 34215 (+32 haste) must write 32 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HASTE_MELEE / CR_HASTE_RANGED (Unit.h 17 / 18), not spell.
     */
    @Test
    void tpSl14EquipAppliesHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Hasty", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_WARHARNESS_OF_RECKLESS_FURY);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17));
        assertEquals(32, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_EXPERTISE_RATING (ItemPrototype.h 37).
     * Autoequip Gauntlets of Enforcement 32280 (+21 expertise) must write 21 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_EXPERTISE (Unit.h 23) only.
     */
    @Test
    void tpSl14EquipAppliesExpertiseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Enforcer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item gloves = new Item(world.nextItemGuid(), Content.ITEM_GAUNTLETS_OF_ENFORCEMENT);
        gloves.inventoryType = 10;
        gloves.slot = src;
        p.items.put((int) gloves.guid, gloves);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(gloves));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(21, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses seventh ItemStat slot (stat_type7).
     * Autoequip Vengeful Gladiator's Dragonhide Tunic 33675 must write CRIT 19 from slot 7
     * on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED
     * (Unit.h 8 / 9). tbc-db: CRIT is stat_type7, not an earlier slot.
     */
    @Test
    void tpSl14EquipAppliesSeventhItemStat() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Gladiator", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_VENGEFUL_GLADIATORS_DRAGONHIDE_TUNIC);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(19, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(19, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HIT_SPELL_RATING (ItemPrototype.h 18).
     * Autoequip Auchenai Anchorite's Robe 29341 (+23 spell hit) must write 23 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_SPELL (Unit.h 7) only.
     */
    @Test
    void tpSl14EquipAppliesSpellHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Anchorite", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item robe = new Item(world.nextItemGuid(), Content.ITEM_AUCHENAI_ANCHORITES_ROBE);
        robe.inventoryType = 20;
        robe.slot = src;
        p.items.put((int) robe.guid, robe);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(robe));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(23, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 7));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_CRIT_SPELL_RATING (ItemPrototype.h 21).
     * Autoequip Garments of Serene Shores 34229 (+25 spell crit) must write 25 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_SPELL (Unit.h 10) only.
     */
    @Test
    void tpSl14EquipAppliesSpellCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Shores", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GARMENTS_OF_SERENE_SHORES);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(25, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 10));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HASTE_SPELL_RATING (ItemPrototype.h 30).
     * Autoequip Sunglow Vest 34212 (+33 spell haste) must write 33 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HASTE_SPELL (Unit.h 19) only.
     */
    @Test
    void tpSl14EquipAppliesSpellHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Sunglow", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_SUNGLOW_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(33, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 19));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses proto-&gt;Block → HandleBaseModValue(SHIELD_BLOCK_VALUE).
     * Autoequip Worn Wooden Shield 2362 must write block 1 on self VALUES PLAYER_SHIELD_BLOCK
     * (update-fields.yaml 1331).
     */
    @Test
    void tpSl14EquipAppliesShieldBlock() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Buckler", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item shield = new Item(world.nextItemGuid(), Content.ITEM_WORN_WOODEN_SHIELD);
        shield.inventoryType = 14;
        shield.slot = src;
        p.items.put((int) shield.guid, shield);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(shield));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(1, client.valuesField(p.guid, UpdateFields.PLAYER_SHIELD_BLOCK));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_CRIT_MELEE_RATING (ItemPrototype.h 19).
     * Autoequip Cloak of Darkness 33122 (+24 melee crit) must write 24 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE (Unit.h 8) only, not CR_CRIT_RANGED.
     */
    @Test
    void tpSl14EquipAppliesMeleeCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Darkcloak", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item cloak = new Item(world.nextItemGuid(), Content.ITEM_CLOAK_OF_DARKNESS);
        cloak.inventoryType = 16;
        cloak.slot = src;
        p.items.put((int) cloak.guid, cloak);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(cloak));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(24, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_CRIT_RANGED_RATING (ItemPrototype.h 20).
     * Autoequip Netherstrand Longbow 30318 (+50 ranged crit) must write 50 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_RANGED (Unit.h 9) only, not CR_CRIT_MELEE.
     */
    @Test
    void tpSl14EquipAppliesRangedCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Bowman", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item bow = new Item(world.nextItemGuid(), Content.ITEM_NETHERSTRAND_LONGBOW);
        bow.inventoryType = 15;
        bow.slot = src;
        p.items.put((int) bow.guid, bow);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(bow));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(50, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
    }

    /**
     * TP-SL14-013 — LoadItemPrototypes third damage line (dmg_min3). CMSG_ITEM_QUERY_SINGLE of
     * Twin Blades of Azzinoth 18582 must put arcane 40–60 school 6 on the third proto damage
     * slot (queries.md; Player::_ApplyWeaponDamage index 2).
     */
    @Test
    void tpSl14ItemQueryCarriesThirdDamageLine() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Glaive", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);

        client.clear();
        WowBuffer q = new WowBuffer(4);
        q.putU32(Content.ITEM_TWIN_BLADES_OF_AZZINOTH);
        client.handle(world, Opcodes.CMSG_ITEM_QUERY_SINGLE, q.array());
        WowBuffer b = new WowBuffer(lastPayload(client, Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE));
        assertEquals(Content.ITEM_TWIN_BLADES_OF_AZZINOTH, b.getU32());
        b.getU32();
        b.getU32();
        b.getU32();
        b.getCString();
        b.getU8();
        b.getU8();
        b.getU8();
        for (int i = 0; i < 20; i++) {
            b.getU32();
        }
        for (int i = 0; i < 10; i++) {
            b.getU32();
            b.getU32();
        }
        b.getFloat();
        b.getFloat();
        b.getU32();
        b.getFloat();
        b.getFloat();
        b.getU32();
        assertEquals(40f, b.getFloat());
        assertEquals(60f, b.getFloat());
        assertEquals(6, b.getU32());
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HIT_MELEE_RATING (ItemPrototype.h 16).
     * Autoequip Tom's Boots 1 32954 (+15 melee hit) must write 15 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE (Unit.h 5). Type 16 must not dump into
     * combined hitRating (that would make CR_HIT_MELEE 30 once type 17 also applies).
     */
    @Test
    void tpSl14EquipAppliesMeleeHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "TomsBoots", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item boots = new Item(world.nextItemGuid(), Content.ITEM_TOMS_BOOTS_1);
        boots.inventoryType = 8;
        boots.slot = src;
        p.items.put((int) boots.guid, boots);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(boots));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(15, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HIT_RANGED_RATING (ItemPrototype.h 17).
     * Autoequip Tom's Boots 1 32954 (+15 ranged hit) must write 15 on self VALUES
     * PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_RANGED (Unit.h 6) and leave CR_HIT_MELEE at 15
     * from type 16 (do not dump 17 into the combined hitRating bucket).
     */
    @Test
    void tpSl14EquipAppliesRangedHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "TomsRanged", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item boots = new Item(world.nextItemGuid(), Content.ITEM_TOMS_BOOTS_1);
        boots.inventoryType = 8;
        boots.slot = src;
        p.items.put((int) boots.guid, boots);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(boots));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(15, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
        assertEquals(15, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_HEALTH (ItemPrototype.h 0 is mana, 1 is health).
     * Autoequip Test HP Ring 6673 (−60 health) must write 1 on self VALUES UNIT_FIELD_MAXHEALTH
     * (create 60 + (−60), clamped) and leave UNIT_FIELD_STAT2 at create 22.
     */
    @Test
    void tpSl14EquipAppliesItemHealth() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "HpRing", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item ring = new Item(world.nextItemGuid(), Content.ITEM_TEST_HP_RING);
        ring.inventoryType = 11;
        ring.slot = src;
        p.items.put((int) ring.guid, ring);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(ring));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(1, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXHEALTH));
        assertEquals(22, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
    }

    /**
     * TP-SL14-013 — Player::_ApplyItemBonuses ITEM_MOD_MANA (ItemPrototype.h 0).
     * Autoequip Test MP Ring 6674 (−60 mana) on a human mage must write 105 on self VALUES
     * UNIT_FIELD_MAXPOWER1 (create 165 + (−60)) and leave UNIT_FIELD_STAT3 at create 23.
     */
    @Test
    void tpSl14EquipAppliesItemMana() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "MpRing", 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item ring = new Item(world.nextItemGuid(), Content.ITEM_TEST_MP_RING);
        ring.inventoryType = 11;
        ring.slot = src;
        p.items.put((int) ring.guid, ring);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(ring));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(105, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_MAXPOWER1));
        assertEquals(23, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT3));
    }

    /**
     * TP-SL14-013 — Player::ApplyItemEquipSpell ITEM_SPELLTRIGGER_ON_EQUIP (ItemPrototype.h 1).
     * Autoequip Band of the Eternal Champion 29301 must write spell 14052 on self VALUES
     * UNIT_FIELD_AURA[0] (Attack Power 60).
     */
    @Test
    void tpSl14EquipAppliesOnEquipSpellAura() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "EternalAp", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item band = new Item(world.nextItemGuid(), Content.ITEM_BAND_OF_THE_ETERNAL_CHAMPION);
        band.inventoryType = 11;
        band.slot = src;
        p.items.put((int) band.guid, band);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(band));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        assertEquals(Content.SPELL_ATTACK_POWER_60, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_AURA));
    }

    /**
     * TP-SL14-013 — Player::ApplyItemEquipSpell(apply=false). CMSG_AUTOSTORE_BAG_ITEM of
     * Band of the Eternal Champion 29301 must clear UNIT_FIELD_AURA[0] on the self VALUES.
     */
    @Test
    void tpSl14UnequipRemovesOnEquipSpellAura() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "NoEternal", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item band = new Item(world.nextItemGuid(), Content.ITEM_BAND_OF_THE_ETERNAL_CHAMPION);
        band.inventoryType = 11;
        band.slot = src;
        p.items.put((int) band.guid, band);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(band));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int finger = band.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(finger);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_AURA));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false). CMSG_AUTOSTORE_BAG_ITEM from the chest
     * must put create STA 22 / armor 40 back on the self VALUES (inventory.md).
     */
    @Test
    void tpSl14UnequipReversesItemMods() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Unequipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int chest = vest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(chest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(22, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(40, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_HIT_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Destroyer Chestguard 30113 must put HIT 0 back on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE / CR_HIT_RANGED.
     */
    @Test
    void tpSl14UnequipReversesHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipHit", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_DEFENSE_SKILL_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Destroyer Chestguard 30113 must put DEFENSE 0
     * back on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL.
     */
    @Test
    void tpSl14UnequipReversesDefenseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipDef", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_DODGE_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Destroyer Chestguard 30113 must put DODGE 0
     * back on self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE.
     */
    @Test
    void tpSl14UnequipReversesDodgeRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipDodge", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_PARRY_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Onslaught Chestguard 30976 must write PARRY 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY (Unit.h 3).
     */
    @Test
    void tpSl14UnequipReversesParryRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipParry", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_BLOCK_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Onslaught Chestguard 30976 must write BLOCK 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK (Unit.h 4).
     */
    @Test
    void tpSl14UnequipReversesBlockRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipBlock", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_CRIT_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Destroyer Breastplate 30118 must write CRIT 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED.
     */
    @Test
    void tpSl14UnequipReversesCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipCrit", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_RESILIENCE_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Gladiator's Plate Chestpiece 24544 must write
     * RESILIENCE 0 on self VALUES CR_CRIT_TAKEN_MELEE / RANGED / SPELL (Unit.h 14 / 15 / 16).
     */
    @Test
    void tpSl14UnequipReversesResilienceRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipRes", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GLADIATORS_PLATE_CHESTPIECE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_HASTE_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Warharness of Reckless Fury 34215 must write
     * HASTE 0 on self VALUES CR_HASTE_MELEE / CR_HASTE_RANGED (Unit.h 17 / 18).
     */
    @Test
    void tpSl14UnequipReversesHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipHaste", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_WARHARNESS_OF_RECKLESS_FURY);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18));
    }

    /**
     * TP-SL14-013 — RemoveItem → _ApplyItemMods(false) ITEM_MOD_EXPERTISE_RATING.
     * CMSG_AUTOSTORE_BAG_ITEM of equipped Gauntlets of Enforcement 32280 must write
     * EXPERTISE 0 on self VALUES CR_EXPERTISE (Unit.h 23).
     */
    @Test
    void tpSl14UnequipReversesExpertiseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UnequipExpertise", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item gloves = new Item(world.nextItemGuid(), Content.ITEM_GAUNTLETS_OF_ENFORCEMENT);
        gloves.inventoryType = 10;
        gloves.slot = src;
        p.items.put((int) gloves.guid, gloves);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(gloves));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = gloves.slot;

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(dest);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23));
    }

    /**
     * TP-SL14-013 — Player::DestroyItem → _ApplyItemMods(false). CMSG_DESTROYITEM of
     * equipped Riverpaw Leather Vest 821 must put create STA 22 / armor 40 back on the
     * self VALUES (same deltas as autostore unequip).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesMods() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyGear", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item vest = new Item(world.nextItemGuid(), Content.ITEM_RIVERPAW_LEATHER_VEST);
        vest.inventoryType = 5;
        vest.slot = src;
        p.items.put((int) vest.guid, vest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(vest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int chest = vest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(chest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(22, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(40, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    /**
     * TP-SL14-013 — Player::DestroyItem → _ApplyItemMods(false) ITEM_MOD_HIT_RATING.
     * CMSG_DESTROYITEM of equipped Destroyer Chestguard 30113 must put HIT 0 back on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_HIT_MELEE / CR_HIT_RANGED.
     */
    @Test
    void tpSl14DestroyEquippedItemReversesHitRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyHit", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 5));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 6));
    }

    /**
     * TP-SL14-013 — Player::DestroyItem → _ApplyItemMods(false) ITEM_MOD_DEFENSE_SKILL_RATING.
     * CMSG_DESTROYITEM of equipped Destroyer Chestguard 30113 must put DEFENSE 0 back on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DEFENSE_SKILL.
     */
    @Test
    void tpSl14DestroyEquippedItemReversesDefenseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyDef", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 1));
    }

    /**
     * TP-SL14-013 — Player::DestroyItem → _ApplyItemMods(false) ITEM_MOD_DODGE_RATING.
     * CMSG_DESTROYITEM of equipped Destroyer Chestguard 30113 must put DODGE 0 back on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_DODGE.
     */
    @Test
    void tpSl14DestroyEquippedItemReversesDodgeRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyDodge", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 2));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_PARRY_RATING.
     * CMSG_DESTROYITEM of equipped Onslaught Chestguard 30976 must write PARRY 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_PARRY (Unit.h 3).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesParryRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyParry", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 3));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_BLOCK_RATING.
     * CMSG_DESTROYITEM of equipped Onslaught Chestguard 30976 must write BLOCK 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_BLOCK (Unit.h 4).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesBlockRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyBlock", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_ONSLAUGHT_CHESTGUARD);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 4));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_CRIT_RATING.
     * CMSG_DESTROYITEM of equipped Destroyer Breastplate 30118 must write CRIT 0 on
     * self VALUES PLAYER_FIELD_COMBAT_RATING_1 + CR_CRIT_MELEE / CR_CRIT_RANGED.
     */
    @Test
    void tpSl14DestroyEquippedItemReversesCritRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyCrit", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_DESTROYER_BREASTPLATE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 8));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 9));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_RESILIENCE_RATING.
     * CMSG_DESTROYITEM of equipped Gladiator's Plate Chestpiece 24544 must write RESILIENCE 0
     * on self VALUES CR_CRIT_TAKEN_MELEE / RANGED / SPELL (Unit.h 14 / 15 / 16).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesResilienceRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyRes", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_GLADIATORS_PLATE_CHESTPIECE);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 14));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 15));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 16));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_HASTE_RATING.
     * CMSG_DESTROYITEM of equipped Warharness of Reckless Fury 34215 must write HASTE 0
     * on self VALUES CR_HASTE_MELEE / CR_HASTE_RANGED (Unit.h 17 / 18).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesHasteRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyHaste", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item chest = new Item(world.nextItemGuid(), Content.ITEM_WARHARNESS_OF_RECKLESS_FURY);
        chest.inventoryType = 5;
        chest.slot = src;
        p.items.put((int) chest.guid, chest);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(chest));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = chest.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 17));
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 18));
    }

    /**
     * TP-SL14-013 — DestroyItem → _ApplyItemMods(false) ITEM_MOD_EXPERTISE_RATING.
     * CMSG_DESTROYITEM of equipped Gauntlets of Enforcement 32280 must write EXPERTISE 0
     * on self VALUES CR_EXPERTISE (Unit.h 23).
     */
    @Test
    void tpSl14DestroyEquippedItemReversesExpertiseRating() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "DestroyExpertise", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        int src = p.firstFreeBagSlot();
        Item gloves = new Item(world.nextItemGuid(), Content.ITEM_GAUNTLETS_OF_ENFORCEMENT);
        gloves.inventoryType = 10;
        gloves.slot = src;
        p.items.put((int) gloves.guid, gloves);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(gloves));

        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());
        int dest = gloves.slot;

        client.clear();
        WowBuffer destroy = new WowBuffer(3);
        destroy.putU8(0);
        destroy.putU8(dest);
        destroy.putU8(0);
        client.handle(world, Opcodes.CMSG_DESTROYITEM, destroy.array());
        assertEquals(0, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COMBAT_RATING_1 + 23));
    }

    @Test
    void tpSl14AutoequipBagOpensContainer() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BagEquipper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item pouch = new Item(world.nextItemGuid(), Content.ITEM_SMALL_BROWN_POUCH);
        pouch.inventoryType = 18;
        pouch.slot = src;
        p.items.put((int) pouch.guid, pouch);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(pouch));

        client.clear();
        WowBuffer equip = new WowBuffer(2);
        equip.putU8(0);
        equip.putU8(src);
        client.handle(world, Opcodes.CMSG_AUTOEQUIP_ITEM, equip.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertTrue(client.saw(Opcodes.SMSG_OPEN_CONTAINER));
        WowBuffer opened = new WowBuffer(client.payload(Opcodes.SMSG_OPEN_CONTAINER));
        assertEquals(UpdateBuilder.itemGuid(pouch), opened.getU64());
    }

    @Test
    void tpSl14AutostoreBagItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BagStorer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = src;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(sword));
        int dest = p.firstFreeBagSlot();

        client.clear();
        WowBuffer store = new WowBuffer(3);
        store.putU8(0);
        store.putU8(src);
        store.putU8(0);
        client.handle(world, Opcodes.CMSG_AUTOSTORE_BAG_ITEM, store.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(src)));
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(dest)));
    }

    @Test
    void tpSl14ReadItem() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Reader", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate letterTpl = new ObjectMgr.ItemTemplate();
        letterTpl.entry = Content.ITEM_DUSTY_UNSENT_LETTER;
        letterTpl.pageText = Content.PAGE_TEXT_STALVAN_CRILLIAN;
        world.objectMgr.items.put(letterTpl.entry, letterTpl);

        int slot = p.firstFreeBagSlot();
        Item letter = new Item(world.nextItemGuid(), Content.ITEM_DUSTY_UNSENT_LETTER);
        letter.slot = slot;
        p.items.put((int) letter.guid, letter);
        p.setGuid(invSlotField(slot), UpdateBuilder.itemGuid(letter));

        client.clear();
        WowBuffer read = new WowBuffer(2);
        read.putU8(0);
        read.putU8(slot);
        client.handle(world, Opcodes.CMSG_READ_ITEM, read.array());

        assertTrue(client.saw(Opcodes.SMSG_READ_ITEM_OK));
        assertFalse(client.saw(Opcodes.SMSG_READ_ITEM_FAILED));
        WowBuffer ok = new WowBuffer(client.payload(Opcodes.SMSG_READ_ITEM_OK));
        assertEquals(UpdateBuilder.itemGuid(letter), ok.getU64());
    }

    @Test
    void tpSl14OpenItem() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Opener", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int slot = p.firstFreeBagSlot();
        Item crate = new Item(world.nextItemGuid(), Content.ITEM_DENTED_CRATE);
        crate.slot = slot;
        p.items.put((int) crate.guid, crate);
        p.setGuid(invSlotField(slot), UpdateBuilder.itemGuid(crate));

        client.clear();
        WowBuffer open = new WowBuffer(2);
        open.putU8(0);
        open.putU8(slot);
        client.handle(world, Opcodes.CMSG_OPEN_ITEM, open.array());

        assertTrue(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
        WowBuffer loot = new WowBuffer(client.payload(Opcodes.SMSG_LOOT_RESPONSE));
        assertEquals(UpdateBuilder.itemGuid(crate), loot.getU64());
        assertEquals(2, loot.getU8());
        assertEquals(0, loot.getU32());
        assertEquals(0, loot.getU8());
    }

    @Test
    void tpSl14WrapItem() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Wrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(0L, guidAt(update, invSlotField(paperSlot)));
        assertEquals(UpdateBuilder.itemGuid(sword), guidAt(update, invSlotField(giftSlot)));
        assertEquals(Content.ITEM_DYNFLAG_WRAPPED, sword.flags);
    }

    @Test
    void tpSl14WrapItemConsumesOnePaper() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "StackWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        paper.count = 2;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(UpdateBuilder.itemGuid(paper), guidAt(update, invSlotField(paperSlot)));
        assertEquals(1, paper.count);
        assertEquals(Content.ITEM_DYNFLAG_WRAPPED, sword.flags);
    }

    @Test
    void tpSl14WrapItemWhenEquippedShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "EquipWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(Player.EQUIPMENT_SLOT_MAINHAND), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(Player.EQUIPMENT_SLOT_MAINHAND);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, sword.flags);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14WrapItemWhenStackNotOneShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "StackGift", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item swords = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        swords.slot = giftSlot;
        swords.count = 2;
        p.items.put((int) swords.guid, swords);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(swords));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, swords.flags);
        assertEquals(2, swords.count);
        assertTrue(p.items.containsKey((int) paper.guid));
    }

    @Test
    void tpSl14WrapItemWhenAlreadyWrappedShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rewrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        sword.flags = Content.ITEM_DYNFLAG_WRAPPED;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(Content.ITEM_DYNFLAG_WRAPPED, sword.flags);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14WrapItemWhenBagShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BagWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item pouch = new Item(world.nextItemGuid(), Content.ITEM_SMALL_BROWN_POUCH);
        pouch.slot = giftSlot;
        pouch.inventoryType = 18;
        p.items.put((int) pouch.guid, pouch);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(pouch));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, pouch.flags);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14WrapItemWhenSoulboundShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "BoundWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        sword.soulbound = true;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, sword.flags);
        assertTrue(sword.soulbound);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14WrapItemWhenUniqueMaxCountShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "UniqueWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        world.objectMgr.items.get(Content.ITEM_WORN_SHORTSWORD).maxCount = 1;

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, sword.flags);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14WrapItemWhenChannelingShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "ChannelWrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.channeling = true;

        ObjectMgr.ItemTemplate paperTpl = new ObjectMgr.ItemTemplate();
        paperTpl.entry = Content.ITEM_RED_RIBBONED_WRAPPING_PAPER;
        paperTpl.flags = Content.ITEM_FLAG_IS_WRAPPER;
        paperTpl.stackable = 20;
        world.objectMgr.items.put(paperTpl.entry, paperTpl);

        int paperSlot = p.firstFreeBagSlot();
        Item paper = new Item(world.nextItemGuid(), Content.ITEM_RED_RIBBONED_WRAPPING_PAPER);
        paper.slot = paperSlot;
        p.items.put((int) paper.guid, paper);
        p.setGuid(invSlotField(paperSlot), UpdateBuilder.itemGuid(paper));

        int giftSlot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = giftSlot;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(giftSlot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer wrap = new WowBuffer(4);
        wrap.putU8(0);
        wrap.putU8(paperSlot);
        wrap.putU8(0);
        wrap.putU8(giftSlot);
        client.handle(world, Opcodes.CMSG_WRAP_ITEM, wrap.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, sword.flags);
        assertTrue(p.channeling);
        assertTrue(p.items.containsKey((int) paper.guid));
        assertEquals(1, paper.count);
    }

    @Test
    void tpSl14CancelTempEnchantment() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Enchanter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        sword.tempEnchant = 1;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(Player.EQUIPMENT_SLOT_MAINHAND), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer cancel = new WowBuffer(4);
        cancel.putU32(Player.EQUIPMENT_SLOT_MAINHAND);
        client.handle(world, Opcodes.CMSG_CANCEL_TEMP_ENCHANTMENT, cancel.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        assertEquals(0, sword.tempEnchant);
    }

    @Test
    void tpSl14OpenWrappedItemUnwraps() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Unwrapper", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int slot = p.firstFreeBagSlot();
        Item sword = new Item(world.nextItemGuid(), Content.ITEM_WORN_SHORTSWORD);
        sword.slot = slot;
        sword.flags = Content.ITEM_DYNFLAG_WRAPPED;
        p.items.put((int) sword.guid, sword);
        p.setGuid(invSlotField(slot), UpdateBuilder.itemGuid(sword));

        client.clear();
        WowBuffer open = new WowBuffer(2);
        open.putU8(0);
        open.putU8(slot);
        client.handle(world, Opcodes.CMSG_OPEN_ITEM, open.array());

        assertFalse(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
        assertEquals(0, sword.flags);
        assertEquals(Content.ITEM_WORN_SHORTSWORD, sword.entry);
    }

    @Test
    void tpSl14SetAmmo() throws Exception {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Archer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();

        int src = p.firstFreeBagSlot();
        Item arrows = new Item(world.nextItemGuid(), Content.ITEM_ROUGH_ARROW);
        arrows.slot = src;
        p.items.put((int) arrows.guid, arrows);
        p.setGuid(invSlotField(src), UpdateBuilder.itemGuid(arrows));

        client.clear();
        WowBuffer ammo = new WowBuffer(4);
        ammo.putU32(Content.ITEM_ROUGH_ARROW);
        client.handle(world, Opcodes.CMSG_SET_AMMO, ammo.array());

        assertFalse(client.saw(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE));
        byte[] update = lastValuesUpdate(client);
        assertEquals(Content.ITEM_ROUGH_ARROW, intAt(update, UpdateFields.PLAYER_AMMO_ID));
    }

    @Test
    void tpSl14TrainerBuySpell() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Trainee", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature trainer = find(world, Content.NPC_LLANE_BESHERE);
        assertNotNull(trainer);
        p.relocate(trainer.x, trainer.y, trainer.z, trainer.o);
        p.setMoney(Content.TRAINER_SPELL_BATTLE_SHOUT_COST);
        client.clear();
        WowBuffer buy = new WowBuffer(12);
        buy.putU64(trainer.guid);
        buy.putU32(Content.SPELL_BATTLE_SHOUT);
        client.handle(world, Opcodes.CMSG_TRAINER_BUY_SPELL, buy.array());

        assertTrue(client.saw(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        WowBuffer ok = new WowBuffer(client.payload(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertEquals(trainer.guid, ok.getU64());
        assertEquals(Content.SPELL_BATTLE_SHOUT, ok.getU32());
        assertEquals(Content.SPELL_BATTLE_SHOUT, WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_LEARNED_SPELL), 0));
        assertEquals(0, p.money);
    }

    @Test
    void tpSl14ActivateTaxi() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Flyer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.relocate(master.x, master.y, master.z, master.o);
        p.learnTaxi(Content.TAXI_STORMWIND);
        p.learnTaxi(Content.TAXI_IRONFORGE);
        client.clear();
        WowBuffer taxi = new WowBuffer(16);
        taxi.putU64(master.guid);
        taxi.putU32(Content.TAXI_STORMWIND);
        taxi.putU32(Content.TAXI_IRONFORGE);
        client.handle(world, Opcodes.CMSG_ACTIVATETAXI, taxi.array());

        assertFalse(client.saw(Opcodes.SMSG_NEW_TAXI_PATH));
        assertTrue(client.saw(Opcodes.SMSG_ACTIVATETAXIREPLY));
        assertEquals(Content.ERR_TAXIOK, WowClientDouble.u32le(client.payload(Opcodes.SMSG_ACTIVATETAXIREPLY), 0));
        WowBuffer move = new WowBuffer(lastPayload(client, Opcodes.SMSG_MONSTER_MOVE));
        assertEquals(p.guid, move.getPackedGuid());
    }

    /**
     * TP-SL14-014 — TaxiHandler SendTaxiStatus. CMSG_TAXINODE_STATUS_QUERY Dungar 352 with
     * Stormwind 2 known → SMSG_TAXINODE_STATUS raw guid + known 1 (taxi.md).
     */
    @Test
    void tpSl14TaxiNodeStatusKnown() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "TaxiIcon", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.relocate(master.x, master.y, master.z, master.o);
        p.learnTaxi(Content.TAXI_STORMWIND);
        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(master.guid);
        client.handle(world, Opcodes.CMSG_TAXINODE_STATUS_QUERY, q.array());
        byte[] status = lastPayload(client, Opcodes.SMSG_TAXINODE_STATUS);
        WowBuffer b = new WowBuffer(status);
        assertEquals(master.guid, b.getU64());
        assertEquals(1, b.getU8());
    }

    /** TP-SL14-014 — SendTaxiStatus: GetCreature miss is silent (TaxiHandler.cpp). */
    @Test
    void tpSl14TaxiNodeStatusWhenCreatureMissingShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "NoMaster", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(0);
        client.handle(world, Opcodes.CMSG_TAXINODE_STATUS_QUERY, q.array());
        assertFalse(client.saw(Opcodes.SMSG_TAXINODE_STATUS));
        client.handle(world, Opcodes.CMSG_TAXINODE_STATUS_QUERY, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_TAXINODE_STATUS));
    }

    /**
     * TP-SL14-014 — HandleTaxiQueryAvailableNodes known node. Dungar 352 in range with
     * Stormwind 2 known → SMSG_SHOWTAXINODES unk 1, raw guid, curloc 2, 16×uint32 (taxi.md).
     */
    @Test
    void tpSl14TaxiQueryAvailableNodes() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "TaxiMap", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.relocate(master.x, master.y, master.z, master.o);
        p.learnTaxi(Content.TAXI_STORMWIND);
        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(master.guid);
        client.handle(world, Opcodes.CMSG_TAXIQUERYAVAILABLENODES, q.array());
        WowBuffer b = new WowBuffer(lastPayload(client, Opcodes.SMSG_SHOWTAXINODES));
        assertEquals(1, b.getU32());
        assertEquals(master.guid, b.getU64());
        assertEquals(Content.TAXI_STORMWIND, b.getU32());
        for (int i = 0; i < 16; i++) {
            assertEquals(p.taxiMask[i], b.getU32());
        }
        assertEquals(0, b.remaining());
        assertFalse(client.saw(Opcodes.SMSG_NEW_TAXI_PATH));
    }

    /**
     * TP-SL14-014 — SendLearnNewTaxiNode. Unknown nearest node → empty SMSG_NEW_TAXI_PATH
     * and SMSG_TAXINODE_STATUS known 1 instead of the menu (taxi.md).
     */
    @Test
    void tpSl14TaxiQueryAvailableNodesLearnsUnknown() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "NewPath", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.relocate(master.x, master.y, master.z, master.o);
        assertFalse(p.taxiKnown(Content.TAXI_STORMWIND));
        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(master.guid);
        client.handle(world, Opcodes.CMSG_TAXIQUERYAVAILABLENODES, q.array());
        assertTrue(client.saw(Opcodes.SMSG_NEW_TAXI_PATH));
        assertEquals(0, lastPayload(client, Opcodes.SMSG_NEW_TAXI_PATH).length);
        WowBuffer status = new WowBuffer(lastPayload(client, Opcodes.SMSG_TAXINODE_STATUS));
        assertEquals(master.guid, status.getU64());
        assertEquals(1, status.getU8());
        assertFalse(client.saw(Opcodes.SMSG_SHOWTAXINODES));
        assertTrue(p.taxiKnown(Content.TAXI_STORMWIND));
    }

    /** TP-SL14-014 — GetNPCIfCanInteractWith miss is silent (TaxiHandler.cpp). */
    @Test
    void tpSl14TaxiQueryAvailableNodesWhenOutOfRangeShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "TooFar", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Creature master = find(world, Content.NPC_DUNGAR_LONGDRINK);
        assertNotNull(master);
        p.learnTaxi(Content.TAXI_STORMWIND);
        client.clear();
        WowBuffer q = new WowBuffer(8);
        q.putU64(master.guid);
        client.handle(world, Opcodes.CMSG_TAXIQUERYAVAILABLENODES, q.array());
        assertFalse(client.saw(Opcodes.SMSG_SHOWTAXINODES));
        assertFalse(client.saw(Opcodes.SMSG_NEW_TAXI_PATH));
        client.handle(world, Opcodes.CMSG_TAXIQUERYAVAILABLENODES, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_SHOWTAXINODES));
    }

    /**
     * TP-SL14-014 — HandleItemTextQuery. CMSG_ITEM_TEXT_QUERY itemTextId →
     * SMSG_ITEM_TEXT_QUERY_RESPONSE id + C-string (misc-player.md).
     */
    @Test
    void tpSl14ItemTextQuery() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Letter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        world.objectMgr.itemTexts.put(1, "keep this");
        client.clear();
        WowBuffer q = new WowBuffer(12);
        q.putU32(1);
        q.putU32(0);
        q.putU32(0);
        client.handle(world, Opcodes.CMSG_ITEM_TEXT_QUERY, q.array());
        WowBuffer b = new WowBuffer(lastPayload(client, Opcodes.SMSG_ITEM_TEXT_QUERY_RESPONSE));
        assertEquals(1, b.getU32());
        assertEquals("keep this", b.getCString());
        assertEquals(0, b.remaining());
    }

    /** TP-SL14-014 — ObjectMgr::GetItemText miss still replies with the C++ fallback string. */
    @Test
    void tpSl14ItemTextQueryWhenMissingShouldReplyNoInfo() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "NoLetter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer q = new WowBuffer(12);
        q.putU32(99);
        q.putU32(0);
        q.putU32(0);
        client.handle(world, Opcodes.CMSG_ITEM_TEXT_QUERY, q.array());
        WowBuffer b = new WowBuffer(lastPayload(client, Opcodes.SMSG_ITEM_TEXT_QUERY_RESPONSE));
        assertEquals(99, b.getU32());
        assertEquals("There is no info for this item", b.getCString());
    }

    /**
     * TP-SL14-014 — HandleMountSpecialAnimOpcode. Empty CMSG_MOUNTSPECIAL_ANIM →
     * SMSG_MOUNTSPECIAL_ANIM raw guid to nearby, not self (movement.md; SendMessageToSet false).
     */
    @Test
    void tpSl14MountSpecialAnim() {
        World world = World.inMemory();
        WowClientDouble rider = new WowClientDouble();
        rider.connect(ACC);
        Player createdA = world.characters.create(ACC.id(), "Rearing", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        rider.login(world, createdA.guid);
        WowClientDouble watcher = new WowClientDouble();
        watcher.connect(ACC_B);
        Player createdB = world.characters.create(ACC_B.id(), "Watcher", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        watcher.login(world, createdB.guid);
        Player a = rider.session().player();
        Player b = watcher.session().player();
        b.relocate(a.x, a.y, a.z, a.o);
        rider.clear();
        watcher.clear();
        rider.handle(world, Opcodes.CMSG_MOUNTSPECIAL_ANIM, new byte[0]);
        assertFalse(rider.saw(Opcodes.SMSG_MOUNTSPECIAL_ANIM));
        WowBuffer pkt = new WowBuffer(lastPayload(watcher, Opcodes.SMSG_MOUNTSPECIAL_ANIM));
        assertEquals(a.guid, pkt.getU64());
        assertEquals(0, pkt.remaining());
    }

    /**
     * TP-SL14-014 — HandleCancelMountAuraOpcode / Unit::Unmount. Empty
     * CMSG_CANCEL_MOUNT_AURA while mounted → SMSG_DISMOUNT packed guid to the
     * visibility set including self (movement.md; SendMessageToSet true).
     */
    @Test
    void tpSl14CancelMountAuraDismount() {
        World world = World.inMemory();
        WowClientDouble rider = new WowClientDouble();
        rider.connect(ACC);
        Player createdA = world.characters.create(ACC.id(), "Dismounter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        rider.login(world, createdA.guid);
        WowClientDouble watcher = new WowClientDouble();
        watcher.connect(ACC_B);
        Player createdB = world.characters.create(ACC_B.id(), "SeesDismount", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        watcher.login(world, createdB.guid);
        Player a = rider.session().player();
        Player b = watcher.session().player();
        b.relocate(a.x, a.y, a.z, a.o);
        a.auras.add(new Unit.Aura(PvpObjectives.MOUNT_AURA, 0, 1));
        a.mounted = true;
        rider.clear();
        watcher.clear();
        rider.handle(world, Opcodes.CMSG_CANCEL_MOUNT_AURA, new byte[0]);
        WowBuffer self = new WowBuffer(lastPayload(rider, Opcodes.SMSG_DISMOUNT));
        assertEquals(a.guid, self.getPackedGuid());
        assertEquals(0, self.remaining());
        WowBuffer near = new WowBuffer(lastPayload(watcher, Opcodes.SMSG_DISMOUNT));
        assertEquals(a.guid, near.getPackedGuid());
        assertEquals(0, near.remaining());
    }

    @Test
    void tpSl14Weather() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Rain", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        ObjectMgr.ZoneWeather row = world.objectMgr.weather.get(p.zoneId);
        assertNotNull(row);
        byte[] payload = lastPayload(client, Opcodes.SMSG_WEATHER);
        assertEquals(row.state(), WowClientDouble.u32le(payload, 0));
        assertEquals(row.grade(), WowClientDouble.floatle(payload, 4), 0.001f);
        assertEquals(Content.WEATHER_INSTANT_SMOOTH, payload[8] & 0xFF);
    }

    @Test
    void tpSl14WeatherTimer() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Storm", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        world.objectMgr.weather.put(p.zoneId, new ObjectMgr.ZoneWeather(p.zoneId, Content.WEATHER_STATE_LIGHT_RAIN, 0.5f));
        client.clear();
        world.tick(10 * 60_000);

        assertTrue(client.saw(Opcodes.SMSG_WEATHER));
        byte[] payload = lastPayload(client, Opcodes.SMSG_WEATHER);
        assertEquals(Content.WEATHER_STATE_FINE, WowClientDouble.u32le(payload, 0));
        assertEquals(0f, WowClientDouble.floatle(payload, 4), 0.001f);
        assertEquals(Content.WEATHER_INSTANT_SMOOTH, payload[8] & 0xFF);
    }

    @Test
    void tpSl14GameEventStartsOnTick() {
        World world = World.inMemory();
        world.events.schedule(Content.GAME_EVENT_MIDSUMMER, 0, Long.MAX_VALUE);
        assertNull(find(world, 547, Content.NPC_LUMA_SKYMOTHER));
        world.tick(60_000);
        assertNotNull(find(world, 547, Content.NPC_LUMA_SKYMOTHER));
    }

    private static int invSlotField(int slot) {
        return UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + slot * 2;
    }

    private static byte[] lastValuesUpdate(WowClientDouble client) throws Exception {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            int op = client.opcodes.get(i);
            if (op == Opcodes.SMSG_UPDATE_OBJECT) {
                return client.payloads.get(i);
            }
            if (op == Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT) {
                return inflate(client.payloads.get(i));
            }
        }
        throw new AssertionError("no SMSG_UPDATE_OBJECT after inventory mutate");
    }

    private static byte[] inflate(byte[] compressed) throws Exception {
        int size = WowClientDouble.u32le(compressed, 0);
        Inflater inf = new Inflater();
        inf.setInput(compressed, 4, compressed.length - 4);
        byte[] out = new byte[size];
        inf.inflate(out);
        inf.end();
        return out;
    }

    private static long guidAt(byte[] payload, int field) {
        WowBuffer b = new WowBuffer(payload);
        b.getU32();
        b.getU8();
        assertEquals(UpdateBuilder.UPDATETYPE_VALUES, b.getU8());
        b.getPackedGuid();
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int written = 0;
        long low = 0;
        long high = 0;
        boolean sawLow = false;
        boolean sawHigh = false;
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) == 0) {
                continue;
            }
            int v = b.getU32();
            written++;
            if (i == field) {
                low = v & 0xFFFFFFFFL;
                sawLow = true;
            }
            if (i == field + 1) {
                high = v & 0xFFFFFFFFL;
                sawHigh = true;
            }
        }
        assertTrue(written > 0);
        assertTrue(sawLow && sawHigh);
        return low | (high << 32);
    }

    private static int intAt(byte[] payload, int field) {
        WowBuffer b = new WowBuffer(payload);
        b.getU32();
        b.getU8();
        assertEquals(UpdateBuilder.UPDATETYPE_VALUES, b.getU8());
        b.getPackedGuid();
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        boolean saw = false;
        int value = 0;
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) == 0) {
                continue;
            }
            int v = b.getU32();
            if (i == field) {
                value = v;
                saw = true;
            }
        }
        assertTrue(saw);
        return value;
    }

    private static Creature find(World world, int entry) {
        return find(world, 0, entry);
    }

    private static Creature find(World world, int map, int entry) {
        for (Creature c : world.map(map, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }

    @Test
    void tpSl14LearnTalent() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Talented", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, 1);
        client.clear();
        client.learnTalent(world, 124, 0);
        assertEquals(12282, WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_LEARNED_SPELL), 0));
    }

    @Test
    void tpSl14LearnTalentWhenNoPointsShouldStaySilent() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Untalented", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        client.learnTalent(world, 124, 0);
        assertFalse(client.saw(Opcodes.SMSG_LEARNED_SPELL));
    }

    @Test
    void tpSl14CorpseExpireOnTick() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Bones", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertNotNull(p.corpse);
        p.corpse.expireAtMs = world.nowMs() - 1;
        client.clear();
        world.tick(org.tbc.world.world.WorldTimers.CORPSES_MS);
        client.handle(world, Opcodes.MSG_CORPSE_QUERY, new byte[0]);
        byte[] q = lastPayload(client, Opcodes.MSG_CORPSE_QUERY);
        assertEquals(0, q[0] & 0xFF);
    }

    @Test
    void tpSl14CorpseExpireWhenTimerNotDueShouldKeepCorpse() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Fresh", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        p.corpse.expireAtMs = world.nowMs() - 1;
        world.tick(org.tbc.world.world.WorldTimers.CORPSES_MS - 1);
        client.clear();
        client.handle(world, Opcodes.MSG_CORPSE_QUERY, new byte[0]);
        byte[] q = lastPayload(client, Opcodes.MSG_CORPSE_QUERY);
        assertEquals(1, q[0] & 0xFF);
    }

    @Test
    void tpSl14GroupOfflineLeaderOnTick() {
        World world = World.inMemory();
        WowClientDouble a = new WowClientDouble();
        WowClientDouble b = new WowClientDouble();
        a.connect(ACC);
        b.connect(ACC_B);
        Player pa = world.characters.create(ACC.id(), "Lead", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player pb = world.characters.create(ACC_B.id(), "Mate", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        a.login(world, pa.guid);
        b.login(world, pb.guid);
        a.groupInvite(world, "Mate");
        b.groupAccept(world);
        Player lead = a.session().player();
        lead.group.leaderLastOnlineMs = world.nowMs() - 301_000;
        lead.session = null;
        b.clear();
        world.tick(org.tbc.world.world.WorldTimers.GROUPS_MS);
        byte[] set = lastPayload(b, Opcodes.SMSG_GROUP_SET_LEADER);
        assertEquals("Mate", new WowBuffer(set).getCString());
        assertEquals(pb.guid, b.session().player().group.leaderGuid);
    }

    @Test
    void tpSl14DeleteCharsOnTick() {
        World world = World.inMemory();
        Player p = world.characters.create(ACC.id(), "Gone", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        world.characters.markDeleted(p.guid, world.nowMs() - 31L * 24 * 60 * 60_000);
        assertEquals(1, world.characters.storedCount(ACC.id()));
        world.tick(org.tbc.world.world.WorldTimers.DELETECHARS_MS);
        assertEquals(0, world.characters.storedCount(ACC.id()));
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
