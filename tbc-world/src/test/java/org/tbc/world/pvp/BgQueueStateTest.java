package org.tbc.world.pvp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-session battleground queue slot (refactoring plan cycle 2.4, formerly WorldSession.bgQueue). */
class BgQueueStateTest {
    @Test
    void newStateShouldNotBeQueued() {
        BgQueueState q = new BgQueueState();
        assertFalse(q.queued());
        assertEquals(0, q.queuedMap());
    }

    @Test
    void joinShouldRememberMapUntilLeave() {
        BgQueueState q = new BgQueueState();
        q.join(489);
        assertTrue(q.queued());
        assertEquals(489, q.queuedMap());
        q.leave();
        assertFalse(q.queued());
        assertEquals(0, q.queuedMap());
    }

    @Test
    void eotsWorldStatesShouldBeOffUntilMarked() {
        BgQueueState q = new BgQueueState();
        assertFalse(q.eotsWorldStatesSent());
        q.markEotsWorldStatesSent();
        assertTrue(q.eotsWorldStatesSent());
    }
}
