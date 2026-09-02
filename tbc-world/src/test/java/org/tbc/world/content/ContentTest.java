package org.tbc.world.content;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTest {
    private ObjectMgr mgr;
    private Content content;
    private GameMap map;
    private Player p;
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();
    private long nextItem = 1;

    @BeforeEach
    void setUp() {
        mgr = new ObjectMgr();
        mgr.load(null, null);
        content = new Content(mgr);
        map = new GameMap(0, 0);
        p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        map.add(p);
        ops.clear();
        last.clear();
        nextItem = 1;
    }

    @Test
    void vendorBuyPushAndGold() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setMoney(70);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        content.listInventory(p, map, u64(vendor.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_LIST_INVENTORY));
        ops.clear();
        content.buy(p, map, buy(vendor.guid, Content.ITEM_WORN_SHORTSWORD, 1), false, nextItem++, this::capture);
        assertEquals(35, p.money);
        assertEquals(1, p.items.size());
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        ops.clear();
        content.buy(p, map, buyInSlot(vendor.guid, Content.ITEM_WORN_SHORTSWORD, p.guid, 23, 1), true, nextItem++, this::capture);
        assertEquals(0, p.money);
        assertEquals(2, p.items.size());
    }

    @Test
    void questAcceptQueryComplete() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.gossipHello(p, map, u64(giver.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_MESSAGE));
        content.queryQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS));
        content.queryQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        ops.clear();
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(Content.QUEST_A_THREAT_WITHIN, p.questLogId[0]);
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        ops.clear();
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        ops.clear();
        content.completeQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertEquals(0, p.questLogId[0]);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTUPDATE_COMPLETE));
        assertTrue(ops.contains(Opcodes.SMSG_QUESTGIVER_QUEST_COMPLETE));
    }

    @Test
    void ignoresTruncationRangeAndWrongNpc() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        Creature kobold = spawn(6, 0, 0);
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        content.gossipHello(p, map, new WowBuffer(3), this::capture);
        content.listInventory(p, map, new WowBuffer(3), this::capture);
        content.buy(p, map, new WowBuffer(8), false, nextItem++, this::capture);
        content.queryQuest(p, map, new WowBuffer(8), this::capture);
        content.acceptQuest(p, map, new WowBuffer(8), this::capture);
        content.completeQuest(p, map, new WowBuffer(8), this::capture);
        content.gossipHello(p, map, u64(0), this::capture);
        content.gossipHello(p, map, u64(99), this::capture);
        content.listInventory(p, map, u64(0), this::capture);
        content.listInventory(p, map, u64(99), this::capture);
        content.queryQuest(p, map, quest(99, 783), this::capture);
        content.acceptQuest(p, map, quest(99, 783), this::capture);
        content.completeQuest(p, map, quest(99, 783), this::capture);
        p.relocate(20, 0, 0, 0);
        content.gossipHello(p, map, u64(vendor.guid), this::capture);
        content.listInventory(p, map, u64(vendor.guid), this::capture);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        content.queryQuest(p, map, quest(giver.guid, 783), this::capture);
        content.acceptQuest(p, map, quest(giver.guid, 783), this::capture);
        content.completeQuest(p, map, quest(turn.guid, 783), this::capture);
        p.relocate(0, 0, 0, 0);
        content.listInventory(p, map, u64(kobold.guid), this::capture);
        content.buy(p, map, buy(kobold.guid, 25, 1), false, nextItem++, this::capture);
        content.buy(p, map, buy(99, 25, 1), false, nextItem++, this::capture);
        content.queryQuest(p, map, quest(kobold.guid, 783), this::capture);
        content.acceptQuest(p, map, quest(turn.guid, 783), this::capture);
        content.completeQuest(p, map, quest(giver.guid, 783), this::capture);
        content.completeQuest(p, map, quest(turn.guid, 783), this::capture);
        assertTrue(ops.isEmpty());
        assertTrue(p.items.isEmpty());
    }

    @Test
    void buyFailuresAndSlotSkip() {
        Creature vendor = spawn(Content.NPC_CORINA_STEELE, 0, 0);
        p.setMoney(50);
        content.buy(p, map, buy(vendor.guid, 26, 1), false, nextItem++, this::capture);
        mgr.vendorItems.put(Content.NPC_CORINA_STEELE, new ArrayList<>(List.of(25, 99999)));
        content.buy(p, map, buy(vendor.guid, 99999, 1), false, nextItem++, this::capture);
        p.setMoney(0);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        assertEquals(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE, ops.get(ops.size() - 1).intValue());
        assertEquals(Content.EQUIP_ERR_NOT_ENOUGH_MONEY, last.get(Opcodes.SMSG_INVENTORY_CHANGE_FAILURE)[0] & 0xFF);
        p.setMoney(100);
        fillBackpack();
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
        assertEquals(16, p.items.size());
        p.items.clear();
        Item otherBag = new Item(50, 25);
        otherBag.bag = 1;
        otherBag.slot = 23;
        p.items.put(50, otherBag);
        ops.clear();
        content.buy(p, map, buyExact12(vendor.guid, 25), false, nextItem++, this::capture);
        assertEquals(2, p.items.size());
        ops.clear();
        p.setMoney(100);
        content.buy(p, map, buyCount(vendor.guid, 25, 0), false, nextItem++, this::capture);
        content.buy(p, map, buyShortInSlot(vendor.guid, 25), true, nextItem++, this::capture);
        mgr.vendorItems.remove(Content.NPC_CORINA_STEELE);
        content.buy(p, map, buy(vendor.guid, 25, 1), false, nextItem++, this::capture);
    }

    @Test
    void questLogFullMissingTemplateAndEncoders() {
        Creature giver = spawn(Content.NPC_DEPUTY_WILLEM, 0, 0);
        Creature turn = spawn(Content.NPC_MARSHAL_MCBRIDE, 0, 0);
        Creature emptyVendor = vendor(9001);
        Creature gossipOnly = spawn(Content.NPC_MARSHAL_DUGHAN, 0, 0);
        mgr.questGivers.put(Content.NPC_DEPUTY_WILLEM, new ArrayList<>(List.of(Content.QUEST_A_THREAT_WITHIN, 1)));
        content.acceptQuest(p, map, quest(giver.guid, 1), this::capture);
        for (int i = 0; i < 25; i++) {
            p.questLogId[i] = 100 + i;
        }
        content.acceptQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_QUESTLOG_FULL));
        p.questLogId[24] = Content.QUEST_A_THREAT_WITHIN;
        mgr.quests.remove(Content.QUEST_A_THREAT_WITHIN);
        content.completeQuest(p, map, quest(turn.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        content.queryQuest(p, map, quest(giver.guid, Content.QUEST_A_THREAT_WITHIN), this::capture);
        mgr.questGivers.put(Content.NPC_MARSHAL_DUGHAN, new ArrayList<>(List.of(404)));
        content.gossipHello(p, map, u64(gossipOnly.guid), this::capture);
        content.listInventory(p, map, u64(emptyVendor.guid), this::capture);
        mgr.vendorItems.put(9001, new ArrayList<>(List.of(99999)));
        content.listInventory(p, map, u64(emptyVendor.guid), this::capture);
        assertTrue(content.gives(Content.NPC_DEPUTY_WILLEM, Content.QUEST_A_THREAT_WITHIN)
                || content.involves(Content.NPC_MARSHAL_MCBRIDE, Content.QUEST_A_THREAT_WITHIN));
        assertTrue(content.offersOrInvolves(Content.NPC_DEPUTY_WILLEM, Content.QUEST_A_THREAT_WITHIN));
        assertTrue(content.offersOrInvolves(Content.NPC_MARSHAL_MCBRIDE, Content.QUEST_A_THREAT_WITHIN));
        assertFalse(content.offersOrInvolves(6, Content.QUEST_A_THREAT_WITHIN));
        assertFalse(content.gives(6, 783));
        assertFalse(content.gives(Content.NPC_DEPUTY_WILLEM, 404));
        assertFalse(content.involves(6, 783));
        assertFalse(content.involves(Content.NPC_MARSHAL_MCBRIDE, 404));
        assertEquals(-1, Content.slotOf(p, 1));
        assertEquals(24, Content.slotOf(p, Content.QUEST_A_THREAT_WITHIN));
        assertEquals(-1, Content.freeSlot(p));
        p.questLogId[0] = 0;
        assertEquals(0, Content.freeSlot(p));
        assertTrue(Content.outOfRange(p, farCreature()));
        assertFalse(Content.outOfRange(p, emptyVendor));
        assertNull(Content.creature(map, 0));
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
    }

    private Creature spawn(int entry, float x, float y) {
        Creature c = mgr.spawnCreature(entry, 0, x, y, 0, 0, null);
        map.add(c);
        return c;
    }

    private Creature vendor(int entry) {
        mgr.creatures.put(entry, new ObjectMgr.CreatureTemplate(entry, "Vendor", 0, 12, 100, 1,
                Content.UNIT_NPC_FLAG_VENDOR, "", "", 0));
        return spawn(entry, 0, 0);
    }

    private Creature farCreature() {
        Creature c = new Creature();
        c.guid = 8;
        c.relocate(40, 0, 0, 0);
        return c;
    }

    private void fillBackpack() {
        for (int slot = Content.BACKPACK_START; slot < Content.BACKPACK_END; slot++) {
            Item it = new Item(100 + slot, 25);
            it.bag = 0;
            it.slot = slot;
            p.items.put(Guid.low(it.guid), it);
        }
    }

    private static WowBuffer u64(long guid) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(guid);
        return b;
    }

    private static WowBuffer buy(long vendor, int itemId, int count) {
        WowBuffer b = new WowBuffer(14);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU8(count);
        b.putU8(1);
        return b;
    }

    private static WowBuffer buyCount(long vendor, int itemId, int count) {
        return buy(vendor, itemId, count);
    }

    private static WowBuffer buyExact12(long vendor, int itemId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(vendor);
        b.putU32(itemId);
        return b;
    }

    private static WowBuffer buyShortInSlot(long vendor, int itemId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(vendor);
        b.putU32(itemId);
        return b;
    }

    private static WowBuffer buyInSlot(long vendor, int itemId, long bagGuid, int bagSlot, int count) {
        WowBuffer b = new WowBuffer(22);
        b.putU64(vendor);
        b.putU32(itemId);
        b.putU64(bagGuid);
        b.putU8(bagSlot);
        b.putU8(count);
        return b;
    }

    private static WowBuffer quest(long guid, int questId) {
        WowBuffer b = new WowBuffer(12);
        b.putU64(guid);
        b.putU32(questId);
        return b;
    }
}
