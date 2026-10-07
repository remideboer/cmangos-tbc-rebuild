package org.tbc.world.session;

import org.junit.jupiter.api.Test;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Character screen (CharacterHandler.cpp) extracted from WorldSession (refactoring plan cycle 2.3). */
class CharacterHandlerTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    private static final class RecordingSink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();
        byte[] last;

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
            last = payload;
        }

        @Override
        public void close() {
        }
    }

    @Test
    void registerShouldCoverExactlyTheCharacterScreenOpcodes() {
        OpcodeTable t = new OpcodeTable();
        CharacterHandler.register(t);
        assertEquals(Set.of(Opcodes.CMSG_CHAR_ENUM, Opcodes.CMSG_CHAR_CREATE, Opcodes.CMSG_CHAR_DELETE,
                Opcodes.CMSG_CHAR_RENAME, Opcodes.CMSG_SET_PLAYER_DECLINED_NAMES, Opcodes.CMSG_PLAYER_LOGIN),
                t.opcodes());
    }

    @Test
    void charEnumWhenAccountHasNoCharactersShouldSendEmptyList() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        CharacterHandler.charEnum(s, World.inMemory());
        assertEquals(List.of(Opcodes.SMSG_CHAR_ENUM), sink.ops);
        assertEquals(0, new WowBuffer(sink.last).getU8());
    }

    @Test
    void charCreateWhenRaceUnknownShouldSendError() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        WowBuffer in = new WowBuffer(32);
        in.putCString("Badrace");
        in.putU8(99);  // no such race in ChrRaces 8606
        in.putU8(1);   // Warrior
        for (int i = 0; i < 6; i++) {
            in.putU8(0);
        }
        CharacterHandler.charCreate(s, World.inMemory(), in);
        assertEquals(List.of(Opcodes.SMSG_CHAR_CREATE), sink.ops);
        assertEquals(Codes.CHAR_CREATE_ERROR, new WowBuffer(sink.last).getU8());
    }

    @Test
    void loginWhenCharacterUnknownShouldFailWithCode5() {
        RecordingSink sink = new RecordingSink();
        WorldSession s = new WorldSession(sink, 0);
        s.injectAccount(ACC);
        WowBuffer in = new WowBuffer(8);
        in.putU64(0xDEAD);
        CharacterHandler.login(s, World.inMemory(), in);
        assertEquals(List.of(Opcodes.SMSG_CHARACTER_LOGIN_FAILED), sink.ops);
        assertEquals(0x05, new WowBuffer(sink.last).getU8());
        assertEquals(WorldSession.STATUS_AUTHED, s.status());
    }
}
