package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PetHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void tpSl18PetAttackWhenCommandShouldSendAttackerStateUpdate() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "HuntAtk");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        Creature prey = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(prey);
        int maxHp = prey.health();
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;
        client.clear();
        WowBuffer act = new WowBuffer(20);
        act.putU64(petGuid);
        act.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        act.putU64(prey.guid);
        client.handle(world, Opcodes.CMSG_PET_ACTION, act.array());
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSTART));
        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        byte[] pkt = lastPayload(client, Opcodes.SMSG_ATTACKERSTATEUPDATE);
        assertEquals(petGuid, packedGuid(pkt, 4));
        assertTrue(attackerStateDamage(pkt) > 0);
        assertTrue(prey.health() < maxHp);
    }

    /**
     * TP-SL18-006 — CMSG_PET_RENAME on a hunter pet with UNIT_CAN_BE_RENAMED.
     * HandlePetRename: name query timestamp + UNIT_FIELD_BYTES_2 rename flag cleared.
     */
    @Test
    void tpSl18PetRename() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Renamer");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(petGuid, ""));
        WowBuffer invalid = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_INVALID));
        assertEquals(3, invalid.getU32());
        assertEquals("", invalid.getCString());
        assertEquals(0, invalid.getU8());

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(petGuid, "Thirteenchars"));
        WowBuffer tooLong = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_INVALID));
        assertEquals(4, tooLong.getU32());
        assertEquals("Thirteenchars", tooLong.getCString());
        assertEquals(0, tooLong.getU8());

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(petGuid, "Wolfie"));
        int timestamp = client.valuesField(petGuid, UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP);
        assertTrue(timestamp > 0);
        int bytes2 = client.valuesField(petGuid, UpdateFields.UNIT_FIELD_BYTES_2);
        assertEquals(0, (bytes2 >>> 16) & 0x01);

        client.clear();
        WowBuffer query = new WowBuffer(12);
        query.putU32(1);
        query.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_NAME_QUERY, query.array());
        WowBuffer named = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_QUERY_RESPONSE));
        assertEquals(1, named.getU32());
        assertEquals("Wolfie", named.getCString());
        assertEquals(timestamp, named.getU32());
        assertEquals(0, named.getU8());

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(petGuid, "Fang"));
        assertFalse(client.saw(Opcodes.SMSG_PET_NAME_INVALID));
        client.handle(world, Opcodes.CMSG_PET_NAME_QUERY, query.array());
        WowBuffer still = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_QUERY_RESPONSE));
        still.getU32();
        assertEquals("Wolfie", still.getCString());
    }

    @Test
    void tpSl18PetRenameWhenWrongGuidShouldKeepName() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "WrongGuid");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(p.pet.guid + 1, "Wolfie"));
        assertFalse(client.saw(Opcodes.SMSG_PET_NAME_INVALID));
        WowBuffer query = new WowBuffer(12);
        query.putU32(1);
        query.putU64(p.pet.guid);
        client.handle(world, Opcodes.CMSG_PET_NAME_QUERY, query.array());
        WowBuffer named = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_QUERY_RESPONSE));
        named.getU32();
        assertEquals("Pet", named.getCString());
    }

    @Test
    void tpSl18PetRenameWhenSummonPetShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "LockPet");
        Player p = client.session().player();
        p.clazz = 9;
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(petGuid, "Wolfie"));
        assertFalse(client.saw(Opcodes.SMSG_PET_NAME_INVALID));
        WowBuffer query = new WowBuffer(12);
        query.putU32(1);
        query.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_NAME_QUERY, query.array());
        WowBuffer named = new WowBuffer(lastPayload(client, Opcodes.SMSG_PET_NAME_QUERY_RESPONSE));
        named.getU32();
        assertEquals("Pet", named.getCString());
    }

    @Test
    void tpSl18PetRenameWhenNoPetShouldIgnore() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "NoPet");
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_RENAME, renamePayload(1, "Wolfie"));
        assertFalse(client.saw(Opcodes.SMSG_PET_NAME_INVALID));
        client.handle(world, Opcodes.CMSG_PET_RENAME, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_PET_NAME_INVALID));
    }

    private static byte[] renamePayload(long petGuid, String name) {
        WowBuffer in = new WowBuffer(32);
        in.putU64(petGuid);
        in.putCString(name);
        in.putU8(0);
        return in.array();
    }

    private static long packedGuid(byte[] p, int off) {
        int mask = p[off] & 0xFF;
        long guid = 0;
        int i = off + 1;
        for (int bit = 0; bit < 8; bit++) {
            if ((mask & (1 << bit)) != 0) {
                guid |= ((long) (p[i++] & 0xFF)) << (8 * bit);
            }
        }
        return guid;
    }

    private static int attackerStateDamage(byte[] p) {
        int mask = p[4] & 0xFF;
        int i = 5;
        for (int bit = 0; bit < 8; bit++) {
            if ((mask & (1 << bit)) != 0) {
                i++;
            }
        }
        mask = p[i++] & 0xFF;
        for (int bit = 0; bit < 8; bit++) {
            if ((mask & (1 << bit)) != 0) {
                i++;
            }
        }
        return WowClientDouble.u32le(p, i);
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
