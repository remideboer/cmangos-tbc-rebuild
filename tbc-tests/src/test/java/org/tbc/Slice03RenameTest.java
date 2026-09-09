package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL03-006 — CMSG_CHAR_RENAME at character select (HandleCharRenameOpcode).
 * Forced rename needs AT_LOGIN_RENAME; success is SMSG_CHAR_RENAME RESPONSE_SUCCESS + guid + name.
 */
class Slice03RenameTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final int CHARACTER_FLAG_RENAME = 0x4000;

    @Test
    void tpSl03CharRename() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Oldname", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        created.atLogin |= Player.AT_LOGIN_RENAME;
        world.characters.save(created);

        client.handle(world, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        int flags = enumFlags(client.payload(Opcodes.SMSG_CHAR_ENUM));
        assertEquals(CHARACTER_FLAG_RENAME, flags & CHARACTER_FLAG_RENAME);

        client.clear();
        WowBuffer in = new WowBuffer(32);
        in.putU64(created.guid);
        in.putCString("newname");
        client.handle(world, Opcodes.CMSG_CHAR_RENAME, in.array());
        assertTrue(client.saw(Opcodes.SMSG_CHAR_RENAME));
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_CHAR_RENAME));
        assertEquals(Codes.RESPONSE_SUCCESS, out.getU8());
        assertEquals(created.guid, out.getU64());
        assertEquals("Newname", out.getCString());

        client.clear();
        client.handle(world, Opcodes.CMSG_CHAR_ENUM, new byte[0]);
        WowBuffer enumer = new WowBuffer(client.payload(Opcodes.SMSG_CHAR_ENUM));
        assertEquals(1, enumer.getU8());
        enumer.getU64();
        assertEquals("Newname", enumer.getCString());
        for (int i = 0; i < 9; i++) {
            enumer.getU8();
        }
        enumer.getU32();
        enumer.getU32();
        enumer.getFloat();
        enumer.getFloat();
        enumer.getFloat();
        enumer.getU32();
        assertEquals(0, enumer.getU32() & CHARACTER_FLAG_RENAME);
    }

    @Test
    void tpSl03CharRenameWhenNameTakenShouldCreateError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        world.characters.create(ACC.id(), "Takenone", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player created = world.characters.create(ACC.id(), "Renamer", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        created.atLogin |= Player.AT_LOGIN_RENAME;
        world.characters.save(created);
        WowBuffer in = new WowBuffer(32);
        in.putU64(created.guid);
        in.putCString("Takenone");
        client.handle(world, Opcodes.CMSG_CHAR_RENAME, in.array());
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_CHAR_RENAME));
        assertEquals(Codes.CHAR_CREATE_ERROR, out.getU8());
        assertEquals(0, out.remaining());
    }

    @Test
    void tpSl03CharRenameWhenEmptyNameShouldNoName() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Emptyone", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        created.atLogin |= Player.AT_LOGIN_RENAME;
        world.characters.save(created);
        WowBuffer in = new WowBuffer(16);
        in.putU64(created.guid);
        in.putCString("");
        client.handle(world, Opcodes.CMSG_CHAR_RENAME, in.array());
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_CHAR_RENAME));
        assertEquals(Codes.CHAR_NAME_NO_NAME, out.getU8());
        assertEquals(0, out.remaining());
    }

    @Test
    void tpSl03CharRenameWhenRenameFlagMissingShouldCreateError() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Noflag", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer in = new WowBuffer(32);
        in.putU64(created.guid);
        in.putCString("Newflag");
        client.handle(world, Opcodes.CMSG_CHAR_RENAME, in.array());
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_CHAR_RENAME));
        assertEquals(Codes.CHAR_CREATE_ERROR, out.getU8());
        assertEquals(0, out.remaining());
    }

    /**
     * HandleSetPlayerDeclinedNamesOpcode: Latin stored name is not Cyrillic → result 1.
     * C++ does not treat non-ruRU as success (YAML was wrong).
     */
    @Test
    void tpSl03SetPlayerDeclinedNames() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Latinone", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        WowBuffer in = new WowBuffer(64);
        in.putU64(created.guid);
        in.putCString("Latinone");
        for (int i = 0; i < 5; i++) {
            in.putCString("Latinone");
        }
        client.handle(world, Opcodes.CMSG_SET_PLAYER_DECLINED_NAMES, in.array());
        assertTrue(client.saw(Opcodes.SMSG_SET_PLAYER_DECLINED_NAMES_RESULT));
        WowBuffer out = new WowBuffer(client.payload(Opcodes.SMSG_SET_PLAYER_DECLINED_NAMES_RESULT));
        assertEquals(1, out.getU32());
        assertEquals(created.guid, out.getU64());
    }

    private static int enumFlags(byte[] payload) {
        WowBuffer b = new WowBuffer(payload);
        assertEquals(1, b.getU8());
        b.getU64();
        b.getCString();
        for (int i = 0; i < 9; i++) {
            b.getU8();
        }
        b.getU32();
        b.getU32();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        b.getU32();
        return b.getU32();
    }
}
