package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** In-memory world defaults seeded outside ObjectMgr (refactoring plan cycle 4.1). */
class WorldDefaultsSeedTest {
    @Test
    void defaultsShouldSeedCreateInfoStarterSpellsAndNorthshireNpcs() {
        ObjectMgr m = new ObjectMgr();
        WorldDefaultsSeed.defaults(m);
        ObjectMgr.CreateInfo human = m.createInfo.get(ObjectMgr.key(1, 1));
        assertEquals(12, human.zone());
        assertTrue(m.createSpells.get((int) ObjectMgr.key(1, 1)).contains(6603));
        assertEquals("Kobold Vermin", m.creatures.get(6).name());
        assertEquals("Llane Beshere", m.creatures.get(Content.NPC_LLANE_BESHERE).name());
        assertEquals(1, m.trainerClass.get(Content.NPC_LLANE_BESHERE));
        assertNotNull(m.talents.get(124));
    }

    @Test
    void queryDefaultsShouldSeedManaWyrmBattlemasterGossipAndTaxi() {
        ObjectMgr m = new ObjectMgr();
        WorldDefaultsSeed.queryDefaults(m);
        assertEquals(65, m.creatureMana.get(15274));
        assertEquals(2, m.battleMasterBgType(2302));
        assertEquals(17, m.gossipOptions.get(0).size());
        assertEquals(Content.GOSSIP_MENU_FARLEY, m.gossipMenuIds.get(Content.NPC_INNKEEPER_FARLEY));
        assertNotNull(m.taxiPaths.get(ObjectMgr.taxiKey(Content.TAXI_STORMWIND, Content.TAXI_IRONFORGE)));
        assertTrue(m.spawns.stream().anyMatch(s -> s.entry() == Content.NPC_REBECCA_LAUGHLIN));
    }

    @Test
    void queryDefaultsTwiceShouldBeIdempotent() {
        ObjectMgr m = new ObjectMgr();
        WorldDefaultsSeed.queryDefaults(m);
        int spawns = m.spawns.size();
        int auctions = m.auctions.size();
        int menu0 = m.gossipOptions.get(0).size();
        WorldDefaultsSeed.queryDefaults(m);
        assertEquals(spawns, m.spawns.size());
        assertEquals(auctions, m.auctions.size());
        assertEquals(menu0, m.gossipOptions.get(0).size());
    }

    @Test
    void loadInMemoryShouldStillSeedBothDefaultSets() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        assertEquals(12, m.createInfo.get(ObjectMgr.key(1, 1)).zone());
        assertEquals(65, m.creatureMana.get(15274));
    }
}
