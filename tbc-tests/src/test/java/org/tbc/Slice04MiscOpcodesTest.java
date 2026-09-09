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

    private static WowClientDouble enter(World world, World.Account acc, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(acc);
        Player created = world.characters.create(acc.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
