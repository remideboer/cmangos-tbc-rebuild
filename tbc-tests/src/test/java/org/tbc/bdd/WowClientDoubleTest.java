package org.tbc.bdd;

import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WowClientDoubleTest {
    @Test
    void recordsNullPayloadAndMisses() {
        WowClientDouble d = new WowClientDouble();
        d.send(Opcodes.SMSG_PONG, null);
        assertTrue(d.saw(Opcodes.SMSG_PONG));
        assertFalse(d.saw(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(0, d.payload(Opcodes.SMSG_LOGOUT_COMPLETE).length);
        assertArrayEquals(new byte[0], d.payload(Opcodes.SMSG_PONG));
        d.clear();
        assertFalse(d.saw(Opcodes.SMSG_PONG));
    }

    @Test
    void decodesLittleEndian() {
        byte[] p = new byte[]{0x30, 0x01, 0x00, 0x00};
        assertEquals(0x130, WowClientDouble.u32le(p, 0));
        byte[] f = new byte[4];
        int bits = Float.floatToIntBits(1.5f);
        f[0] = (byte) bits;
        f[1] = (byte) (bits >>> 8);
        f[2] = (byte) (bits >>> 16);
        f[3] = (byte) (bits >>> 24);
        assertEquals(1.5f, WowClientDouble.floatle(f, 0));
        byte[] z = new byte[]{0};
        assertEquals(1, WowClientDouble.skipPackedGuid(z, 0));
        byte[] one = new byte[]{0x01, 0x7F};
        assertEquals(2, WowClientDouble.skipPackedGuid(one, 0));
        byte[] u = new byte[]{0x78, 0x56, 0x34, 0x12, 0, 0, 0, 0};
        assertEquals(0x12345678L, WowClientDouble.u64le(u, 0));
    }

    @Test
    void connectPingRoundTrip() {
        World w = World.inMemory();
        WowClientDouble d = new WowClientDouble();
        d.connect(new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86"));
        d.ping(w, 7);
        assertTrue(d.saw(Opcodes.SMSG_PONG));
        assertEquals(7, WowClientDouble.u32le(d.payload(Opcodes.SMSG_PONG), 0));
        org.tbc.world.entity.Player created = w.characters.create(1, "Cast", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        d.login(w, created.guid);
        d.clear();
        d.castSpell(w, 78, 1, 0);
        d.castSpell(w, 78, 1, created.guid);
        d.sendMail(w, 1, "Nobody", "s", "b", 0);
        d.sendMail(w, 1, "Nobody", "s", "b", 1);
        d.who(w);
        d.addFriend(w, "Nobody");
        d.delFriend(w, created.guid);
        d.guildCreate(w, "Lions");
        d.guildInvite(w, "Nobody");
        d.guildAccept(w);
        d.auctionSell(w, 0, 0, 100, 0, 720);
        d.auctionBid(w, 0, 1, 100);
        d.groupInvite(w, "Nobody");
        d.groupAccept(w);
        d.partyChat(w, "hi");
        d.say(w, "hi");
        d.whisper(w, "Nobody", "x");
        d.nameQuery(w, created.guid);
        d.heartbeat(w, 0, 0, 0, 0);
        d.areaTrigger(w, 2230);
        d.worldportAck(w);
        d.resetInstances(w);
        d.battlemasterJoin(w);
        d.battlefieldPort(w, 1);
        d.initiateTrade(w, 0);
        d.beginTrade(w);
        d.setTradeItem(w, 0, 0, 23);
        d.acceptTrade(w);
        d.getMailList(w, 1);
        d.takeMailItem(w, 1, 1, 1);
        d.learnTalent(w, 124, 0);
    }
}
