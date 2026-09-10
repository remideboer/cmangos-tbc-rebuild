package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL09-011 — party decline / kick / promote, one opcode per method. */
class Slice09GroupTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 0, 1, "Win", "x86");

    /**
     * CMSG_GROUP_DECLINE — HandleGroupDeclineOpcode: UninviteFromGroup then SMSG_GROUP_DECLINE
     * (decliner name C-string) to the inviter if online. Empty C2S. No list for a declined invite.
     */
    @Test
    void tpSl09GroupDecline() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(2, "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);

        alpha.groupInvite(world, "Bravo");
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_INVITE));
        alpha.clear();
        bravo.clear();
        bravo.handle(world, Opcodes.CMSG_GROUP_DECLINE, new byte[0]);
        assertTrue(alpha.saw(Opcodes.SMSG_GROUP_DECLINE));
        WowBuffer dec = new WowBuffer(alpha.payload(Opcodes.SMSG_GROUP_DECLINE));
        assertEquals("Bravo", dec.getCString());
        assertEquals(0, dec.remaining());
        assertFalse(bravo.saw(Opcodes.SMSG_GROUP_LIST));
        assertNull(a.group);
        assertNull(b.group);
        assertNull(bravo.session().pendingInviteFrom);

        bravo.handle(world, Opcodes.CMSG_GROUP_ACCEPT, new byte[0]);
        assertFalse(bravo.saw(Opcodes.SMSG_GROUP_LIST), "accept after decline must not join");
    }

    @Test
    void tpSl09GroupDeclineWhenNoInviteShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Solo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        client.handle(world, Opcodes.CMSG_GROUP_DECLINE, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_GROUP_DECLINE));
        assertNull(created.group);
    }
}
