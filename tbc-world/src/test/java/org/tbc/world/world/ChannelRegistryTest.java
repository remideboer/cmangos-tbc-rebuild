package org.tbc.world.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Channel.cpp membership/owner/ban/mode rules, extracted from World (refactoring plan cycle 2.1). */
class ChannelRegistryTest {
    private static final long ALICE = 10;
    private static final long BOB = 11;

    @Test
    void joinWhenFirstOnCustomChannelShouldBecomeOwnerAndModerator() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("Trade", ALICE);
        r.join("Trade", BOB);
        assertEquals(ALICE, r.owner("Trade"));
        assertEquals(ChannelRegistry.MEMBER_FLAG_OWNER | ChannelRegistry.MEMBER_FLAG_MODERATOR, r.flags("Trade", ALICE));
        assertEquals(ChannelRegistry.MEMBER_FLAG_NONE, r.flags("Trade", BOB));
        assertTrue(r.isMember("Trade", ALICE));
        assertTrue(r.isMember("Trade", BOB));
    }

    @Test
    void joinWhenGeneralShouldNotClaimOwner() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("General", ALICE);
        assertNull(r.owner("General"));
        assertTrue(r.isMember("General", ALICE));
    }

    @Test
    void leaveWhenMemberShouldKeepFlagsButDropMembership() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("Trade", ALICE);
        r.leave("Trade", ALICE);
        assertFalse(r.isMember("Trade", ALICE));
        assertEquals(ALICE, r.owner("Trade"));
    }

    @Test
    void removeMemberWhenKickedShouldDropMembershipAndFlags() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("Trade", ALICE);
        r.join("Trade", BOB);
        r.setFlags("Trade", BOB, ChannelRegistry.MEMBER_FLAG_MODERATOR);
        r.removeMember("Trade", BOB);
        assertFalse(r.isMember("Trade", BOB));
        assertEquals(ChannelRegistry.MEMBER_FLAG_NONE, r.flags("Trade", BOB));
    }

    @Test
    void leaveAllWhenLoggingOutShouldDropEveryMembership() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("General", ALICE);
        r.join("Trade", ALICE);
        r.leaveAll(ALICE);
        assertFalse(r.isMember("General", ALICE));
        assertFalse(r.isMember("Trade", ALICE));
    }

    @Test
    void banWhenNewShouldReturnTrueThenFalseOnRepeat() {
        ChannelRegistry r = new ChannelRegistry();
        assertTrue(r.ban("Trade", BOB));
        assertFalse(r.ban("Trade", BOB));
        assertTrue(r.isBanned("Trade", BOB));
        assertTrue(r.unban("Trade", BOB));
        assertFalse(r.unban("Trade", BOB));
        assertFalse(r.isBanned("Trade", BOB));
    }

    @Test
    void passwordWhenUnsetShouldBeEmpty() {
        ChannelRegistry r = new ChannelRegistry();
        assertEquals("", r.password("Trade"));
        r.setPassword("Trade", "pw");
        assertEquals("pw", r.password("Trade"));
        r.setPassword("Trade", null);
        assertEquals("", r.password("Trade"));
    }

    @Test
    void toggleAnnouncementsWhenCustomShouldDefaultOnAndGeneralOff() {
        ChannelRegistry r = new ChannelRegistry();
        assertFalse(r.toggleAnnouncements("Trade"));
        assertTrue(r.toggleAnnouncements("Trade"));
        assertTrue(r.toggleAnnouncements("General"));
    }

    @Test
    void toggleModerationShouldDefaultOff() {
        ChannelRegistry r = new ChannelRegistry();
        assertTrue(r.toggleModeration("Trade"));
        assertFalse(r.toggleModeration("Trade"));
    }

    @Test
    void setOwnerShouldReplaceOwner() {
        ChannelRegistry r = new ChannelRegistry();
        r.join("Trade", ALICE);
        r.setOwner("Trade", BOB);
        assertEquals(BOB, r.owner("Trade"));
    }
}
