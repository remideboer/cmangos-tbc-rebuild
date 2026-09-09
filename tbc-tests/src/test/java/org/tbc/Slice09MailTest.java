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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL09-009 — CMSG_MAIL_TAKE_MONEY (HandleMailTakeMoney).
 * Success is MAIL_MONEY_TAKEN + MAIL_OK then PLAYER_FIELD_COINAGE.
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
}
