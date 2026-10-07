package org.tbc.world.content.catalog;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Narrow read-only spawn view over ObjectMgr; mutation only via named methods (cycle 4.3). */
class SpawnCatalogTest {
    @Test
    void creatureSpawnsWhenSeededShouldListGoldshireRows() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        SpawnCatalog spawns = m;
        assertTrue(spawns.creatureSpawns().stream().anyMatch(s -> s.entry() == Content.NPC_MARSHAL_DUGHAN));
    }

    @Test
    void creatureSpawnsWhenMutatedThroughViewShouldRefuse() {
        SpawnCatalog spawns = new ObjectMgr();
        assertThrows(UnsupportedOperationException.class,
                () -> spawns.creatureSpawns().add(new ObjectMgr.Spawn(1, 6, 0, 0f, 0f, 0f, 0f)));
        assertThrows(UnsupportedOperationException.class,
                () -> spawns.gameObjectSpawns().add(new ObjectMgr.Spawn(1, 6, 0, 0f, 0f, 0f, 0f)));
    }

    @Test
    void moveSpawnWhenGuidKnownShouldReplacePoseAndKeepRespawn() {
        ObjectMgr m = new ObjectMgr();
        m.spawns.add(new ObjectMgr.Spawn(42, 6, 0, 1f, 2f, 3f, 0f, 5f, 1, 60, 120));

        m.moveSpawn(42, 10f, 20f, 30f, 1.5f);

        ObjectMgr.Spawn moved = m.creatureSpawns().get(0);
        assertEquals(10f, moved.x());
        assertEquals(1.5f, moved.o());
        assertEquals(5f, moved.spawnDist());
        assertEquals(60, moved.respawnMinSecs());
    }

    @Test
    void moveSpawnWhenGuidUnknownShouldChangeNothing() {
        ObjectMgr m = new ObjectMgr();
        m.spawns.add(new ObjectMgr.Spawn(42, 6, 0, 1f, 2f, 3f, 0f));

        m.moveSpawn(7, 10f, 20f, 30f, 1.5f);

        assertEquals(1, m.creatureSpawns().size());
        assertEquals(1f, m.creatureSpawns().get(0).x());
    }
}
