package org.tbc.world.content;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectMgrCreatureStatsTest {
    @Test
    void spawnCreatureWhenSeedKoboldShouldSetDefaultCombatReachAndMeleeFields() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(6, 0, 0, 0, 0, 0, null);
        assertEquals(1.5f, c.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH), 1e-4f);
        assertEquals(1f, c.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE), 1e-4f);
        assertEquals(3f, c.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE), 1e-4f);
        assertEquals(2000, c.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void spawnCreatureWhenTemplateMeleeStatsShouldWriteDamageReachAiNameAndExtraFlags() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.creatures.put(9000, new ObjectMgr.CreatureTemplate(
                9000, "Wolf", 1, 38, 50, 1, 0, "", "", 0,
                "", "", 0, 0, 0, 0, 0, 0, 0, 0, 1f, 1f, 0,
                "EventAI", Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT, 5f, 8f, 1600, 2.25f, 0, 0, 0));
        Creature c = mgr.spawnCreature(9000, 0, 0, 0, 0, 0, null);
        assertEquals(5f, c.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE), 1e-4f);
        assertEquals(8f, c.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE), 1e-4f);
        assertEquals(1600, c.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2.25f, c.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH), 1e-4f);
        assertEquals("EventAI", c.aiName);
        assertEquals(Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT, c.extraFlags);
    }

    @Test
    void giveStartItemsWhenMainhandShouldWriteWeaponDamageAndAttackTime() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.createItems.put((int) ObjectMgr.key(1, 1), List.of(new ObjectMgr.CreateItem(25, 1)));
        Player p = new Player();
        p.guid = 1;
        p.race = 1;
        p.clazz = 1;
        mgr.giveStartItems(p, new AtomicLong(10)::getAndIncrement);
        assertEquals(1f, p.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE), 1e-4f);
        assertEquals(3f, p.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE), 1e-4f);
        assertEquals(1900, p.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(1.5f, p.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH), 1e-4f);
    }

    @Test
    void spawnCreatureWhenFactionNeutralToAllShouldSetCreatureFlag() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        mgr.factions = org.tbc.world.combat.Factions.seeded();
        mgr.creatures.put(9001, new ObjectMgr.CreatureTemplate(9001, "Kobold", 1, 25, 42, 1, 0, "", "", 0));
        Creature c = mgr.spawnCreature(9001, 0, 0, 0, 0, 0, null);
        assertTrue(c.neutralToAll);
    }
}
