package org.tbc.matrix;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryHandlerTest {
    static final class Capture implements PacketSink {
        final List<Integer> opcodes = new ArrayList<>();
        final Map<Integer, byte[]> last = new HashMap<>();

        @Override
        public void send(int opcode, byte[] payload) {
            opcodes.add(opcode);
            last.put(opcode, payload == null ? new byte[0] : payload);
        }

        @Override
        public void close() {
        }
    }

    @Test
    void creatureKnownSuccessAndUnknownFailBit() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "QryBot", 3);

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CREATURE_QUERY, creatureQuery(6, 0));
        byte[] ok = sink.last.get(Opcodes.SMSG_CREATURE_QUERY_RESPONSE);
        assertTrue(sink.opcodes.contains(Opcodes.SMSG_CREATURE_QUERY_RESPONSE));
        assertEquals(6, u32(ok, 0));
        assertTrue(cstring(ok, 4).contains("Kobold"));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_CREATURE_QUERY, creatureQuery(0x00FFFFFF, 0));
        byte[] fail = sink.last.get(Opcodes.SMSG_CREATURE_QUERY_RESPONSE);
        assertEquals(4, fail.length);
        assertEquals(0x00FFFFFF | 0x80000000, u32(fail, 0));
    }

    @Test
    void itemFailBitAndWornShortswordSuccess() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "ItemBot", 3);

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_ITEM_QUERY_SINGLE, u32buf(0x00FFFFFF));
        byte[] fail = sink.last.get(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE);
        assertEquals(4, fail.length);
        assertEquals(0x00FFFFFF | 0x80000000, u32(fail, 0));

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_ITEM_QUERY_SINGLE, u32buf(25));
        byte[] ok = sink.last.get(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE);
        assertTrue(ok.length > 4);
        assertEquals(25, u32(ok, 0));
        assertTrue(cstring(ok, 16).contains("Worn Shortsword"));
    }

    @Test
    void pageMissingTextPetUnknownGuildEmpty() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "MiscBot", 3);

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_PAGE_TEXT_QUERY, u32buf(0x00FFFFFF));
        byte[] page = sink.last.get(Opcodes.SMSG_PAGE_TEXT_QUERY_RESPONSE);
        assertEquals(0x00FFFFFF, u32(page, 0));
        assertEquals("Item page missing.", cstring(page, 4));
        assertEquals(0, u32(page, 4 + "Item page missing.".length() + 1));

        sink.opcodes.clear();
        WowBuffer pet = new WowBuffer(12);
        pet.putU32(1);
        pet.putU64(0);
        s.handle(w, Opcodes.CMSG_PET_NAME_QUERY, pet.array());
        byte[] petOut = sink.last.get(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE);
        assertEquals(10, petOut.length);
        assertEquals(1, u32(petOut, 0));
        assertEquals(0, petOut[4]);
        assertEquals(0, u32(petOut, 5));
        assertEquals(0, petOut[9]);

        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_GUILD_QUERY, u32buf(1));
        byte[] guild = sink.last.get(Opcodes.SMSG_GUILD_QUERY_RESPONSE);
        assertEquals(1, u32(guild, 0));
        assertEquals(0, guild[4]);
    }

    @Test
    void petKnownNameFromPlayerPet() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "PetBot", 3);
        Pet pet = new Pet();
        pet.guid = 0x1001L;
        pet.name = "Fluffy";
        s.player().pet = pet;

        sink.opcodes.clear();
        WowBuffer in = new WowBuffer(12);
        in.putU32(7);
        in.putU64(0x1001L);
        s.handle(w, Opcodes.CMSG_PET_NAME_QUERY, in.array());
        byte[] out = sink.last.get(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE);
        assertEquals(7, u32(out, 0));
        assertEquals("Fluffy", cstring(out, 4));
    }

    @Test
    void whoisGmGate() {
        World w = World.inMemory();
        Capture gmSink = new Capture();
        WorldSession gm = loggedIn(w, gmSink, "WhoisGm", 3);
        Capture playerSink = new Capture();
        WorldSession player = loggedIn(w, playerSink, "WhoisPl", 0);

        playerSink.opcodes.clear();
        WowBuffer name = new WowBuffer(16);
        name.putCString("WhoisGm");
        player.handle(w, Opcodes.CMSG_WHOIS, name.array());
        assertFalse(playerSink.opcodes.contains(Opcodes.SMSG_WHOIS));

        gmSink.opcodes.clear();
        WowBuffer gmName = new WowBuffer(16);
        gmName.putCString("WhoisPl");
        gm.handle(w, Opcodes.CMSG_WHOIS, gmName.array());
        assertTrue(gmSink.opcodes.contains(Opcodes.SMSG_WHOIS));
        String msg = cstring(gmSink.last.get(Opcodes.SMSG_WHOIS), 0);
        assertTrue(msg.contains("account is"), msg);
    }

    @Test
    void queryTimeHasDailyResetOffset() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "TimeBot", 3);
        sink.opcodes.clear();
        s.handle(w, Opcodes.CMSG_QUERY_TIME, new byte[0]);
        byte[] payload = sink.last.get(Opcodes.SMSG_QUERY_TIME_RESPONSE);
        assertEquals(8, payload.length);
        assertEquals(0, u32(payload, 4));
    }

    @Test
    void questAndGameObjectUnknownFailBit() {
        World w = World.inMemory();
        Capture sink = new Capture();
        WorldSession s = loggedIn(w, sink, "FailBot", 3);

        s.handle(w, Opcodes.CMSG_QUEST_QUERY, u32buf(0x00FFFFFF));
        byte[] q = sink.last.get(Opcodes.SMSG_QUEST_QUERY_RESPONSE);
        assertEquals(4, q.length);
        assertEquals(0x00FFFFFF | 0x80000000, u32(q, 0));

        s.handle(w, Opcodes.CMSG_GAMEOBJECT_QUERY, creatureQuery(0x00FFFFFF, 0));
        byte[] go = sink.last.get(Opcodes.SMSG_GAMEOBJECT_QUERY_RESPONSE);
        assertEquals(4, go.length);
        assertEquals(0x00FFFFFF | 0x80000000, u32(go, 0));
    }

    private static byte[] creatureQuery(int entry, long guid) {
        WowBuffer b = new WowBuffer(12);
        b.putU32(entry);
        b.putU64(guid);
        return b.array();
    }

    private static byte[] u32buf(int v) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(v);
        return b.array();
    }

    private static int u32(byte[] p, int off) {
        return (p[off] & 0xFF)
                | ((p[off + 1] & 0xFF) << 8)
                | ((p[off + 2] & 0xFF) << 16)
                | ((p[off + 3] & 0xFF) << 24);
    }

    private static String cstring(byte[] p, int off) {
        int end = off;
        while (end < p.length && p[end] != 0) {
            end++;
        }
        return new String(p, off, end - off, StandardCharsets.UTF_8);
    }

    private static WorldSession loggedIn(World w, Capture sink, String name, int gmlevel) {
        WorldSession s = new WorldSession(sink, 0x11111111);
        s.injectAccount(new World.Account(1, "PLAYER", new byte[40], gmlevel, 1, "Win", "x86"));
        Player p = w.characters.create(1, name, 1, 1, 0, 1, 1, 1, 1, 0, w.objectMgr);
        WowBuffer g = new WowBuffer(8);
        g.putU64(p.guid);
        s.handle(w, Opcodes.CMSG_PLAYER_LOGIN, g.array());
        return s;
    }
}
