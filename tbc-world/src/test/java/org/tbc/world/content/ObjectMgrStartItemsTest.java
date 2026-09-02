package org.tbc.world.content;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrStartItemsTest {
    @TempDir
    Path tmp;

    @Test
    void sqlCreateItemsEquipMainhandThenOverflowBackpack() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.createItems.put((int) ObjectMgr.key(1, 1), List.of(new ObjectMgr.CreateItem(25, 2)));
        Player p = player(1, 1, 0);
        AtomicLong next = new AtomicLong(10);
        mgr.giveStartItems(p, next::getAndIncrement);
        Item mh = p.itemAt(0, 15);
        assertNotNull(mh);
        assertEquals(25, mh.entry);
        assertEquals(10, mh.guid);
        Item bag = p.itemAt(0, 23);
        assertNotNull(bag);
        assertEquals(25, bag.entry);
        assertEquals(11, bag.guid);
        assertEquals(12, next.get());
    }

    @Test
    void charStartOutfitDbcWhenPresent() throws Exception {
        Path dbcDir = tmp.resolve("dbc");
        Files.createDirectories(dbcDir);
        writeOutfit(dbcDir.resolve("CharStartOutfit.dbc"), 1 | (1 << 8), 25);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null, tmp);
        Player p = player(1, 1, 0);
        mgr.giveStartItems(p, () -> 3L);
        Item mh = p.itemAt(0, 15);
        assertNotNull(mh);
        assertEquals(25, mh.entry);
        assertEquals(1542, mh.displayId);
    }

    @Test
    void missingOutfitDbcIsNoOp() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null, tmp);
        assertTrue(mgr.startOutfit.isEmpty());
        Player p = player(1, 1, 0);
        mgr.giveStartItems(p, () -> 1L);
        assertNull(p.itemAt(0, 15));
    }

    @Test
    void skipUnknownItemAndFillVisuals() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.createItems.put((int) ObjectMgr.key(1, 1), List.of(
                new ObjectMgr.CreateItem(999999, 1),
                new ObjectMgr.CreateItem(25, 1)));
        Player p = player(1, 1, 0);
        mgr.giveStartItems(p, () -> 5L);
        assertEquals(1, p.items.size());
        Item it = p.items.get(5);
        it.displayId = 0;
        it.inventoryType = 0;
        mgr.fillItemVisuals(p);
        assertEquals(1542, it.displayId);
        assertEquals(21, it.inventoryType);
    }

    private static Player player(int race, int clazz, int gender) {
        Player p = new Player();
        p.guid = 1;
        p.race = race;
        p.clazz = clazz;
        p.gender = gender;
        return p;
    }

    private static void writeOutfit(Path file, int rcg, int itemId) throws Exception {
        int recSize = 56;
        ByteBuffer b = ByteBuffer.allocate(20 + recSize + 1).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(14);
        b.putInt(recSize);
        b.putInt(1);
        b.putInt(1);
        b.putInt(rcg);
        b.putInt(itemId);
        for (int i = 0; i < 11; i++) {
            b.putInt(0);
        }
        b.put((byte) 0);
        Files.write(file, b.array());
    }
}
