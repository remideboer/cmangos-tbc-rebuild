package org.tbc.world.persist;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterStoreNextItemGuidTest {
    @Test
    void nextItemGuidWhenInventoriesAlreadyCountedShouldNotRescan() {
        CharacterStore store = new CharacterStore(null);
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player p = store.create(1, "Bag", 1, 1, 0, 1, 1, 1, 1, 0, mgr);
        store.setOnline(p, true);
        long first = store.nextItemGuid();
        Item planted = new Item(99_999, 25);
        p.items.put(Guid.low(planted.guid), planted);
        assertEquals(first + 1, store.nextItemGuid());
    }
}
