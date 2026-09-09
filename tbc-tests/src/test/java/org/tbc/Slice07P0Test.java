package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice 7 spell P0 wire beyond the Gherkin: kill path parity, cast timer, cooldowns, auras. */
class Slice07P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final int FIREBALL = 133;

    /** TP-SL07-011 — Unit::DealDamage → Unit::Kill for spell damage too: XP log, PLAYER_XP, lootable corpse. */
    @Test
    void tpSl07SpellKillRewardsAndLoots() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Pyro", 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.spells.add(FIREBALL);
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        c.setHealth(1);
        client.clear();
        client.castSpell(world, FIREBALL, 1, c.guid);
        assertFalse(c.alive());
        byte[] log = client.payload(Opcodes.SMSG_LOG_XPGAIN);
        assertEquals(22, log.length);
        assertEquals(c.guid, WowClientDouble.u64le(log, 0));
        assertEquals(50, WowClientDouble.u32le(log, 8));
        assertEquals(50, client.valuesField(p.guid, UpdateFields.PLAYER_XP));
        client.clear();
        client.loot(world, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
    }
}
