package org.tbc.world.content;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.session.QueryHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CMaNGOS Player::_CanStoreItem merge: stackable food (Tough Hunk of Bread 4540) must share a bag
 * slot up to ItemTemplate.stackable, and SMSG_ITEM_QUERY must telegraph that max stack.
 */
class ObjectMgrItemStackTest {
    private static final int BREAD = Content.ITEM_TOUGH_HUNK_OF_BREAD;

    @Test
    void toughHunkOfBreadWhenQueriedShouldReportStackable20() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.ItemTemplate t = mgr.items.get(BREAD);
        assertNotNull(t);
        assertEquals(20, t.stackable);
        byte[] wire = QueryHandler.encodeItemQuery(t);
        int stackableOff = findStackableOffset(wire, t);
        assertEquals(20, u32le(wire, stackableOff));
    }

    @Test
    void storeNewItemWhenPartialBreadStackShouldMergeInsteadOfNewSlot() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1;
        AtomicLong next = new AtomicLong(100);
        List<ObjectMgr.StoredItem> first = mgr.storeNewItem(p, BREAD, 5, next::getAndIncrement);
        assertEquals(1, first.size());
        assertTrue(first.get(0).created());
        assertEquals(5, first.get(0).item().count);
        assertEquals(1, p.items.size());

        List<ObjectMgr.StoredItem> second = mgr.storeNewItem(p, BREAD, 3, next::getAndIncrement);
        assertEquals(1, second.size());
        assertFalse(second.get(0).created(), "must merge onto existing stack");
        assertEquals(8, second.get(0).item().count);
        assertEquals(1, p.items.size(), "still one bag slot");
        assertEquals(8, p.itemAt(0, first.get(0).item().slot).count);
    }

    @Test
    void storeNewItemWhenStackFullShouldOpenNewSlot() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1;
        AtomicLong next = new AtomicLong(200);
        mgr.storeNewItem(p, BREAD, 20, next::getAndIncrement);
        List<ObjectMgr.StoredItem> overflow = mgr.storeNewItem(p, BREAD, 2, next::getAndIncrement);
        assertEquals(1, overflow.size());
        assertTrue(overflow.get(0).created());
        assertEquals(2, overflow.get(0).item().count);
        assertEquals(2, p.items.size());
    }

    @Test
    void storeNewItemWhenUnknownEntryShouldStillCreateNonStackable() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = new Player();
        p.guid = 1;
        AtomicLong next = new AtomicLong(400);
        int unknown = 9_990_001;
        List<ObjectMgr.StoredItem> stored = mgr.storeNewItem(p, unknown, 1, next::getAndIncrement);
        assertEquals(1, stored.size());
        assertTrue(stored.get(0).created());
        assertEquals(1, stored.get(0).item().count);
        assertEquals(1, p.items.size());
    }

    @Test
    void giveStartItemsWhenMultipleBreadRowsShouldCollapseToOneStack() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        int rcg = (1) | (1 << 8) | (0 << 16);
        mgr.startOutfit.put(rcg, List.of(BREAD, BREAD, BREAD, BREAD));
        Player p = new Player();
        p.guid = 1;
        p.race = 1;
        p.clazz = 1;
        p.gender = 0;
        AtomicLong next = new AtomicLong(300);
        mgr.giveStartItems(p, next::getAndIncrement);
        long breadStacks = p.items.values().stream().filter(i -> i.entry == BREAD).count();
        assertEquals(1, breadStacks);
        Item stack = p.items.values().stream().filter(i -> i.entry == BREAD).findFirst().orElseThrow();
        assertEquals(4, stack.count);
    }

    /** Locate stackable uint32 after the name cstrings in encodeItemQuery. */
    private static int findStackableOffset(byte[] wire, ObjectMgr.ItemTemplate t) {
        // entry..requiredReputationRank: 19×u32 before name, then name+3 empty cstrings, then
        // displayId..requiredReputationRank already counted differently — use QueryHandler layout:
        // 4 (entry) + 3×4 (class/subclass/unk) + name + 3 zero bytes + then fields until stackable.
        int off = 4 + 12;
        while (off < wire.length && wire[off] != 0) {
            off++;
        }
        off++; // name NUL
        off += 3; // three empty name slots
        // displayId, quality, flags, buy, sell, invType, allowClass, allowRace, itemLevel,
        // reqLevel, reqSkill, reqSkillRank, reqSpell, reqHonor, reqCity, reqRepFaction, reqRepRank,
        // maxCount, stackable
        off += 17 * 4;
        assertEquals(t.maxCount, u32le(wire, off));
        return off + 4;
    }

    private static int u32le(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }
}
