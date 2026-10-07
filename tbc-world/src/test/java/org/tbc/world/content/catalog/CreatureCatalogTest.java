package org.tbc.world.content.catalog;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Narrow read-only creature_template view over ObjectMgr (refactoring plan cycle 4.3). */
class CreatureCatalogTest {
    @Test
    void creatureWhenSeededEntryShouldReturnTemplate() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        CreatureCatalog creatures = m;
        assertEquals("Garrick Padfoot", creatures.creature(103).name());
    }

    @Test
    void creatureWhenUnknownEntryShouldReturnNull() {
        CreatureCatalog creatures = new ObjectMgr();
        assertNull(creatures.creature(999_999));
    }
}
