package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Mail;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.SocialHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL09-009 — mail take-money, delete, mark-as-read, return-to-sender, and copy-body letter.
 */
class Slice09MailTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    @Test
    void tpSl09MailTakeMoney() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Mailer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.money = 100;
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        client.clear();
        WowBuffer in = new WowBuffer(12);
        in.putU64(1);
        in.putU32(m.id);
        client.handle(world, Opcodes.CMSG_MAIL_TAKE_MONEY, in.array());
        assertTrue(client.saw(Opcodes.SMSG_SEND_MAIL_RESULT));
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(m.id, WowClientDouble.u32le(r, 0));
        assertEquals(SocialHandler.MAIL_MONEY_TAKEN, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_OK, WowClientDouble.u32le(r, 8));
        assertEquals(12, r.length);
        assertEquals(100, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(0, world.characters.mail(m.id).money);
    }

    @Test
    void tpSl09MailTakeMoneyWhenMissingShouldInternalError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Nomail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer in = new WowBuffer(12);
        in.putU64(1);
        in.putU32(99);
        client.handle(world, Opcodes.CMSG_MAIL_TAKE_MONEY, in.array());
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(99, WowClientDouble.u32le(r, 0));
        assertEquals(SocialHandler.MAIL_MONEY_TAKEN, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_ERR_INTERNAL, WowClientDouble.u32le(r, 8));
    }

    @Test
    void tpSl09MailDelete() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Delmail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.subject = "gone";
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        client.clear();
        WowBuffer in = new WowBuffer(16);
        in.putU64(1);
        in.putU32(m.id);
        in.putU32(0);
        client.handle(world, Opcodes.CMSG_MAIL_DELETE, in.array());
        assertTrue(client.saw(Opcodes.SMSG_SEND_MAIL_RESULT));
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(m.id, WowClientDouble.u32le(r, 0));
        assertEquals(SocialHandler.MAIL_DELETED, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_OK, WowClientDouble.u32le(r, 8));

        client.clear();
        client.getMailList(world, 1);
        assertEquals(0, client.payload(Opcodes.SMSG_MAIL_LIST_RESULT)[0] & 0xFF);
    }

    @Test
    void tpSl09MailDeleteWhenCodShouldInternalError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Codmail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.cod = 50;
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        client.clear();
        WowBuffer in = new WowBuffer(16);
        in.putU64(1);
        in.putU32(m.id);
        in.putU32(0);
        client.handle(world, Opcodes.CMSG_MAIL_DELETE, in.array());
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(SocialHandler.MAIL_DELETED, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_ERR_INTERNAL, WowClientDouble.u32le(r, 8));

        client.clear();
        client.getMailList(world, 1);
        assertEquals(1, client.payload(Opcodes.SMSG_MAIL_LIST_RESULT)[0] & 0xFF);
    }

    @Test
    void tpSl09MailMarkAsRead() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Readmail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.checked = 0x10;
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        client.clear();
        client.getMailList(world, 1);
        int before = firstMailChecked(client.payload(Opcodes.SMSG_MAIL_LIST_RESULT));
        assertEquals(0, before & 0x01);
        assertEquals(0x10, before & 0x10);

        client.clear();
        WowBuffer in = new WowBuffer(12);
        in.putU64(1);
        in.putU32(m.id);
        client.handle(world, Opcodes.CMSG_MAIL_MARK_AS_READ, in.array());
        assertFalse(client.saw(Opcodes.SMSG_SEND_MAIL_RESULT));
        assertFalse(client.saw(Opcodes.SMSG_MAIL_LIST_RESULT));

        client.getMailList(world, 1);
        int after = firstMailChecked(client.payload(Opcodes.SMSG_MAIL_LIST_RESULT));
        assertEquals(0x01, after & 0x01);
        assertEquals(0x10, after & 0x10);
    }

    @Test
    void tpSl09MailReturnToSender() {
        World world = World.inMemory();
        WowClientDouble sender = new WowClientDouble();
        WowClientDouble receiver = new WowClientDouble();
        sender.connect(ACC);
        receiver.connect(new World.Account(2, "OTHER", new byte[40], 0, 1, "Win", "x86"));
        Player from = world.characters.create(ACC.id(), "Frommail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player to = world.characters.create(2, "Tomail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        sender.login(world, from.guid);
        receiver.login(world, to.guid);
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.sender = Guid.low(from.guid);
        m.receiver = Guid.low(to.guid);
        m.subject = "hello";
        m.money = 50;
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        sender.clear();
        receiver.clear();
        WowBuffer in = new WowBuffer(20);
        in.putU64(1);
        in.putU32(m.id);
        in.putU64(from.guid);
        receiver.handle(world, Opcodes.CMSG_MAIL_RETURN_TO_SENDER, in.array());
        byte[] r = receiver.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(m.id, WowClientDouble.u32le(r, 0));
        assertEquals(SocialHandler.MAIL_RETURNED_TO_SENDER, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_OK, WowClientDouble.u32le(r, 8));
        assertTrue(sender.saw(Opcodes.SMSG_RECEIVED_MAIL));

        receiver.clear();
        receiver.getMailList(world, 1);
        assertEquals(0, receiver.payload(Opcodes.SMSG_MAIL_LIST_RESULT)[0] & 0xFF);

        sender.clear();
        sender.getMailList(world, 1);
        byte[] list = sender.payload(Opcodes.SMSG_MAIL_LIST_RESULT);
        assertEquals(1, list[0] & 0xFF);
        assertEquals(Guid.low(to.guid), firstMailSender(list));
        assertEquals(50, firstMailMoney(list));
        assertEquals(0x02, firstMailChecked(list) & 0x02);
    }

    @Test
    void tpSl09MailReturnToSenderWhenMissingShouldInternalError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Noreturn", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        client.clear();
        WowBuffer in = new WowBuffer(20);
        in.putU64(1);
        in.putU32(99);
        in.putU64(0);
        client.handle(world, Opcodes.CMSG_MAIL_RETURN_TO_SENDER, in.array());
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(99, WowClientDouble.u32le(r, 0));
        assertEquals(SocialHandler.MAIL_RETURNED_TO_SENDER, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_ERR_INTERNAL, WowClientDouble.u32le(r, 8));
    }

    @Test
    void tpSl09MailCreateTextItem() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Copymail", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.body = "keep this";
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);

        client.clear();
        WowBuffer in = new WowBuffer(16);
        in.putU64(1);
        in.putU32(m.id);
        in.putU32(0);
        client.handle(world, Opcodes.CMSG_MAIL_CREATE_TEXT_ITEM, in.array());
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(m.id, WowClientDouble.u32le(r, 0));
        assertEquals(5, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_OK, WowClientDouble.u32le(r, 8));
        assertTrue(client.saw(Opcodes.SMSG_ITEM_PUSH_RESULT));
        WowBuffer push = new WowBuffer(client.payload(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertEquals(p.guid, push.getU64());
        push.getU32();
        push.getU32();
        push.getU32();
        push.getU8();
        push.getU32();
        assertEquals(8383, push.getU32());

        client.clear();
        client.getMailList(world, 1);
        assertEquals(0x04, firstMailChecked(client.payload(Opcodes.SMSG_MAIL_LIST_RESULT)) & 0x04);
    }

    @Test
    void tpSl09MailCreateTextItemWhenEmptyBodyShouldInternalError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Nobodytxt", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        Mail m = new Mail();
        m.id = world.characters.nextMailId();
        m.receiver = Guid.low(p.guid);
        m.deliverTime = world.nowMs() / 1000;
        world.characters.storeMail(m);
        client.clear();
        WowBuffer in = new WowBuffer(16);
        in.putU64(1);
        in.putU32(m.id);
        in.putU32(0);
        client.handle(world, Opcodes.CMSG_MAIL_CREATE_TEXT_ITEM, in.array());
        byte[] r = client.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(5, WowClientDouble.u32le(r, 4));
        assertEquals(SocialHandler.MAIL_ERR_INTERNAL, WowClientDouble.u32le(r, 8));
    }

    /** mail.md SMSG_MAIL_LIST_RESULT: count, row size, then id/type/sender/COD/itemText/package/stationery/money/checked. */
    private static int firstMailChecked(byte[] list) {
        WowBuffer b = firstMailAfterMoney(list);
        return b.getU32();
    }

    private static int firstMailMoney(byte[] list) {
        WowBuffer b = new WowBuffer(list);
        skipToMailMoney(b);
        return b.getU32();
    }

    private static int firstMailSender(byte[] list) {
        WowBuffer b = new WowBuffer(list);
        b.getU8();
        b.getU16();
        b.getU32();
        b.getU8();
        return Guid.low(b.getU64());
    }

    private static WowBuffer firstMailAfterMoney(byte[] list) {
        WowBuffer b = new WowBuffer(list);
        skipToMailMoney(b);
        b.getU32();
        return b;
    }

    private static void skipToMailMoney(WowBuffer b) {
        b.getU8();
        b.getU16();
        b.getU32();
        b.getU8();
        b.getU64();
        b.getU32();
        b.getU32();
        b.getU32();
        b.getU32();
    }
}
