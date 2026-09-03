package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.pvp.PvpObjectives;
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
