package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PetHandler;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.spell.SpellEngine;
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

    /**
     * TP-SL18-006 — CMSG_PET_SET_ACTION rewrites a spell slot and SMSG_PET_SPELLS.
     * HandlePetSetAction: position &lt; 10, known spell; command/reaction cannot be removed (count 1).
     */
    @Test
    void tpSl18PetSetAction() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "BarEdit");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(1, 3, 2947 | (0x81 << 24)));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;
        p.pet.learnSpell(2947);

        client.clear();
        int disabled = 2947 | (0x81 << 24);
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid, 3, disabled));
        byte[] bar = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(petGuid, WowClientDouble.u64le(bar, 0));
        assertEquals(disabled, WowClientDouble.u32le(bar, 16 + 3 * 4));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid, 4, 99999 | (0x81 << 24)));
        byte[] unknown = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(0, WowClientDouble.u32le(unknown, 16 + 4 * 4));
        assertEquals(disabled, WowClientDouble.u32le(unknown, 16 + 3 * 4));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION,
                setActionPayload(petGuid, 0, PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24)));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid, 10, disabled));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));

        client.clear();
        WowBuffer swap = new WowBuffer(24);
        swap.putU64(petGuid);
        swap.putU32(3);
        swap.putU32(PetHandler.ACT_DISABLED << 24);
        swap.putU32(4);
        swap.putU32(disabled);
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, swap.array());
        byte[] swapped = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(PetHandler.ACT_DISABLED << 24, WowClientDouble.u32le(swapped, 16 + 3 * 4));
        assertEquals(disabled, WowClientDouble.u32le(swapped, 16 + 4 * 4));

        client.clear();
        WowBuffer badCmd = new WowBuffer(24);
        badCmd.putU64(petGuid);
        badCmd.putU32(0);
        badCmd.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        badCmd.putU32(1);
        badCmd.putU32(1 | (PetHandler.ACT_COMMAND << 24));
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, badCmd.array());
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid + 1, 3, disabled));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, new byte[8]);
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid, 0, 1 | (PetHandler.ACT_REACTION << 24)));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        WowBuffer shortPkt = new WowBuffer(12);
        shortPkt.putU64(petGuid);
        shortPkt.putU32(3);
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, shortPkt.array());
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
    }

    /**
     * TP-SL18-006 — CMSG_PET_SPELL_AUTOCAST toggles the autocast bit in SMSG_PET_SPELLS.
     * HandlePetSpellAutocastOpcode → SetSpellAutocast ACT_ENABLED / ACT_DISABLED.
     */
    @Test
    void tpSl18PetSpellAutocast() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "AutoCast");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(1, 2947, 1));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;
        p.pet.learnSpell(2947);
        int disabled = 2947 | (PetHandler.ACT_DISABLED << 24);
        int enabled = 2947 | (PetHandler.ACT_ENABLED << 24);
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION, setActionPayload(petGuid, 3, disabled));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid, 2947, 1));
        byte[] on = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(enabled, WowClientDouble.u32le(on, 16 + 3 * 4));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid, 2947, 0));
        byte[] off = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(disabled, WowClientDouble.u32le(off, 16 + 3 * 4));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid, 99999, 1));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        p.pet.learnSpell(133);
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid, 133, 1));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid + 1, 2947, 1));
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        WowBuffer guidOnly = new WowBuffer(8);
        guidOnly.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, guidOnly.array());
        assertFalse(client.saw(Opcodes.SMSG_PET_SPELLS));
        client.handle(world, Opcodes.CMSG_PET_SET_ACTION,
                setActionPayload(petGuid, 3, 2947 | (PetHandler.ACT_PASSIVE << 24)));
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_SPELL_AUTOCAST, autocastPayload(petGuid, 2947, 1));
        byte[] fromPassive = lastPayload(client, Opcodes.SMSG_PET_SPELLS);
        assertEquals(enabled, WowClientDouble.u32le(fromPassive, 16 + 3 * 4));
    }

    /**
     * TP-SL18-006 — CMSG_PET_CAST_SPELL casts through the pet.
     * HandlePetCastSpellOpcode: SMSG_SPELL_START caster packed GUID is the pet.
     */
    @Test
    void tpSl18PetCastSpell() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "PetCast");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL, petCastPayload(1, SpellEngine.FIREBALL, 2));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;
        Creature prey = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(prey);

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL,
                petCastPayload(petGuid, SpellEngine.FIREBALL, prey.guid));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));

        p.pet.learnSpell(SpellEngine.FIREBALL);
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL,
                petCastPayload(petGuid, SpellEngine.FIREBALL, prey.guid));
        byte[] start = lastPayload(client, Opcodes.SMSG_SPELL_START);
        assertEquals(petGuid, packedGuid(start, 0));
        int off = WowClientDouble.skipPackedGuid(start, 0);
        off = WowClientDouble.skipPackedGuid(start, off);
        assertEquals(SpellEngine.FIREBALL, WowClientDouble.u32le(start, off));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL,
                petCastPayload(petGuid + 1, SpellEngine.FIREBALL, prey.guid));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL,
                petCastPayload(petGuid, 99999, prey.guid));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        WowBuffer guidOnly = new WowBuffer(8);
        guidOnly.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL, guidOnly.array());
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));

        p.pet.learnSpell(SpellEngine.FROST_NOVA);
        client.clear();
        client.handle(world, Opcodes.CMSG_PET_CAST_SPELL,
                petCastPayload(petGuid, SpellEngine.FROST_NOVA, prey.guid));
        byte[] instant = lastPayload(client, Opcodes.SMSG_SPELL_START);
        assertEquals(petGuid, packedGuid(instant, 0));
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(petGuid, packedGuid(lastPayload(client, Opcodes.SMSG_SPELL_GO), 0));
    }

    /**
     * TP-SL18-006 — CMSG_PET_STOP_ATTACK while the pet is swinging.
     * HandlePetStopAttack → AttackStop → SMSG_ATTACKSTOP packed pet → victim, nowDead 0.
     */
    @Test
    void tpSl18PetStopAttack() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "PetStop");
        Player p = client.session().player();
        p.clazz = PetHandler.CLASS_HUNTER;
        Creature prey = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(prey);
        WowBuffer summon = new WowBuffer(20);
        summon.putU64(0);
        summon.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        summon.putU64(0);
        client.handle(world, Opcodes.CMSG_PET_ACTION, summon.array());
        long petGuid = p.pet.guid;

        client.clear();
        WowBuffer stopIdle = new WowBuffer(8);
        stopIdle.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_STOP_ATTACK, stopIdle.array());
        assertFalse(client.saw(Opcodes.SMSG_ATTACKSTOP));

        WowBuffer act = new WowBuffer(20);
        act.putU64(petGuid);
        act.putU32(PetHandler.COMMAND_ATTACK | (PetHandler.ACT_COMMAND << 24));
        act.putU64(prey.guid);
        client.handle(world, Opcodes.CMSG_PET_ACTION, act.array());
        assertTrue(client.saw(Opcodes.SMSG_ATTACKSTART));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_STOP_ATTACK, new byte[0]);
        assertFalse(client.saw(Opcodes.SMSG_ATTACKSTOP));
        WowBuffer wrong = new WowBuffer(8);
        wrong.putU64(petGuid + 1);
        client.handle(world, Opcodes.CMSG_PET_STOP_ATTACK, wrong.array());
        assertFalse(client.saw(Opcodes.SMSG_ATTACKSTOP));

        WowBuffer stop = new WowBuffer(8);
        stop.putU64(petGuid);
        client.handle(world, Opcodes.CMSG_PET_STOP_ATTACK, stop.array());
        byte[] pkt = lastPayload(client, Opcodes.SMSG_ATTACKSTOP);
        int off = 0;
        assertEquals(petGuid, packedGuid(pkt, off));
        off = WowClientDouble.skipPackedGuid(pkt, off);
        assertEquals(prey.guid, packedGuid(pkt, off));
        off = WowClientDouble.skipPackedGuid(pkt, off);
        assertEquals(0, WowClientDouble.u32le(pkt, off));

        client.clear();
        client.handle(world, Opcodes.CMSG_PET_STOP_ATTACK, stop.array());
        assertFalse(client.saw(Opcodes.SMSG_ATTACKSTOP));
    }

    private static byte[] petCastPayload(long petGuid, int spellId, long targetGuid) {
        WowBuffer in = new WowBuffer(32);
        in.putU64(petGuid);
        in.putU32(spellId);
        in.putU32(SpellCastTargets.UNIT);
        in.putPackedGuid(targetGuid);
        return in.array();
    }

    private static byte[] autocastPayload(long petGuid, int spellId, int state) {
        WowBuffer in = new WowBuffer(13);
        in.putU64(petGuid);
        in.putU32(spellId);
        in.putU8(state);
        return in.array();
    }

    private static byte[] setActionPayload(long petGuid, int position, int data) {
        WowBuffer in = new WowBuffer(16);
        in.putU64(petGuid);
        in.putU32(position);
        in.putU32(data);
        return in.array();
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
