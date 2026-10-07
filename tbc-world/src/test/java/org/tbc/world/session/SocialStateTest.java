package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Per-session social scratch state (refactoring plan cycle 2.4): pending group invite and open GM ticket. */
class SocialStateTest {
    @Test
    void newStateShouldHaveNoInviteAndNoTicket() {
        SocialState s = new SocialState();
        assertNull(s.pendingInvite());
        assertFalse(s.hasTicket());
        assertEquals("", s.ticket());
    }

    @Test
    void takePendingInviteShouldReturnInviterOnceThenNull() {
        SocialState s = new SocialState();
        Player from = new Player();
        s.invite(from);
        assertSame(from, s.pendingInvite());
        assertSame(from, s.takePendingInvite());
        assertNull(s.takePendingInvite());
    }

    @Test
    void openTicketShouldBeReadableUntilCleared() {
        SocialState s = new SocialState();
        s.openTicket("stuck in wall");
        assertTrue(s.hasTicket());
        assertEquals("stuck in wall", s.ticket());
        s.clearTicket();
        assertFalse(s.hasTicket());
        assertEquals("", s.ticket());
    }

    @Test
    void openTicketWhenTextNullShouldCountAsNoTicket() {
        SocialState s = new SocialState();
        s.openTicket(null);
        assertFalse(s.hasTicket());
    }
}
