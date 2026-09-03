package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PetHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL18-* from spec/03-protocol/packets/pet.md */
class Slice18P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl18PetSpellsAttack() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Hunter");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer act = new WowBuffer(20);
        act.putU64(0);
        act.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        act.putU64(2);
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_ACTION, act.array());
        byte[] bar = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(p.pet.guid, WowClientDouble.u64le(bar, 0));
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSTART));
        assertNotNull(p.pet);
    }

    @Test
    void tpSl18StableResultBytes() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Hunter");
        client.session().player().clazz = PetHandler.CLASS_HUNTER;
        client.clear();
        client.handle(world, Opcodes.CMSG_BUY_STABLE_SLOT, new byte[0]);
        client.handle(world, Opcodes.CMSG_STABLE_PET, new byte[0]);
        client.handle(world, Opcodes.CMSG_UNSTABLE_PET, new byte[0]);
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) == Opcodes.SMSG_STABLE_RESULT) {
                codes.add(client.payloads.get(i)[0] & 0xFF);
            }
        }
        assertTrue(codes.contains(PetHandler.BUY_SLOT_OK));
        assertTrue(codes.contains(PetHandler.STABLE_OK));
        assertTrue(codes.contains(PetHandler.UNSTABLE_OK));
    }

    @Test
    void tpSl18WarlockDismissHunterKeepsPet() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "PetClass");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        WowBuffer dismiss = new WowBuffer(20);
        dismiss.putU64(0);
        dismiss.putU32(PetHandler.COMMAND_DISMISS | (PetHandler.ACT_COMMAND << 24));
        client.handle(world, Opcodes.CMSG_PET_ACTION, dismiss.array());
        assertNotNull(p.pet);
        p.clazz = 9;
        client.handle(world, Opcodes.CMSG_PET_ACTION, dismiss.array());
        assertNull(p.pet);
        byte[] hide = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(0L, WowClientDouble.u64le(hide, 0));
    }

    @Test
    void tpSl18TotemDestroyed() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Shaman");
        Player p = client.session().player();
        p.totems[0] = 99;
        client.clear();
        WowBuffer tot = new WowBuffer(1);
        tot.putU8(0);
        client.handle(world, Opcodes.CMSG_TOTEM_DESTROYED, tot.array());
        assertEquals(0, p.totems[0]);
        assertEquals(99L, WowClientDouble.u64le(lastPayload(client, Opcodes.SMSG_DESTROY_OBJECT), 0));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
