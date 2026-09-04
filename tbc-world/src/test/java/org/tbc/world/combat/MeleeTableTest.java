package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MeleeTableTest {
    @Test
    void tpSl12MeleeTableOneRoll() {
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

    @Test
    void rollOneWhenPlayerHasFivePercentCritShouldCritAndDoubleDamage() {
        Player a = new Player();
        a.level = 1;
        a.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_CRIT_PERCENTAGE, 5f);
        Creature v = new Creature();
        v.level = 1;
        v.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        MeleeTable.Result r = table(0.22).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRIT, r.outcome());
        assertEquals(4, r.damage());
        assertEquals(4, r.threat());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        a.setFloat(org.tbc.world.net.wow8606.UpdateFields.PLAYER_CRIT_PERCENTAGE, 200f);
        assertEquals(MeleeTable.Outcome.CRIT, table(0.22).rollOne(a, v, 2, 2).outcome());
    }

    @Test
    void rollOneWhenCreatureShouldUseFivePercentBaseCrit() {
        Creature a = new Creature();
        a.level = 1;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player v = new Player();
        v.level = 1;
        v.setHealth(100);
        MeleeTable.Result r = table(0.22).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRIT, r.outcome());
        assertEquals(4, r.damage());
    }

    @Test
    void rollOneWhenNpcSkillDeficitAtLeast15ShouldCrushAt150Percent() {
        Creature a = new Creature();
        a.level = 4;
        a.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 4);
        Player v = new Player();
        v.level = 1;
        MeleeTable.Result r = table(0.30).rollOne(a, v, 2, 2);
        assertEquals(MeleeTable.Outcome.CRUSH, r.outcome());
        assertEquals(3, r.damage());
        assertEquals(3, r.threat());
        assertEquals(MeleeTable.Outcome.HIT, table(0.50).rollOne(a, v, 2, 2).outcome());
        a.level = 1;
        assertEquals(MeleeTable.Outcome.HIT, table(0.30).rollOne(a, v, 2, 2).outcome());
        Creature other = new Creature();
        other.level = 4;
        a.level = 4;
        assertEquals(MeleeTable.Outcome.HIT, table(0.30).rollOne(a, other, 2, 2).outcome());
    }

    private static MeleeTable table(double r) {
        return new MeleeTable(() -> r, (min, max) -> min >= max ? min : min);
    }
}
