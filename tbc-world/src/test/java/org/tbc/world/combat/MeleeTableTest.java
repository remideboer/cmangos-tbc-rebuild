package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MeleeTableTest {
    @Test
    void sequentialSidesAndGlance() {
        Player a = new Player();
        a.level = 1;
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        assertEquals(MeleeTable.Outcome.MISS, table(0.01).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.DODGE, table(0.06).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.PARRY, table(0.11).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.BLOCK, table(0.16).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        v.level = 11;
        assertEquals(MeleeTable.Outcome.GLANCE, table(0.22).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 1, 3).outcome());
        assertEquals(MeleeTable.Outcome.HIT, MeleeTable.alwaysHit().rollOne(a, v, 4, 9).outcome());
        assertEquals(4, MeleeTable.alwaysHit().rollOne(a, v, 4, 9).damage());
        assertNotNull(MeleeTable.roll(a, v, 1, 3));
        assertNotNull(MeleeTable.roll(a, v, 2, 2));
    }

    private static MeleeTable table(double r) {
        return new MeleeTable(() -> r, (min, max) -> min >= max ? min : min);
    }
}
