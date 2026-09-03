package org.tbc;

import org.tbc.common.Codes;
import org.tbc.common.Sha1;
import org.tbc.common.Srp6;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.AddonInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarlySliceP0Test {
    @Test
    void tpInv001And014ChallengeAndWrongWorldBuild() {
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 0x22222222);
        s.sendChallenge();
        assertEquals(Opcodes.SMSG_AUTH_CHALLENGE, sink.opcodes.get(0));
        assertEquals(0x1EC, Opcodes.SMSG_AUTH_CHALLENGE);
        WowBuffer seed = new WowBuffer(sink.payloads.get(0));
        assertEquals(0x22222222, seed.getU32());
        World w = World.inMemory();
        sink.opcodes.clear();
        sink.payloads.clear();
        s.handle(w, Opcodes.CMSG_AUTH_SESSION, authSession("PLAYER", 0x22222222, new byte[40], 12340).array());
        assertEquals(Opcodes.SMSG_AUTH_RESPONSE, sink.opcodes.get(0));
        assertEquals(Codes.AUTH_VERSION_MISMATCH, sink.payloads.get(0)[0] & 0xFF);
        assertTrue(sink.closed);
    }

    @Test
    void tpSl02ExpansionOneOnAuthOk() {
        World w = World.inMemory();
        Srp6.Verifier v = Srp6.makeVerifier("PLAYER", "PLAYER");
        Srp6.Session sess = Srp6.serverChallenge(v.salt(), v.vLe());
        Srp6.Client c = Srp6.clientRespond("PLAYER", "PLAYER", v.salt(), sess.bPubLe);
        Srp6.serverSessionKey(sess, c.aPubLe);
        w.testAccounts.put("PLAYER", new World.Account(1, "PLAYER", sess.k, 0, 1, "Win", "x86"));
        CaptureSink sink = new CaptureSink();
        int seed = 99;
        WorldSession s = new WorldSession(sink, seed);
        s.handle(w, Opcodes.CMSG_AUTH_SESSION, authSession("PLAYER", seed, sess.k, 8606).array());
        assertEquals(Opcodes.SMSG_AUTH_RESPONSE, sink.opcodes.get(1));
        byte[] ok = sink.payloads.get(1);
        assertEquals(Codes.AUTH_OK, ok[0] & 0xFF);
        assertEquals(1, ok[ok.length - 1] & 0xFF);
        assertEquals(WorldSession.STATUS_AUTHED, s.status());
    }

    @Test
    void tpSl02IdlePingStaysAuthed() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Pinger", 0);
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_PING, ping(1));
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_PONG));
        assertFalse(sink.closed);
        assertEquals(WorldSession.STATUS_LOGGEDIN, s.status());
    }

    @Test
    void tpSl02OverspeedPingsKickPlayer() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = loggedIn(w, sink, "Flood", 0);
        for (int i = 0; i < 4; i++) {
            s.handle(w, Opcodes.CMSG_PING, ping(i));
        }
        assertTrue(sink.closed);
    }

    @Test
    void tpSl03AllianceHordeAndDuplicateName() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Allyone", 1, 1).array());
        assertEquals(Codes.CHAR_CREATE_SUCCESS, sink.payloads.get(0)[0] & 0xFF);
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Hordone", 2, 1).array());
        assertEquals(Codes.CHAR_CREATE_SUCCESS, sink.payloads.get(1)[0] & 0xFF);
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Allyone", 1, 1).array());
        assertEquals(Codes.CHAR_CREATE_NAME_IN_USE, sink.payloads.get(2)[0] & 0xFF);
        sink.opcodes.clear();
        sink.payloads.clear();
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertEquals(2, sink.payloads.get(0)[0] & 0xFF);
        Player ally = w.characters.enumAccount(1, w.objectMgr).stream()
                .filter(p -> p.name.equals("Allyone")).findFirst().orElseThrow();
        Player horde = w.characters.enumAccount(1, w.objectMgr).stream()
                .filter(p -> p.name.equals("Hordone")).findFirst().orElseThrow();
        assertEquals(ChrStatic.team(1), ally.team);
        assertEquals(ChrStatic.team(2), horde.team);
        assertEquals(ally.bindMap, ally.mapId);
        assertEquals(horde.bindMap, horde.mapId);
    }

    @Test
    void tpSl03BloodElfNeedsExpansion() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 0, "Win", "x86"));
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Bloodone", 10, 1).array());
        assertEquals(Codes.CHAR_CREATE_EXPANSION, sink.payloads.get(0)[0] & 0xFF);
    }

    @Test
    void tpSl03DeleteRemovesFromEnum() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Goneone", 1, 1).array());
        Player p = w.characters.enumAccount(1, w.objectMgr).get(0);
        sink.opcodes.clear();
        sink.payloads.clear();
        WowBuffer del = new WowBuffer(8);
        del.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_CHAR_DELETE, del.array());
        assertEquals(Codes.CHAR_DELETE_SUCCESS, sink.payloads.get(0)[0] & 0xFF);
        sink.opcodes.clear();
        sink.payloads.clear();
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertEquals(0, sink.payloads.get(0)[0] & 0xFF);
    }

    @Test
    void tpSl03GuildLeaderDeleteRefused() {
        World w = World.inMemory();
        CaptureSink sink = new CaptureSink();
        WorldSession s = new WorldSession(sink, 1);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86"));
        s.handle(w, Opcodes.CMSG_CHAR_CREATE, charCreate("Leaderone", 1, 1).array());
        Player p = w.characters.enumAccount(1, w.objectMgr).get(0);
        p.guildLeader = true;
        sink.opcodes.clear();
        sink.payloads.clear();
        WowBuffer del = new WowBuffer(8);
        del.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_CHAR_DELETE, del.array());
        assertEquals(Codes.CHAR_DELETE_FAILED_GUILD_LEADER, sink.payloads.get(0)[0] & 0xFF);
        sink.opcodes.clear();
        sink.payloads.clear();
        s.handle(w, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        assertEquals(1, sink.payloads.get(0)[0] & 0xFF);
    }

    private static WorldSession loggedIn(World w, CaptureSink sink, String name, int gm) {
        WorldSession s = new WorldSession(sink, 0x11111111);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], gm, 1, "Win", "x86"));
        Player p = w.characters.create(1, name, 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        return s;
    }

    private static byte[] ping(int seq) {
        WowBuffer b = new WowBuffer(8);
        b.putU32(seq);
        b.putU32(0);
        return b.array();
    }

    private static WowBuffer charCreate(String name, int race, int clazz) {
        WowBuffer create = new WowBuffer(32);
        create.putCString(name);
        create.putU8(race);
        create.putU8(clazz);
        create.putU8(0);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(1);
        create.putU8(0);
        create.putU8(0);
        return create;
    }

    private static WowBuffer authSession(String user, int serverSeed, byte[] k, int build) {
        int clientSeed = 7;
        WowBuffer proof = new WowBuffer(user.length() + 4 + 4 + 4 + 40);
        proof.putBytes(user.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        proof.putU32(0);
        proof.putU32(clientSeed);
        proof.putU32(serverSeed);
        proof.putBytes(k);
        byte[] digest = Sha1.hash(proof.array());
        WowBuffer in = new WowBuffer(128);
        in.putU32(build);
        in.putU32(0);
        in.putCString(user);
        in.putU32(clientSeed);
        in.putBytes(digest);
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
