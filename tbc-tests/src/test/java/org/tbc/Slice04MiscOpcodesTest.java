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

/**
 * TP-SL04-016 — MiscHandler leftover C2S (one method per opcode).
 */
class Slice04MiscOpcodesTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    /**
     * CMSG_PLAYED_TIME → SMSG_PLAYED_TIME total+level u32 (HandlePlayedTime).
     * A new character has both clocks at 0.
     */
    @Test
    void tpSl04PlayedTime() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Played");
        client.clear();
        client.handle(world, Opcodes.CMSG_PLAYED_TIME, new byte[0]);
        assertTrue(client.saw(Opcodes.SMSG_PLAYED_TIME));
        WowBuffer b = new WowBuffer(client.payload(Opcodes.SMSG_PLAYED_TIME));
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.remaining());
    }

    /**
     * CMSG_SHOWING_HELM — ToggleFlag PLAYER_FLAGS_HIDE_HELM (0x400) on PLAYER_FLAGS VALUES.
     */
    @Test
    void tpSl04ShowingHelmTogglesHideHelm() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Helmer");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.CMSG_SHOWING_HELM, new byte[0]);
        int hidden = client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS);
        assertEquals(Player.PLAYER_FLAGS_HIDE_HELM, hidden & Player.PLAYER_FLAGS_HIDE_HELM);
        client.clear();
        client.handle(world, Opcodes.CMSG_SHOWING_HELM, new byte[0]);
        int shown = client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS);
        assertEquals(0, shown & Player.PLAYER_FLAGS_HIDE_HELM);
    }

    /**
     * CMSG_SHOWING_CLOAK — ToggleFlag PLAYER_FLAGS_HIDE_CLOAK (0x800) on PLAYER_FLAGS VALUES.
     */
    @Test
    void tpSl04ShowingCloakTogglesHideCloak() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Cloaker");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.CMSG_SHOWING_CLOAK, new byte[0]);
        int hidden = client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS);
        assertEquals(Player.PLAYER_FLAGS_HIDE_CLOAK, hidden & Player.PLAYER_FLAGS_HIDE_CLOAK);
        client.clear();
        client.handle(world, Opcodes.CMSG_SHOWING_CLOAK, new byte[0]);
        int shown = client.valuesField(p.guid, UpdateFields.PLAYER_FLAGS);
        assertEquals(0, shown & Player.PLAYER_FLAGS_HIDE_CLOAK);
    }

    /**
     * CMSG_REALM_SPLIT → SMSG_REALM_SPLIT unk echo, state 0 (normal), date "01/01/01".
     */
    @Test
    void tpSl04RealmSplitNormal() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Splitter");
        client.clear();
        WowBuffer req = new WowBuffer(4);
        req.putU32(0x12345678);
        client.handle(world, Opcodes.CMSG_REALM_SPLIT, req.array());
        assertTrue(client.saw(Opcodes.SMSG_REALM_SPLIT));
        WowBuffer r = new WowBuffer(client.payload(Opcodes.SMSG_REALM_SPLIT));
        assertEquals(0x12345678, r.getU32());
        assertEquals(0, r.getU32());
        assertEquals("01/01/01", r.getCString());
        assertEquals(0, r.remaining());
    }

    /**
     * TP-SL04-009 / TP-SL04-016 — CMSG_UPDATE_ACCOUNT_DATA then CMSG_REQUEST_ACCOUNT_DATA
     * returns the same decompressed string (session-misc.md).
     */
    @Test
    void tpSl04AccountDataRoundTrip() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Macros");
        client.clear();
        byte[] raw = "layout-v1\0".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        WowBuffer upd = new WowBuffer(8 + 64);
        upd.putU32(6);
        upd.putU32(raw.length);
        upd.putBytes(deflate(raw));
        client.handle(world, Opcodes.CMSG_UPDATE_ACCOUNT_DATA, upd.array());
        WowBuffer req = new WowBuffer(4);
        req.putU32(6);
        client.handle(world, Opcodes.CMSG_REQUEST_ACCOUNT_DATA, req.array());
        assertTrue(client.saw(Opcodes.SMSG_UPDATE_ACCOUNT_DATA));
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_UPDATE_ACCOUNT_DATA));
        assertEquals(6, out.getU32());
        int len = out.getU32();
        assertEquals(9, len);
        byte[] inflated = inflate(out.remainingBytes(), len);
        assertEquals("layout-v1", new String(inflated, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * CMSG_SET_ACTIONBAR_TOGGLES — SetByteValue PLAYER_FIELD_BYTES offset 2.
     */
    @Test
    void tpSl04ActionBarToggles() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, ACC, "Bars");
        Player p = client.session().player();
        client.clear();
        WowBuffer in = new WowBuffer(1);
        in.putU8(0x07);
        client.handle(world, Opcodes.CMSG_SET_ACTIONBAR_TOGGLES, in.array());
        int bytes = client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_BYTES);
        assertEquals(0x07, (bytes >>> 16) & 0xFF);
    }

    private static byte[] deflate(byte[] raw) {
        java.util.zip.Deflater def = new java.util.zip.Deflater();
        def.setInput(raw);
        def.finish();
        byte[] z = new byte[128];
        int n = def.deflate(z);
        def.end();
        return java.util.Arrays.copyOf(z, n);
    }

    private static byte[] inflate(byte[] z, int size) {
        byte[] raw = new byte[size];
        java.util.zip.Inflater inf = new java.util.zip.Inflater();
        inf.setInput(z);
        try {
            assertEquals(size, inf.inflate(raw));
        } catch (java.util.zip.DataFormatException e) {
            throw new AssertionError(e);
        } finally {
            inf.end();
        }
        return raw;
    }

    private static WowClientDouble enter(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
