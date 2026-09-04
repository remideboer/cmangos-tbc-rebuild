package org.tbc.world.events;

import org.tbc.common.DbPool;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.GameObject;
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

    @Test
    void startWhenSqlEventCreatureOnContinentShouldStayHiddenUntilStart() throws Exception {
        String url = "jdbc:h2:mem:gec_cont_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "event-continent-test")) {
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
                        VALUES (5470211, 25697, 0, -92.45719, -110.6642, -2.866759, 2.408554)
                        """);
                st.execute("INSERT INTO game_event_creature (guid, event) VALUES (5470211, 1)");
            }
            World world = new World(null, null, worldDb, null);
            assertNull(find(world, 0, Content.NPC_LUMA_SKYMOTHER));
            world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
            Creature luma = find(world, 0, Content.NPC_LUMA_SKYMOTHER);
            assertNotNull(luma);
            assertEquals(5470211, luma.spawnId);
        }
    }

    @Test
    void loadWhenSqlContinentGameObjectShouldSpawnIceStone() throws Exception {
        String url = "jdbc:h2:mem:go_cont_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "go-continent-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE gameobject (
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
                        INSERT INTO gameobject (guid, id, map, position_x, position_y, position_z, orientation)
                        VALUES (5470020, 187882, 0, -69.9045, -162.245, -2.36656, 2.42601)
                        """);
            }
            World world = new World(null, null, worldDb, null);
            GameObject stone = findGo(world, 0, Content.GO_ICE_STONE);
            assertNotNull(stone);
            assertEquals(5470020, (int) stone.guid);
        }
    }

    @Test
    void startWhenMidsummerHasOnlyGameObjectsShouldSpawnIceStone() {
        World world = World.inMemory();
        world.objectMgr.eventCreatures.remove(Content.GAME_EVENT_MIDSUMMER);
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNull(find(world, Content.NPC_LUMA_SKYMOTHER));
        assertNotNull(findGo(world, Content.GO_ICE_STONE));
    }

    @Test
    void startWhenMidsummerShouldSpawnIceStone() {
        World world = World.inMemory();
        assertNull(findGo(world, Content.GO_ICE_STONE));
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(findGo(world, Content.GO_ICE_STONE));
    }

    @Test
    void stopWhenMidsummerShouldDespawnIceStone() {
        World world = World.inMemory();
        world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
        assertNotNull(findGo(world, Content.GO_ICE_STONE));
        world.events.stop(world, Content.GAME_EVENT_MIDSUMMER);
        assertNull(findGo(world, Content.GO_ICE_STONE));
    }

    @Test
    void startWhenSqlGameEventGameobjectShouldSpawnIceBlock() throws Exception {
        String url = "jdbc:h2:mem:gego_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool worldDb = new DbPool(url, "sa", "", "event-go-test")) {
            try (Connection c = worldDb.get(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE gameobject (
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
                        CREATE TABLE game_event_gameobject (
                          guid INT,
                          event SMALLINT
                        )
                        """);
                st.execute("""
                        INSERT INTO gameobject (guid, id, map, position_x, position_y, position_z, orientation)
                        VALUES (5470021, 188067, 547, -83.5253, -172.181, -3.81652, 0.017452)
                        """);
                st.execute("INSERT INTO game_event_gameobject (guid, event) VALUES (5470021, 1)");
            }
            World world = new World(null, null, worldDb, null);
            assertNull(findGo(world, Content.GO_ICE_BLOCK));
            world.events.start(world, Content.GAME_EVENT_MIDSUMMER);
            GameObject block = findGo(world, Content.GO_ICE_BLOCK);
            assertNotNull(block);
            assertEquals(5470021, (int) block.guid);
        }
    }

    private static Creature find(World world, int entry) {
        return find(world, 547, entry);
    }

    private static Creature find(World world, int map, int entry) {
        for (Creature c : world.map(map, 0).creatures.values()) {
            if (c.entry == entry) {
                return c;
            }
        }
        return null;
    }

    private static GameObject findGo(World world, int entry) {
        return findGo(world, 547, entry);
    }

    private static GameObject findGo(World world, int map, int entry) {
        for (GameObject go : world.map(map, 0).gameObjects.values()) {
            if (go.entry == entry) {
                return go;
            }
        }
        return null;
    }
}
