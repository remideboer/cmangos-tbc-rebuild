package org.tbc.world.combat;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreatManagerTest {
    @Test
    void addWhenWhiteMeleeShouldKeepThreatPerAttackerAndReset() {
        ThreatManager tm = new ThreatManager();
        Player a = new Player();
        a.guid = 1;
        Player b = new Player();
        b.guid = 2;
        tm.add(a, 3);
        tm.add(b, 1);
        tm.add(a, 2);
        assertEquals(5f, tm.threatOf(a));
        assertEquals(1f, tm.threatOf(b));
        tm.reset();
        assertEquals(0f, tm.threatOf(a));
        assertEquals(0f, tm.threatOf(b));
    }

    @Test
    void highestGuidWhenTwoAttackersShouldReturnTheGreaterThreat() {
        ThreatManager tm = new ThreatManager();
        Player a = new Player();
        a.guid = 1;
        Player b = new Player();
        b.guid = 2;
        tm.add(a, 1);
        tm.add(b, 5);
        assertEquals(2L, tm.highestGuid());
        tm.add(a, 10);
        assertEquals(1L, tm.highestGuid());
        tm.reset();
        assertEquals(0L, tm.highestGuid());
    }
}
