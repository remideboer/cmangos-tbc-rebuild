package org.tbc.world.events;

import org.tbc.common.DbPool;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameEventMgrTest {
    @Test
    void startWhenMidsummerShouldSpawnLumaSkymother() {
        World world = World.inMemory();
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(find(world, Content.NPC_LUMA_SKYMOTHER));
    }

    @Test
    void stopWhenMidsummerShouldDespawnLumaSkymother() {
        World world = World.inMemory();
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        world.events.stop(world, Content.GAME_EVENT_MIDSUMMER);
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
    }

    @Test
    void startWhenSqlGameEventCreatureShouldSpawnLumaSkymother() throws Exception {
        String url = "jdbc:h2:mem:gec_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "event-creature-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE creature (
                          guid INT PRIMARY KEY,
                          id INT,
                          map INT,
                          position_x FLOAT,
                          position_y FLOAT,
                          position_z FLOAT,
                          orientation FLOAT
                        )
                        """);
                st.execute("""
                        CREATE TABLE game_event_creature (
                          guid INT,
                          event SMALLINT
                        )
                        """);
                st.execute("""
                        INSERT INTO creature (guid, id, map, position_x, position_y, position_z, orientation)
                        VALUES (5470211, 25697, 547, -92.45719, -110.6642, -2.866759, 2.408554)
                        """);
                st.execute("INSERT INTO game_event_creature (guid, event) VALUES (5470211, 1)");
            }
            World world = new World(null, null, worldDb, null);
            assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
            world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
            Creature luma = find(world, Content.NPC_LUMA_SKYMOTHER);
            assertNotNull(luma);
            assertEquals(5470211, luma.spawnId);
        }
    }

    private static Creature find(World world, int entry) {
        for (Creature c : world.map(547, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }
}
