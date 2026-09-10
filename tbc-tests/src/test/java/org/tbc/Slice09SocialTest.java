package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL09-010 — ignore list and trade gold, one opcode per method. */
class Slice09SocialTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpSl09AddIgnore() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC.id(), "Ignorer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(2, "Ignored", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);

        alpha.clear();
        WowBuffer in = new WowBuffer(16);
        in.putCString("Ignored");
        alpha.handle(world, Opcodes.CMSG_ADD_IGNORE, in.array());
        assertTrue(alpha.saw(Opcodes.SMSG_FRIEND_STATUS));
        byte[] st = alpha.payload(Opcodes.SMSG_FRIEND_STATUS);
        assertEquals(0x0F, st[0] & 0xFF);
        assertEquals(b.guid, WowClientDouble.u64le(st, 1));
        assertEquals(9, st.length);

        alpha.clear();
        WowBuffer list = new WowBuffer(4);
        list.putU32(0);
        alpha.handle(world, Opcodes.CMSG_CONTACT_LIST, list.array());
        WowBuffer out = new WowBuffer(alpha.payload(Opcodes.SMSG_CONTACT_LIST));
        assertEquals(0x02, out.getU32());
        assertEquals(1, out.getU32());
        assertEquals(b.guid, out.getU64());
        assertEquals(0x02, out.getU32());
        assertEquals("", out.getCString());
        assertEquals(0, out.remaining());
    }

    @Test
    void tpSl09AddIgnoreWhenSelfShouldIgnoreSelf() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Selfign", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer in = new WowBuffer(16);
        in.putCString("Selfign");
        client.handle(world, Opcodes.CMSG_ADD_IGNORE, in.array());
        byte[] st = client.payload(Opcodes.SMSG_FRIEND_STATUS);
        assertEquals(0x0C, st[0] & 0xFF);
        assertEquals(created.guid, WowClientDouble.u64le(st, 1));
    }

    @Test
    void tpSl09DelIgnore() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC.id(), "Ignorer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(2, "Ignored", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
        WowBuffer add = new WowBuffer(16);
        add.putCString("Ignored");
        alpha.handle(world, Opcodes.CMSG_ADD_IGNORE, add.array());

        alpha.clear();
        WowBuffer in = new WowBuffer(8);
        in.putU64(b.guid);
        alpha.handle(world, Opcodes.CMSG_DEL_IGNORE, in.array());
        assertTrue(alpha.saw(Opcodes.SMSG_FRIEND_STATUS));
        byte[] st = alpha.payload(Opcodes.SMSG_FRIEND_STATUS);
        assertEquals(0x10, st[0] & 0xFF);
        assertEquals(b.guid, WowClientDouble.u64le(st, 1));
        assertEquals(9, st.length);

        alpha.clear();
        WowBuffer list = new WowBuffer(4);
        list.putU32(0);
        alpha.handle(world, Opcodes.CMSG_CONTACT_LIST, list.array());
        WowBuffer out = new WowBuffer(alpha.payload(Opcodes.SMSG_CONTACT_LIST));
        assertEquals(0, out.getU32());
        assertEquals(0, out.getU32());
        assertEquals(0, out.remaining());
    }

    @Test
    void tpSl09DelIgnoreWhenNotListedShouldStillReportRemoved() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Soloign", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        long missing = 0x00000000000000ABL;
        client.clear();
        WowBuffer in = new WowBuffer(8);
        in.putU64(missing);
        client.handle(world, Opcodes.CMSG_DEL_IGNORE, in.array());
        byte[] st = client.payload(Opcodes.SMSG_FRIEND_STATUS);
        assertEquals(0x10, st[0] & 0xFF);
        assertEquals(missing, WowClientDouble.u64le(st, 1));
        assertEquals(9, st.length);
    }

    @Test
    void tpSl09SetContactNotes() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC.id(), "Noter", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(2, "Noted", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
        WowBuffer add = new WowBuffer(16);
        add.putCString("Noted");
        add.putCString("");
        alpha.handle(world, Opcodes.CMSG_ADD_FRIEND, add.array());

        alpha.clear();
        WowBuffer in = new WowBuffer(32);
        in.putU64(b.guid);
        in.putCString("hello");
        alpha.handle(world, Opcodes.CMSG_SET_CONTACT_NOTES, in.array());
        assertEquals(0, alpha.payload(Opcodes.SMSG_FRIEND_STATUS).length);

        WowBuffer list = new WowBuffer(4);
        list.putU32(0);
        alpha.handle(world, Opcodes.CMSG_CONTACT_LIST, list.array());
        WowBuffer out = new WowBuffer(alpha.payload(Opcodes.SMSG_CONTACT_LIST));
        assertEquals(0x01, out.getU32());
        assertEquals(1, out.getU32());
        assertEquals(b.guid, out.getU64());
        assertEquals(0x01, out.getU32());
        assertEquals("hello", out.getCString());
    }

    @Test
    void tpSl09SetTradeGold() {
        World world = World.inMemory();
        WowClientDouble alpha = new WowClientDouble();
        WowClientDouble bravo = new WowClientDouble();
        alpha.connect(ACC);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC.id(), "TraderA", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(2, "TraderB", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
        Player ap = alpha.session().player();
        Player bp = bravo.session().player();
        bp.relocate(ap.x, ap.y, ap.z, ap.o);
        ap.setMoney(200);
        bp.setMoney(0);

        WowBuffer init = new WowBuffer(8);
        init.putU64(bp.guid);
        alpha.handle(world, Opcodes.CMSG_INITIATE_TRADE, init.array());
        bravo.handle(world, Opcodes.CMSG_BEGIN_TRADE, new byte[0]);

        alpha.clear();
        bravo.clear();
        WowBuffer gold = new WowBuffer(4);
        gold.putU32(100);
        alpha.handle(world, Opcodes.CMSG_SET_TRADE_GOLD, gold.array());
        assertTrue(bravo.saw(Opcodes.SMSG_TRADE_STATUS_EXTENDED));
        byte[] ext = bravo.payload(Opcodes.SMSG_TRADE_STATUS_EXTENDED);
        assertEquals(1, ext[0] & 0xFF);
        assertEquals(0, WowClientDouble.u32le(ext, 1));
        assertEquals(7, WowClientDouble.u32le(ext, 5));
        assertEquals(7, WowClientDouble.u32le(ext, 9));
        assertEquals(100, WowClientDouble.u32le(ext, 13));
        assertEquals(0, WowClientDouble.u32le(ext, 17));

        alpha.clear();
        bravo.clear();
        WowBuffer accept = new WowBuffer(4);
        accept.putU32(0);
        alpha.handle(world, Opcodes.CMSG_ACCEPT_TRADE, accept.array());
        bravo.handle(world, Opcodes.CMSG_ACCEPT_TRADE, accept.array());
        assertEquals(100, alpha.valuesField(ap.guid, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(100, bravo.valuesField(bp.guid, UpdateFields.PLAYER_FIELD_COINAGE));
    }
}
