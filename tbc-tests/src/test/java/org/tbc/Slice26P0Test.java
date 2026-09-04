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
