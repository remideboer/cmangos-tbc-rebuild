package org.tbc.editor.quest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tbc.common.DbPool;
import org.tbc.editor.EditorException;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.MapSurfaceService;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEditPersistTest {
    private DbPool world;

    @AfterEach
    void tearDown() {
        if (world != null) {
            world.close();
        }
    }

    @Test
    void saveNpcEditsWhenMovedShouldPersistAndRoundTripFromSql() throws Exception {
        world = memPool("ok");
        try (Connection c = world.get(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE creature (
                      guid INT PRIMARY KEY,
                      position_x DECIMAL(40,20),
                      position_y DECIMAL(40,20),
                      position_z DECIMAL(40,20)
                    )
                    """);
            s.execute("INSERT INTO creature VALUES (1, 10349.6, -6357.29, 33)");
            s.execute("""
                    CREATE TABLE creature_template (
                      Entry INT PRIMARY KEY,
                      Name VARCHAR(100),
                      DisplayId1 INT,
                      EquipmentTemplateId INT,
                      CreatureType INT,
                      Faction INT
                    )
                    """);
            s.execute("INSERT INTO creature_template VALUES (15271, 'Mana Wyrm', 49, 0, 1, 14)");
        }
        ObjectMgr mgr = mgrWithWyrm();
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"),
                MapSurfaceService.unavailable(), world);
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        NpcEditStore.Result written = service.saveNpcEdits(session);
        assertEquals(1, written.spawns());
        assertEquals(0, written.templates());
        assertTrue(!session.dirty());
        assertEquals(10380f, mgr.spawns.get(0).x(), 0.05f);
        try (Connection c = world.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT position_x, position_y, position_z FROM creature WHERE guid = 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(10380f, rs.getFloat(1), 0.05f);
            assertEquals(-6340f, rs.getFloat(2), 0.05f);
            assertEquals(12.5f, rs.getFloat(3), 0.05f);
        }
    }

    @Test
    void saveNpcEditsWhenGuidMissingShouldLeaveMemoryUnchanged() {
        world = memPool("missing");
        try (Connection c = world.get(); Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE creature (
                      guid INT PRIMARY KEY,
                      position_x DECIMAL(40,20),
                      position_y DECIMAL(40,20),
                      position_z DECIMAL(40,20)
                    )
                    """);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ObjectMgr mgr = mgrWithWyrm();
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"),
                MapSurfaceService.unavailable(), world);
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        EditorException ex = assertThrows(EditorException.class, () -> service.saveNpcEdits(session));
        assertTrue(ex.getMessage().contains("guid 1"));
        assertTrue(session.dirty());
        assertEquals(10349.6f, mgr.spawns.get(0).x(), 0.05f);
        assertEquals(-6357.29f, mgr.spawns.get(0).y(), 0.05f);
    }

    @Test
    void saveNpcEditsWhenCleanShouldWriteNothing() {
        ObjectMgr mgr = mgrWithWyrm();
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"));
        NpcEditStore.Result written = service.saveNpcEdits(new NpcEditSession());
        assertEquals(0, written.spawns());
        assertEquals(0, written.templates());
        assertEquals(10349.6f, mgr.spawns.get(0).x(), 0.05f);
    }

    private static ObjectMgr mgrWithWyrm() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 49, 14, 40, 1, 0, "", "", 0));
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 0f));
        return mgr;
    }

    private static MapSpawnLayer.Pin wyrm() {
        return new MapSpawnLayer.Pin(MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast",
                10349.6f, -6357.29f, 33f);
    }

    private static DbPool memPool(String name) {
        String url = "jdbc:h2:mem:npc_" + name + "_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        return new DbPool(url, "sa", "", "editor-npc-" + name);
    }
}
