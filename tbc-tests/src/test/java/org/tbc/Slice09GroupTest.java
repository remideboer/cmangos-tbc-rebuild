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

    /**
     * CMSG_GROUP_UNINVITE — HandleGroupUninviteOpcode name C-string. Leader/assistant only.
     * Kick: empty SMSG_GROUP_UNINVITE to the removed player, then empty SMSG_GROUP_LIST (group.md);
     * remaining members get SMSG_GROUP_LIST without the kicked player. Party of 3 so the group survives.
     */
    @Test
    void tpSl09GroupUninviteByName() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        WowClientDouble charlie = new WowClientDouble();
        World.Account accC = new World.Account(3, "THIRD", new byte[40], 0, 1, "Win", "x86");
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        charlie.connect(accC);
        Player createdA = world.characters.create(ACC.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player createdB = world.characters.create(2, "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player createdC = world.characters.create(3, "Charlie", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, createdA.guid);
        bravo.login(world, createdB.guid);
        charlie.login(world, createdC.guid);
        Player a = alpha.session().player();
        Player b = bravo.session().player();
        Player c = charlie.session().player();
        alpha.groupInvite(world, "Bravo");
        bravo.groupAccept(world);
        alpha.groupInvite(world, "Charlie");
        charlie.groupAccept(world);
        assertEquals(3, a.group.members.size());

        alpha.clear();
        bravo.clear();
        charlie.clear();
        WowBuffer kick = new WowBuffer(16);
        kick.putCString("Bravo");
        alpha.handle(world, Opcodes.CMSG_GROUP_UNINVITE, kick.array());
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_UNINVITE));
        assertEquals(0, bravo.payload(Opcodes.SMSG_GROUP_UNINVITE).length);
        WowBuffer empty = new WowBuffer(bravo.payload(Opcodes.SMSG_GROUP_LIST));
        assertEquals(0L, empty.getU64());
        assertEquals(0L, empty.getU64());
        assertEquals(0L, empty.getU64());
        assertEquals(0, empty.remaining());
        assertNull(b.group);
        assertTrue(alpha.saw(Opcodes.SMSG_GROUP_LIST));
        assertTrue(charlie.saw(Opcodes.SMSG_GROUP_LIST));
        WowBuffer remaining = new WowBuffer(alpha.payload(Opcodes.SMSG_GROUP_LIST));
        remaining.getU8();
        remaining.getU8();
        remaining.getU8();
        remaining.getU8();
        remaining.getU64();
        assertEquals(1, remaining.getU32());
        assertEquals("Charlie", remaining.getCString());
        assertEquals(2, a.group.members.size());
        assertTrue(a.group.members.contains(a));
        assertTrue(a.group.members.contains(c));
        assertFalse(a.group.members.contains(b));
    }

    /**
     * CMSG_GROUP_UNINVITE_GUID — HandleGroupUninviteGuidOpcode raw guid. Same kick S2C as name uninvite.
     */
    @Test
    void tpSl09GroupUninviteByGuid() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        WowClientDouble charlie = new WowClientDouble();
        World.Account accC = new World.Account(3, "THIRD", new byte[40], 0, 1, "Win", "x86");
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        charlie.connect(accC);
        Player createdA = world.characters.create(ACC.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player createdB = world.characters.create(2, "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player createdC = world.characters.create(3, "Charlie", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, createdA.guid);
        bravo.login(world, createdB.guid);
        charlie.login(world, createdC.guid);
        Player a = alpha.session().player();
        Player b = bravo.session().player();
        Player c = charlie.session().player();
        alpha.groupInvite(world, "Bravo");
        bravo.groupAccept(world);
        alpha.groupInvite(world, "Charlie");
        charlie.groupAccept(world);

        alpha.clear();
        bravo.clear();
        charlie.clear();
        WowBuffer kick = new WowBuffer(8);
        kick.putU64(b.guid);
        alpha.handle(world, Opcodes.CMSG_GROUP_UNINVITE_GUID, kick.array());
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_UNINVITE));
        assertEquals(0, bravo.payload(Opcodes.SMSG_GROUP_UNINVITE).length);
        WowBuffer empty = new WowBuffer(bravo.payload(Opcodes.SMSG_GROUP_LIST));
        assertEquals(0L, empty.getU64());
        assertEquals(0L, empty.getU64());
        assertEquals(0L, empty.getU64());
        assertEquals(0, empty.remaining());
        assertNull(b.group);
        assertTrue(alpha.saw(Opcodes.SMSG_GROUP_LIST));
        assertTrue(charlie.saw(Opcodes.SMSG_GROUP_LIST));
        WowBuffer remaining = new WowBuffer(alpha.payload(Opcodes.SMSG_GROUP_LIST));
        remaining.getU8();
        remaining.getU8();
        remaining.getU8();
        remaining.getU8();
        remaining.getU64();
        assertEquals(1, remaining.getU32());
        assertEquals("Charlie", remaining.getCString());
        assertEquals(2, a.group.members.size());
        assertTrue(a.group.members.contains(c));
        assertFalse(a.group.members.contains(b));
    }

    /**
     * CMSG_GROUP_SET_LEADER — HandleGroupSetLeaderOpcode raw guid. Leader only; target online and in the group.
     * ChangeLeader broadcasts SMSG_GROUP_SET_LEADER name C-string then SMSG_GROUP_LIST (group.md).
     */
    @Test
    void tpSl09GroupSetLeader() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player createdA = world.characters.create(ACC.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player createdB = world.characters.create(2, "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, createdA.guid);
        bravo.login(world, createdB.guid);
        Player a = alpha.session().player();
        Player b = bravo.session().player();
        alpha.groupInvite(world, "Bravo");
        bravo.groupAccept(world);

        alpha.clear();
        bravo.clear();
        WowBuffer promote = new WowBuffer(8);
        promote.putU64(b.guid);
        alpha.handle(world, Opcodes.CMSG_GROUP_SET_LEADER, promote.array());
        assertTrue(alpha.saw(Opcodes.SMSG_GROUP_SET_LEADER));
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_SET_LEADER));
        WowBuffer name = new WowBuffer(alpha.payload(Opcodes.SMSG_GROUP_SET_LEADER));
        assertEquals("Bravo", name.getCString());
        assertEquals(0, name.remaining());
        assertEquals("Bravo", new WowBuffer(bravo.payload(Opcodes.SMSG_GROUP_SET_LEADER)).getCString());
        assertEquals(b.guid, a.group.leaderGuid);
        WowBuffer list = new WowBuffer(alpha.payload(Opcodes.SMSG_GROUP_LIST));
        list.getU8();
        list.getU8();
        list.getU8();
        list.getU8();
        list.getU64();
        assertEquals(1, list.getU32());
        assertEquals("Bravo", list.getCString());
        assertEquals(b.guid, list.getU64());
        list.getU8();
        list.getU8();
        list.getU8();
        assertEquals(b.guid, list.getU64());
    }
}
