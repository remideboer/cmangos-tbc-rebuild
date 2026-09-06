package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.spell.GameObjectUse;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-* from spell.md */
class Slice26P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl26GoLoot() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        client.clear();
        WowBuffer go = new WowBuffer(8);
        go.putU64(1);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, go.array());
        byte[] loot = lastPayload(client, Opcodes.SMSG_LOOT_RESPONSE);
        assertEquals(1L, WowClientDouble.u64le(loot, 0));
        assertEquals(Combat.LOOT_CORPSE, loot[8] & 0xFF);
    }

    @Test
    void tpSl26GoDoorOpens() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        org.tbc.world.entity.GameObject door = new org.tbc.world.entity.GameObject();
        door.guid = 42;
        door.type = GameObjectUse.TYPE_DOOR;
        door.state = GameObjectUse.STATE_READY;
        world.map(p.mapId, p.instanceId).gameObjects.put(door.guid, door);
        client.clear();
        WowBuffer use = new WowBuffer(8);
        use.putU64(door.guid);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, use.array());
        assertEquals(GameObjectUse.STATE_ACTIVE, door.state);
        byte[] anim = lastPayload(client, Opcodes.SMSG_GAMEOBJECT_CUSTOM_ANIM);
        assertEquals(42L, WowClientDouble.u64le(anim, 0));
        assertEquals(0, WowClientDouble.u32le(anim, 8));
        assertFalse(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
    }

    @Test
    void tpSl26OpenLockChest() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        org.tbc.world.entity.GameObject chest = new org.tbc.world.entity.GameObject();
        chest.guid = 99;
        chest.type = GameObjectUse.TYPE_CHEST;
        chest.state = GameObjectUse.STATE_READY;
        world.map(p.mapId, p.instanceId).gameObjects.put(chest.guid, chest);
        client.clear();
        WowBuffer use = new WowBuffer(8);
        use.putU64(chest.guid);
        client.handle(world, Opcodes.CMSG_GAMEOBJ_USE, use.array());
        assertEquals(GameObjectUse.STATE_ACTIVE, chest.state);
        assertTrue(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
        assertEquals(99L, WowClientDouble.u64le(lastPayload(client, Opcodes.SMSG_LOOT_RESPONSE), 0));
    }

    @Test
    void tpSl26TalentWipeSpell() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        client.clear();
        client.handle(world, Opcodes.MSG_TALENT_WIPE_CONFIRM, new byte[8]);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.TALENT_WIPE));
        assertEquals(PvpObjectives.TALENT_WIPE,
                WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_LEARNED_SPELL), 0));
    }

    @Test
    void tpSl26CancelChannelling() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        p.channeling = true;
        client.clear();
        WowBuffer cancel = new WowBuffer(4);
        cancel.putU32(0);
        client.handle(world, Opcodes.CMSG_CANCEL_CHANNELLING, cancel.array());
        assertFalse(p.channeling);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_FAILURE));
    }

    @Test
    void tpSl26CancelAura() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        p.auras.add(new org.tbc.world.entity.Unit.Aura(org.tbc.world.script.ClassScripts.SPELL_UNSTABLE_AFFLICTION, 30_000, 1));
        client.clear();
        WowBuffer cancel = new WowBuffer(4);
        cancel.putU32(org.tbc.world.script.ClassScripts.SPELL_UNSTABLE_AFFLICTION);
        client.handle(world, Opcodes.CMSG_CANCEL_AURA, cancel.array());
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == org.tbc.world.script.ClassScripts.SPELL_UNSTABLE_AFFLICTION));
    }

    @Test
    void tpSl26StandStateSit() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        assertTrue(p.isStanding());
        client.clear();
        WowBuffer sit = new WowBuffer(4);
        sit.putU32(org.tbc.world.entity.Unit.UNIT_STAND_STATE_SIT);
        client.handle(world, Opcodes.CMSG_STANDSTATECHANGE, sit.array());
        assertEquals(org.tbc.world.entity.Unit.UNIT_STAND_STATE_SIT, p.standState());
        WowBuffer junk = new WowBuffer(4);
        junk.putU32(99);
        client.handle(world, Opcodes.CMSG_STANDSTATECHANGE, junk.array());
        assertEquals(org.tbc.world.entity.Unit.UNIT_STAND_STATE_SIT, p.standState());
    }

    @Test
    void tpSl26UnlearnSkill() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        p.learnSkill(171, 1, 75, 1, true);
        assertTrue(p.hasSkill(171));
        client.clear();
        WowBuffer unlearn = new WowBuffer(4);
        unlearn.putU32(171);
        client.handle(world, Opcodes.CMSG_UNLEARN_SKILL, unlearn.array());
        assertFalse(p.hasSkill(171));
        p.learnSkill(98, 300, 300, 0, false);
        WowBuffer refuse = new WowBuffer(4);
        refuse.putU32(98);
        client.handle(world, Opcodes.CMSG_UNLEARN_SKILL, refuse.array());
        assertTrue(p.hasSkill(98));
    }

    @Test
    void tpSl26MirrorImageData() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        p.skin = 2;
        p.face = 3;
        p.hairStyle = 4;
        p.hairColor = 5;
        p.facialHair = 6;
        p.displayId = 49;
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_BYTES,
                (p.skin & 0xFF) | ((p.face & 0xFF) << 8) | ((p.hairStyle & 0xFF) << 16) | ((p.hairColor & 0xFF) << 24));
        p.setInt(org.tbc.world.net.wow8606.UpdateFields.PLAYER_BYTES_2, p.facialHair & 0xFF);
        org.tbc.world.entity.Creature clone = new org.tbc.world.entity.Creature();
        clone.guid = 500L;
        clone.applyTemplate(1, "Image", 49, 35, 100, 70);
        clone.setInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BYTES_0,
                (p.race & 0xFF) | ((p.clazz & 0xFF) << 8) | ((p.gender & 0xFF) << 16));
        clone.auras.add(new org.tbc.world.entity.Unit.Aura(36847, 0, 1));
        clone.mirrorImageCasterGuid = p.guid;
        world.map(p.mapId, p.instanceId).creatures.put(clone.guid, clone);
        client.clear();
        WowBuffer req = new WowBuffer(8);
        req.putU64(clone.guid);
        client.handle(world, Opcodes.CMSG_GET_MIRRORIMAGE_DATA, req.array());
        byte[] data = lastPayload(client, Opcodes.SMSG_MIRRORIMAGE_DATA);
        assertEquals(clone.guid, WowClientDouble.u64le(data, 0));
        assertEquals(49, WowClientDouble.u32le(data, 8));
        assertEquals(p.race & 0xFF, data[12] & 0xFF);
        assertEquals(p.gender & 0xFF, data[13] & 0xFF);
        assertEquals(2, data[14] & 0xFF);
        assertEquals(3, data[15] & 0xFF);
        assertEquals(4, data[16] & 0xFF);
        assertEquals(5, data[17] & 0xFF);
        assertEquals(6, data[18] & 0xFF);
        assertEquals(0, WowClientDouble.u32le(data, 19));
    }

    @Test
    void tpSl26FarSightToggleView() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Caster");
        Player p = client.session().player();
        org.tbc.world.entity.Creature totem = new org.tbc.world.entity.Creature();
        totem.guid = 777L;
        totem.applyTemplate(1, "Sight", 1, 35, 100, 1);
        world.map(p.mapId, p.instanceId).creatures.put(totem.guid, totem);
        p.setFarSightGuid(totem.guid);
        assertEquals(0L, p.cameraViewGuid());
        client.clear();
        WowBuffer setView = new WowBuffer(1);
        setView.putU8(1);
        client.handle(world, Opcodes.CMSG_FAR_SIGHT, setView.array());
        assertEquals(totem.guid, p.cameraViewGuid());
        assertEquals(totem.guid, p.farSightGuid());
        WowBuffer reset = new WowBuffer(1);
        reset.putU8(0);
        client.handle(world, Opcodes.CMSG_FAR_SIGHT, reset.array());
        assertEquals(0L, p.cameraViewGuid());
        assertEquals(totem.guid, p.farSightGuid());
        world.map(p.mapId, p.instanceId).creatures.remove(totem.guid);
        WowBuffer missing = new WowBuffer(1);
        missing.putU8(1);
        client.handle(world, Opcodes.CMSG_FAR_SIGHT, missing.array());
        assertEquals(0L, p.cameraViewGuid());
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
