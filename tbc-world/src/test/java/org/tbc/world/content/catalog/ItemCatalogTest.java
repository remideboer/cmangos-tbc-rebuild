package org.tbc.world.content.catalog;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Narrow read-only item view over ObjectMgr (refactoring plan cycle 4.3). */
class ItemCatalogTest {
    @Test
    void itemWhenSeededEntryShouldReturnTemplate() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        ItemCatalog items = m;
        assertEquals("Worn Shortsword", items.item(Content.ITEM_WORN_SHORTSWORD).name);
    }

    @Test
    void itemWhenUnknownEntryShouldReturnNull() {
        ItemCatalog items = new ObjectMgr();
        assertNull(items.item(999_999));
    }
}
