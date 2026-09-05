package org.tbc.world.persist;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPersistTest {
    @Test
    void copyEmptyItemsAndBuybackExcluded() {
        Player src = new Player();
        src.guid = 7;
        src.accountId = 1;
        src.name = "Copy";
        src.money = 9;
        src.resting = true;
        src.restBonus = 12.5f;
        src.deleteDateMs = 99;
        src.actionButtons[3] = 78;
        Item back = new Item(99, 25);
        src.buyback.put(99, back);
        Player d = PlayerPersist.copy(src);
        assertNotSame(src, d);
        assertEquals(9, d.money);
        assertEquals(12.5f, d.restBonus);
        assertTrue(d.resting);
        assertEquals(99, d.deleteDateMs);
        assertEquals(78, d.actionButtons[3]);
        assertTrue(d.items.isEmpty());
        assertTrue(d.buyback.isEmpty());
        src.money = 1;
        assertEquals(9, d.money);
    }

    @Test
    void copyClonesItems() {
        Player src = new Player();
        src.guid = 8;
        Item it = new Item(5, 25);
        it.count = 3;
        it.bag = 0;
        it.slot = 23;
        src.items.put(5, it);
        Player d = PlayerPersist.copy(src);
        assertEquals(1, d.items.size());
        Item c = d.items.get(5);
        assertNotSame(it, c);
        assertEquals(25, c.entry);
        assertEquals(3, c.count);
        assertEquals(23, c.slot);
        it.count = 9;
        assertEquals(3, c.count);
    }
}
