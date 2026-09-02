package org.tbc;

import org.tbc.common.AuthCrypt;
import org.tbc.common.Codes;
import org.tbc.common.Sha1;
import org.tbc.common.Srp6;
import org.tbc.common.WorldHeader;
import org.tbc.common.WowBuffer;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.AddonInfo;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.BossScript;
import org.tbc.world.script.ClassScripts;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.session.LoginBurst;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

class CaptureSink implements PacketSink {
    final List<Integer> opcodes = new ArrayList<>();
    final List<byte[]> payloads = new ArrayList<>();
    boolean closed;

    @Override
    public void send(int opcode, byte[] payload) {
        opcodes.add(opcode);
        payloads.add(payload == null ? new byte[0] : payload);
    }

    @Override
    public void close() {
        closed = true;
    }
}

class TpInvTest {
    @Test
    void tpInv011AuthIsNotWorldHeaders() {
        WowBuffer b = new WowBuffer(4);
        b.putU8(Codes.CMD_AUTH_LOGON_CHALLENGE);
        b.putU8(0);
        b.putU16(0);
        assertEquals(0x00, b.array()[0] & 0xFF);
        assertNotEquals(6, b.size());
    }

    @Test
    void tpInv012Srp6RoundTrip() {
        Srp6.Verifier v = Srp6.makeVerifier("PLAYER", "PLAYER");
        Srp6.Session s = Srp6.serverChallenge(v.salt(), v.vLe());
        Srp6.Client c = Srp6.clientRespond("PLAYER", "PLAYER", v.salt(), s.bPubLe);
        assertTrue(Srp6.serverSessionKey(s, c.aPubLe));
        assertTrue(Srp6.proofM1(s, "PLAYER", c.m1));
        assertArrayEquals(c.m2, s.m2);
        assertEquals(40, s.k.length);
        byte[] bad = c.m1.clone();
        bad[0] ^= 1;
        Srp6.Session s2 = Srp6.serverChallenge(v.salt(), v.vLe());
        Srp6.serverSessionKey(s2, c.aPubLe);
        assertFalse(Srp6.proofM1(s2, "PLAYER", bad));
        assertFalse(Arrays.equals(s2.m1, bad));
        Srp6.Verifier mixed = Srp6.makeVerifier("Remi", "kitnipper");
        Srp6.Session sm = Srp6.serverChallenge(mixed.salt(), mixed.vLe());
        Srp6.Client cm = Srp6.clientRespond("REMI", "KITNIPPER", mixed.salt(), sm.bPubLe);
        assertTrue(Srp6.serverSessionKey(sm, cm.aPubLe));
        assertTrue(Srp6.proofM1(sm, "REMI", cm.m1));
    }

    @Test
    void tpInv003And004HeadersAndCrypt() {
        byte[] payload = {1, 2, 3};
        byte[] pkt = WorldHeader.serverPacket(Opcodes.SMSG_AUTH_CHALLENGE, payload);
        assertEquals(4 + 3, pkt.length);
        int size = ((pkt[0] & 0xFF) << 8) | (pkt[1] & 0xFF);
        assertEquals(2 + 3, size);
        int op = (pkt[2] & 0xFF) | ((pkt[3] & 0xFF) << 8);
        assertEquals(Opcodes.SMSG_AUTH_CHALLENGE, op);
        byte[] clientPkt = WorldHeader.clientPacket(Opcodes.CMSG_PING, new byte[]{1, 2, 3, 4});
        assertEquals(6 + 4, clientPkt.length);
        int csize = ((clientPkt[0] & 0xFF) << 8) | (clientPkt[1] & 0xFF);
        assertEquals(4 + 4, csize);
        int cop = (clientPkt[2] & 0xFF) | ((clientPkt[3] & 0xFF) << 8) | ((clientPkt[4] & 0xFF) << 16) | ((clientPkt[5] & 0xFF) << 24);
        assertEquals(Opcodes.CMSG_PING, cop);
        assertFalse(WorldHeader.validClientSize(3));
        assertFalse(WorldHeader.validClientSize(0x2801));
        assertTrue(WorldHeader.validClientSize(4));
        assertFalse(Opcodes.valid(0x424));
        assertEquals(0x424, Opcodes.NUM_MSG_TYPES);

        byte[] k = new byte[40];
        Arrays.fill(k, (byte) 0x11);
        AuthCrypt crypt = new AuthCrypt();
        crypt.init(k);
        byte[] plain = pkt.clone();
        crypt.encryptSend(pkt);
        assertFalse(Arrays.equals(Arrays.copyOf(plain, 4), Arrays.copyOf(pkt, 4)));
        assertArrayEquals(Arrays.copyOfRange(plain, 4, plain.length), Arrays.copyOfRange(pkt, 4, pkt.length));
        AuthCrypt crypt2 = new AuthCrypt();
        crypt2.init(k);
        byte[] c2s = new byte[6];
        Arrays.fill(c2s, (byte) 0x22);
        byte[] orig = c2s.clone();
        crypt2.decryptRecv(c2s);
        assertFalse(Arrays.equals(orig, c2s));
    }

    @Test
    void tpInv002CastResultNotFailed() {
        assertEquals(0x130, Opcodes.SMSG_CAST_RESULT);
        assertEquals(0x130, Codes.SMSG_CAST_RESULT);
        assertEquals("SMSG_CAST_RESULT", Opcodes.name(0x130));
        for (int i = 0; i < Opcodes.NUM_MSG_TYPES; i++) {
            assertFalse("SMSG_CAST_FAILED".equals(Opcodes.name(i)));
        }
        CaptureSink sink = new CaptureSink();
        new SpellEngine().sendFail(sink::send, 133, 0x5C, 1);
        assertEquals(Opcodes.SMSG_CAST_RESULT, sink.opcodes.get(0));
        new SpellEngine().sendFail(sink::send, 133, 0xFF, 1);
        assertEquals(1, sink.opcodes.size());
    }

    @Test
    void tpInv005LittleEndian() {
        WowBuffer b = new WowBuffer(4);
        b.putU32(0x130);
        assertEquals(0x30, b.array()[0] & 0xFF);
        assertEquals(0x01, b.array()[1] & 0xFF);
    }

    @Test
    void tpInv007FallTimeAlways() {
        WowBuffer b = new WowBuffer(32);
        b.putU32(0);
        b.putU8(0);
        b.putU32(1);
        b.putFloat(1);
        b.putFloat(2);
        b.putFloat(3);
        b.putFloat(0);
        b.putU32(42);
        MovementInfo m = MovementInfo.readC2s(b);
        assertEquals(42, m.fallTime);
    }

    @Test
    void tpInv008WardenCatalog() {
        assertEquals(0x2E7, Opcodes.CMSG_WARDEN_DATA);
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "P", new byte[40], 0, 1, "Win", "x86"));
        s.handle(w, Opcodes.CMSG_WARDEN_DATA, new byte[]{1});
        assertTrue(sink.opcodes.isEmpty());
    }

    @Test
    void tpInv009PlayerEnd() {
        assertEquals(1592, UpdateFields.PLAYER_END);
        assertEquals(0x0638, UpdateFields.PLAYER_END);
    }

    @Test
    void packedGuid() {
        WowBuffer b = new WowBuffer(16);
        b.putPackedGuid(0x00000000000000FFL);
        b.rpos(0);
        assertEquals(0xFFL, b.getPackedGuid());
    }
}

class SliceTests {
    private WorldSession loggedIn(World w, CaptureSink sink, String name) {
        return loggedIn(w, sink, name, 3);
    }

    private WorldSession loggedIn(World w, CaptureSink sink, String name, int gm) {
        WorldSession s = new WorldSession(sink, 0x11111111);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], gm, 1, "Win", "x86"));
        Player p = w.characters.create(1, name, 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        assertNotNull(p);
        WowBuffer g = new WowBuffer(8);
        g.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        return s;
    }

    private static byte[] payloadOf(CaptureSink sink, int opcode) {
        int i = sink.opcodes.indexOf(opcode);
        assertTrue(i >= 0);
        return sink.payloads.get(i);
    }

    private static void clear(CaptureSink sink) {
        sink.opcodes.clear();
        sink.payloads.clear();
    }

    private static int updateCount(CaptureSink sink) {
        int n = 0;
        for (int op : sink.opcodes) {
            if (op == Opcodes.SMSG_UPDATE_OBJECT || op == Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT) {
                n++;
            }
        }
        return n;
    }

    @Test
    void tpInv006LoginBurstOrder() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        loggedIn(w, sink, "Burst");
        assertEquals(Opcodes.MSG_SET_DUNGEON_DIFFICULTY, sink.opcodes.get(0));
        assertEquals(Opcodes.SMSG_LOGIN_VERIFY_WORLD, sink.opcodes.get(1));
        assertEquals(Opcodes.SMSG_ACCOUNT_DATA_TIMES, sink.opcodes.get(2));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.opcodes.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_CONTACT_LIST));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_TIME_SYNC_REQ));
        int i329 = sink.opcodes.indexOf(Opcodes.MSG_SET_DUNGEON_DIFFICULTY);
        int i236 = sink.opcodes.indexOf(Opcodes.SMSG_LOGIN_VERIFY_WORLD);
        int i209 = sink.opcodes.indexOf(Opcodes.SMSG_ACCOUNT_DATA_TIMES);
        assertTrue(i329 < i236 && i236 < i209);
        assertEquals(LoginBurst.ORDER[0], Opcodes.MSG_SET_DUNGEON_DIFFICULTY);
    }

    @Test
    void tpInv015UpdateFlagSelf() {
        World w = World.inMemory();
        Player p = w.characters.create(1, "Self", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        p.applyCreateFields();
        byte[] self = UpdateBuilder.createUnit(p, true, 0);
        WowBuffer b = new WowBuffer(self);
        assertEquals(1, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(UpdateBuilder.UPDATETYPE_CREATE_OBJECT2, b.getU8());
        b.getPackedGuid();
        assertEquals(4, b.getU8());
        int flags = b.getU8();
        assertEquals(0x71, flags);
        byte[] other = UpdateBuilder.createUnit(p, false, 0);
        WowBuffer o = new WowBuffer(other);
        o.getU32();
        o.getU8();
        o.getU8();
        o.getPackedGuid();
        o.getU8();
        int of = o.getU8();
        assertEquals(0, of & 0x01);
    }

    @Test
    void tpSl03CreateEnum() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        WowBuffer create = new WowBuffer(32);
        create.putCString("Warone");
        create.putU8(1);
        create.putU8(1);
        create.putU8(0);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(0);
        create.putU8(0);
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, create.array());
        assertEquals(Opcodes.SMSG_CHAR_CREATE, sink.opcodes.get(0));
        assertEquals(Codes.CHAR_CREATE_SUCCESS, sink.payloads.get(0)[0] & 0xFF);
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertEquals(Opcodes.SMSG_CHAR_ENUM, sink.opcodes.get(1));
        assertEquals(1, sink.payloads.get(1)[0] & 0xFF);
    }

    @Test
    void tpSl03StartOutfitOnCreateAndEnum() {
        World w = World.inMemory();
        w.objectMgr.createItems.put((int) ObjectMgr.key(1, 1), List.of(new ObjectMgr.CreateItem(25, 1)));
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        Player p = w.characters.create(1, "Geared", 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        assertNotNull(p);
        Item sword = p.itemAt(0, 15);
        assertNotNull(sword);
        assertEquals(25, sword.entry);
        assertEquals(1542, sword.displayId);
        assertEquals(21, sword.inventoryType);
        assertEquals(25, p.getInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0 + 15 * Player.MAX_VISIBLE_ITEM_OFFSET));
        assertEquals(1, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF);
        assertEquals(Player.POWER_RAGE, p.powerType);
        assertEquals(Player.POWER_RAGE_MAX, p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER2));
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        WowBuffer b = new WowBuffer(sink.payloads.get(0));
        assertEquals(1, b.getU8());
        b.getU64();
        assertEquals("Geared", b.getCString());
        for (int i = 0; i < 9; i++) {
            b.getU8();
        }
        b.getU32();
        b.getU32();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        b.getU32();
        b.getU32();
        b.getU8();
        b.getU32();
        b.getU32();
        b.getU32();
        for (int slot = 0; slot < 15; slot++) {
            b.getU32();
            b.getU8();
            b.getU32();
        }
        assertEquals(1542, b.getU32());
        assertEquals(21, b.getU8());
        assertEquals(0, b.getU32());
    }

    @Test
    void tpSl04ChatAndLogout() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Talker");
        WowBuffer initSpells = new WowBuffer(payloadOf(sink, Opcodes.SMSG_INITIAL_SPELLS));
        initSpells.getU8();
        int spellCount = initSpells.getU16();
        boolean sawCommon = false;
        for (int i = 0; i < spellCount; i++) {
            if (initSpells.getU16() == ChrStatic.SPELL_LANG_COMMON) {
                sawCommon = true;
            }
            initSpells.getU16();
        }
        assertTrue(sawCommon);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_LEARNED_SPELL));
        WowBuffer learned = new WowBuffer(payloadOf(sink, Opcodes.SMSG_LEARNED_SPELL));
        assertEquals(ChrStatic.SPELL_LANG_COMMON, learned.getU32());
        int learnedAt = sink.opcodes.indexOf(Opcodes.SMSG_LEARNED_SPELL);
        int createAt = Math.max(sink.opcodes.indexOf(Opcodes.SMSG_UPDATE_OBJECT),
                sink.opcodes.indexOf(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(createAt >= 0 && learnedAt > createAt);
        clear(sink);
        WowBuffer chat = new WowBuffer(32);
        chat.putU32(1);
        chat.putU32(7);
        chat.putCString("hello");
        s.handle(w, Opcodes.CMSG_MESSAGECHAT, chat.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_MESSAGECHAT));
        byte[] chatPayload = payloadOf(sink, Opcodes.SMSG_MESSAGECHAT);
        WowBuffer decoded = new WowBuffer(chatPayload);
        assertEquals(1, decoded.getU8());
        assertEquals(7, decoded.getU32());
        assertEquals(0, chatPayload[chatPayload.length - 1] & 0xFF);
        assertEquals(ChrStatic.SKILL_LANG_COMMON, s.player().getInt(UpdateFields.PLAYER_SKILL_INFO_1_1) & 0xFFFF);
        int skillVal = s.player().getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + 1);
        assertEquals(300, skillVal & 0xFFFF);
        assertEquals(300, (skillVal >>> 16) & 0xFFFF);
        assertTrue(s.player().spells.contains(ChrStatic.SPELL_LANG_COMMON));
        assertTrue(s.player().visibleToOwner(UpdateFields.PLAYER_SKILL_INFO_1_1, true));
        assertFalse(s.player().visibleToOwner(UpdateFields.PLAYER_SKILL_INFO_1_1, false));

        CaptureSink hearSink = new CaptureSink();
        loggedIn(w, hearSink, "Hearer");
        clear(sink);
        clear(hearSink);
        WowBuffer whisper = new WowBuffer(32);
        whisper.putU32(0x07);
        whisper.putU32(7);
        whisper.putCString("Hearer");
        whisper.putCString("psst");
        s.handle(w, Opcodes.CMSG_MESSAGECHAT, whisper.array());
        WowBuffer toHear = new WowBuffer(payloadOf(hearSink, Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x07, toHear.getU8());
        assertEquals(0, toHear.getU32());
        WowBuffer inform = new WowBuffer(payloadOf(sink, Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x09, inform.getU8());
        assertEquals(0, inform.getU32());

        hearSink.opcodes.clear();
        hearSink.payloads.clear();
        WowBuffer addon = new WowBuffer(32);
        addon.putU32(0x07);
        addon.putU32(0xFFFFFFFF);
        addon.putCString("Hearer");
        addon.putCString("addon");
        s.handle(w, Opcodes.CMSG_MESSAGECHAT, addon.array());
        WowBuffer addonChat = new WowBuffer(payloadOf(hearSink, Opcodes.SMSG_MESSAGECHAT));
        assertEquals(0x07, addonChat.getU8());
        assertEquals(0xFFFFFFFF, addonChat.getU32());

        Player gnome = w.characters.create(1, "Gnomey", 7, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        assertTrue(gnome.hasSkill(ChrStatic.SKILL_LANG_COMMON));
        assertTrue(gnome.hasSkill(ChrStatic.SKILL_LANG_GNOMISH));
        assertTrue(gnome.spells.contains(ChrStatic.SPELL_LANG_COMMON));
        assertTrue(gnome.spells.contains(ChrStatic.SPELL_LANG_GNOMISH));
        gnome.setSkill(-1, 98, 300, 300);
        gnome.setSkill(127, 98, 300, 300);

        assertEquals(UpdateFields.PRIVATE, UpdateFields.visibility(929));
        assertEquals(UpdateFields.PRIVATE, UpdateFields.visibility(1311));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_RESPONSE));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_COMPLETE));
    }

    @Test
    void tpSl04ContactListCountZero() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        loggedIn(w, sink, "Alone");
        WowBuffer b = new WowBuffer(payloadOf(sink, Opcodes.SMSG_CONTACT_LIST));
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
    }

    @Test
    void tpSl04LogoutCombatBlockAndPlayerLogoutNoop() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Stuck");
        s.player().inCombat = true;
        clear(sink);
        s.handle(w, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        WowBuffer r = new WowBuffer(payloadOf(sink, Opcodes.SMSG_LOGOUT_RESPONSE));
        assertEquals(1, r.getU32());
        assertEquals(0, r.getU8());
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
        clear(sink);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGOUT, new byte[0]);
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
        assertNotNull(s.player());
    }

    @Test
    void tpSl04DelayedLogoutResponseAndTimeSync() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Camper", 0);
        assertFalse(s.player().resting);
        clear(sink);
        s.handle(w, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        WowBuffer r = new WowBuffer(payloadOf(sink, Opcodes.SMSG_LOGOUT_RESPONSE));
        assertEquals(0, r.getU32());
        assertEquals(0, r.getU8());
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_LOGOUT_COMPLETE));
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
        clear(sink);
        WowBuffer sync = new WowBuffer(8);
        sync.putU32(0);
        sync.putU32(0);
        s.handle(w, Opcodes.CMSG_TIME_SYNC_RESP, sync.array());
        assertFalse(sink.closed);
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
    }

    @Test
    void tpSl04LogoutReenter() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Again");
        long guid = s.player().guid;
        s.handle(w, Opcodes.CMSG_LOGOUT_REQUEST, new byte[0]);
        assertEquals(WorldSession.STATUS_AUTHED, s.status());
        clear(sink);
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertEquals(Opcodes.SMSG_CHAR_ENUM, sink.opcodes.get(0));
        WowBuffer g = new WowBuffer(8);
        g.putU64(guid);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_LOGIN_VERIFY_WORLD));
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
    }

    @Test
    void tpSl06NearbyCreaturesOnLogin() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Looker");
        assertTrue(updateCount(sink) >= 2);
        assertFalse(w.map(s.player().mapId, s.player().instanceId)
                .nearbyCreatures(s.player(), GameMap.VISIBILITY).isEmpty());
    }

    @Test
    void tpSl07HeroicStrikeAndOutOfRange() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Warrior");
        Player p = s.player();
        Creature kobold = null;
        for (Creature c : w.map(p.mapId, p.instanceId).creatures.values()) {
            if (c.entry == 6) {
                kobold = c;
                break;
            }
        }
        assertNotNull(kobold);
        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        p.spells.add(78);
        p.setPower(0);
        int hp = kobold.health();
        WowBuffer cast = new WowBuffer(32);
        cast.putU32(78);
        cast.putU8(1);
        cast.putU32(SpellCastTargets.UNIT);
        cast.putPackedGuid(kobold.guid);
        clear(sink);
        s.handle(w, Opcodes.CMSG_CAST_SPELL, cast.array());
        WowBuffer noRage = new WowBuffer(payloadOf(sink, Opcodes.SMSG_CAST_RESULT));
        assertEquals(78, noRage.getU32());
        assertEquals(SpellEngine.SPELL_FAILED_NO_POWER, noRage.getU8());
        p.setPower(150);
        clear(sink);
        s.handle(w, Opcodes.CMSG_CAST_SPELL, cast.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_CAST_RESULT));
        assertTrue(kobold.health() < hp);
        assertEquals(0, p.power());
        p.relocate(kobold.x + 40f, kobold.y, kobold.z, kobold.o);
        clear(sink);
        s.handle(w, Opcodes.CMSG_CAST_SPELL, cast.array());
        WowBuffer r = new WowBuffer(payloadOf(sink, Opcodes.SMSG_CAST_RESULT));
        assertEquals(78, r.getU32());
        assertEquals(SpellEngine.SPELL_FAILED_OUT_OF_RANGE, r.getU8());
    }

    @Test
    void meleeRangeHealthAndItemCreate() {
        World w = World.inMemory();
        w.objectMgr.createItems.put((int) ObjectMgr.key(1, 1), List.of(new ObjectMgr.CreateItem(25, 1)));
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Swinger");
        Player p = s.player();
        assertNotNull(p.itemAt(0, 15));
        boolean itemCreate = false;
        for (int i = 0; i < sink.opcodes.size(); i++) {
            if (sink.opcodes.get(i) != Opcodes.SMSG_UPDATE_OBJECT) {
                continue;
            }
            byte[] raw = sink.payloads.get(i);
            if (raw.length < 12) {
                continue;
            }
            WowBuffer b = new WowBuffer(raw);
            if (b.getU32() != 1) {
                continue;
            }
            b.getU8();
            if (b.getU8() != UpdateBuilder.UPDATETYPE_CREATE_OBJECT) {
                continue;
            }
            long guid = b.getPackedGuid();
            if (b.getU8() == UpdateBuilder.TYPEID_ITEM && (guid & Guid.HIGH_ITEM) == Guid.HIGH_ITEM) {
                itemCreate = true;
                break;
            }
        }
        assertTrue(itemCreate);

        Creature kobold = null;
        for (Creature c : w.map(p.mapId, p.instanceId).creatures.values()) {
            if (c.entry == 6) {
                kobold = c;
                break;
            }
        }
        assertNotNull(kobold);
        p.relocate(kobold.x + 40f, kobold.y, kobold.z, kobold.o);
        int farHp = kobold.health();
        WowBuffer atk = new WowBuffer(8);
        atk.putU64(kobold.guid);
        clear(sink);
        s.handle(w, Opcodes.CMSG_ATTACKSWING, atk.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_ATTACKSTART));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_ATTACKSWING_NOTINRANGE));
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertEquals(farHp, kobold.health());

        p.relocate(kobold.x, kobold.y, kobold.z, kobold.o);
        clear(sink);
        s.handle(w, Opcodes.CMSG_ATTACKSWING, atk.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.opcodes.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        int n = 0;
        while (kobold.health() >= farHp && n++ < 40) {
            w.meleeHit(p, kobold);
        }
        assertTrue(kobold.health() < farHp);
        assertTrue(p.power() > 0);

        s.handle(w, Opcodes.CMSG_SETSHEATHED, new byte[0]);

        WowBuffer sheath = new WowBuffer(4);
        sheath.putU32(0);
        s.handle(w, Opcodes.CMSG_SETSHEATHED, sheath.array());
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF);
        sheath = new WowBuffer(4);
        sheath.putU32(1);
        s.handle(w, Opcodes.CMSG_SETSHEATHED, sheath.array());
        assertEquals(1, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF);
        sheath = new WowBuffer(4);
        sheath.putU32(99);
        s.handle(w, Opcodes.CMSG_SETSHEATHED, sheath.array());
        assertEquals(1, p.getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF);
    }

    @Test
    void tpSl08TeleRevealsGoldshireNpc() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Walker");
        Player p = s.player();
        String out = w.gm.handle(w, p, ".tele -9465 16 57");
        assertTrue(out.toLowerCase().contains("tele"));
        assertEquals(-9465f, p.x, 0.1f);
        clear(sink);
        s.handle(w, Opcodes.MSG_MOVE_WORLDPORT_ACK, new byte[0]);
        assertTrue(updateCount(sink) >= 1);
        boolean dughan = false;
        for (Creature c : w.map(0, 0).nearbyCreatures(p, GameMap.VISIBILITY)) {
            if (c.entry == Content.NPC_MARSHAL_DUGHAN) {
                dughan = true;
                break;
            }
        }
        assertTrue(dughan);
    }

    @Test
    void tpSl02OsReject() {
        World w = World.inMemory();
        byte[] k = new byte[40];
        w.testAccounts.put("LINUSER", new World.Account(2, "LINUSER", k, 0, 1, "Lin", "x86"));
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 5);
        WowBuffer in = authSession("LINUSER", 5, k, true);
        s.handle(w, Opcodes.CMSG_AUTH_SESSION, in.array());
        assertTrue(sink.closed);
        assertFalse(sink.opcodes.contains(Opcodes.SMSG_AUTH_RESPONSE));
    }

    @Test
    void tpSl02AddonThenAuthOk() {
        World w = World.inMemory();
        Srp6.Verifier v = Srp6.makeVerifier("PLAYER", "PLAYER");
        Srp6.Session sess = Srp6.serverChallenge(v.salt(), v.vLe());
        Srp6.Client c = Srp6.clientRespond("PLAYER", "PLAYER", v.salt(), sess.bPubLe);
        Srp6.serverSessionKey(sess, c.aPubLe);
        w.testAccounts.put("PLAYER", new World.Account(1, "PLAYER", sess.k, 0, 1, "Win", "x86"));
        CaptureSink sink = new CaptureSink();
        int seed = 99;
        WorldSession s = new WorldSession(sink, seed);
        WowBuffer in = authSession("PLAYER", seed, sess.k, true);
        s.handle(w, Opcodes.CMSG_AUTH_SESSION, in.array());
        assertTrue(sink.opcodes.size() >= 2);
        assertEquals(Opcodes.SMSG_ADDON_INFO, sink.opcodes.get(0));
        assertEquals(Opcodes.SMSG_AUTH_RESPONSE, sink.opcodes.get(1));
        assertEquals(Codes.AUTH_OK, sink.payloads.get(1)[0] & 0xFF);
        assertEquals(1, sink.payloads.get(1)[sink.payloads.get(1).length - 1] & 0xFF);
    }

    @Test
    void combatAndSpellAndGmAndGruul() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Fighter");
        Player p = s.player();
        Creature mob = w.map(0, 0).creatures.values().iterator().next();
        p.relocate(mob.x, mob.y, mob.z, mob.o);
        WowBuffer atk = new WowBuffer(8);
        atk.putU64(mob.guid);
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_ATTACKSWING, atk.array());
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_ATTACKSTART)
                || sink.opcodes.contains(Opcodes.SMSG_ATTACKERSTATEUPDATE));

        MeleeTable.Result r = MeleeTable.roll(p, mob, 1, 3);
        assertNotNull(r.outcome());

        p.spells.add(78);
        p.setPower(150);
        WowBuffer cast = new WowBuffer(8);
        cast.putU32(78);
        cast.putU8(1);
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CAST_SPELL, cast.array());
        assertFalse(sink.opcodes.contains(0x1B4));

        var exec = ClassScripts.warriorExecute(50);
        assertEquals(ClassScripts.SPELL_EXECUTE_DAMAGE, exec.damageSpell());
        assertEquals(0, exec.rageAfter());
        assertEquals(31117, ClassScripts.unstableAfflictionDispel());

        ScriptRegistry reg = w.scripts;
        assertTrue(reg.knows("boss_gruul"));
        BossScript gruul = reg.create("boss_gruul");
        gruul.aggro();
        List<Integer> casts = new ArrayList<>();
        gruul.update(mob, p, 30_000, (c, t, id) -> casts.add(id));
        assertTrue(casts.contains(36300));

        assertTrue(w.gm.allowed(p, "help"));
        p.gmLevel = 0;
        assertFalse(w.gm.allowed(p, "die"));
        p.gmLevel = 3;
        String out = w.gm.handle(w, p, ".die");
        assertTrue(out.toLowerCase().contains("die"));
        w.gm.overlay("die", 0);
        p.gmLevel = 0;
        assertTrue(w.gm.allowed(p, "die"));

        assertEquals(256, AddonInfo.PUBLIC_KEY.length);
        assertEquals(ChrStatic.team(1), 469);
        assertEquals(World.TICK_MS, 50);
    }

    private static WowBuffer authSession(String user, int serverSeed, byte[] k, boolean addon) {
        int clientSeed = 7;
        WowBuffer proof = new WowBuffer(user.length() + 4 + 4 + 4 + 40);
        proof.putBytes(user.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        proof.putU32(0);
        proof.putU32(clientSeed);
        proof.putU32(serverSeed);
        proof.putBytes(k);
        byte[] digest = Sha1.hash(proof.array());
        WowBuffer in = new WowBuffer(128);
        in.putU32(8606);
        in.putU32(0);
        in.putCString(user);
        in.putU32(clientSeed);
        in.putBytes(digest);
        byte[] raw = new byte[]{0}; // empty name
        WowBuffer add = new WowBuffer(16);
        add.putCString("Blizzard");
        add.putU32(AddonInfo.STANDARD_CRC);
        add.putU32(0);
        add.putU8(0);
        byte[] uncompressed = add.array();
        Deflater def = new Deflater();
        def.setInput(uncompressed);
        def.finish();
        byte[] z = new byte[128];
        int n = def.deflate(z);
        def.end();
        in.putU32(uncompressed.length);
        in.putBytes(Arrays.copyOf(z, n));
        return in;
    }
}

