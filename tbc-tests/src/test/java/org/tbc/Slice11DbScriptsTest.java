package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.ai.DbScriptStore;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Slice11DbScriptsTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void deathWhenDbscriptCastShouldSendSpellGo7164() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "DeathCast");
        Player p = client.session().player();
        world.objectMgr.dbScriptStore.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 0, 7164));
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        c.setHealth(1);
        client.clear();
        for (int i = 0; i < 50 && c.alive(); i++) {
            client.attackSwing(world, c.guid);
        }
        assertFalse(c.alive());
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(7164, spellId(client.payload(Opcodes.SMSG_SPELL_GO)));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static int spellId(byte[] p) {
        int off = WowClientDouble.skipPackedGuid(p, 0);
        off = WowClientDouble.skipPackedGuid(p, off);
        return WowClientDouble.u32le(p, off);
    }
}
