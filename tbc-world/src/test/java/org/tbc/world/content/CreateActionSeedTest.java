package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** playercreateinfo_action seeded outside ObjectMgr (refactoring plan cycle 4.1). */
class CreateActionSeedTest {
    @Test
    void seedShouldPackActionAndTypeForHumanWarriorButtons() {
        ObjectMgr m = new ObjectMgr();
        CreateActionSeed.seed(m);
        int[] bar = m.createActions.get((int) ObjectMgr.key(1, 1));
        assertEquals(132, bar.length);
        assertEquals(6603, bar[72]);
        assertEquals(78, bar[73]);
        assertEquals(117 | (128 << 24), bar[83]);
        assertEquals(0, bar[0]);
    }

    @Test
    void seedShouldNotCreateBarsForUnknownRaceClass() {
        ObjectMgr m = new ObjectMgr();
        CreateActionSeed.seed(m);
        assertNull(m.createActions.get((int) ObjectMgr.key(1, 7)));
    }

    @Test
    void loadInMemoryShouldStillSeedCreateActions() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        assertEquals(6603, m.createActions.get((int) ObjectMgr.key(1, 1))[72]);
    }
}
