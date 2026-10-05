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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void saveNpcEditsWhenMovedExistingRowShouldUpdate() throws Exception {
        world = memPool("ok");
        createCreatureTable(true);
        try (Connection c = world.get(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO creature VALUES (1, 15271, 530, 1, 10349.6, -6357.29, 33, 0, 300, 300, 0, 0)");
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
        mgr.markDbCreature(1);
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"),
                MapSurfaceService.unavailable(), world);
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        session.face(1, NpcFacing.left(0f));
        NpcEditStore.Result written = service.saveNpcEdits(session);
        assertEquals(1, written.updated());
        assertEquals(0, written.inserted());
        assertEquals(1, written.spawns());
        assertEquals(0, written.templates());
        assertTrue(!session.dirty());
        assertEquals(10380f, mgr.spawns.get(0).x(), 0.05f);
        assertEquals(NpcFacing.left(0f), mgr.spawns.get(0).o(), 0.05f);
        assertPose(1, 10380f, -6340f, 12.5f, NpcFacing.left(0f));
    }

    @Test
    void saveNpcEditsWhenMemoryOnlySpawnShouldInsertAndMarkDb() throws Exception {
        world = memPool("insert");
        createCreatureTable(true);
        ObjectMgr mgr = mgrWithWyrm();
        assertFalse(mgr.dbCreature(1));
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"),
                MapSurfaceService.unavailable(), world);
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        session.face(1, 1.25f);
        NpcEditStore.Result written = service.saveNpcEdits(session);
        assertEquals(0, written.updated());
        assertEquals(1, written.inserted());
        assertTrue(mgr.dbCreature(1));
        assertTrue(!session.dirty());
        assertPose(1, 10380f, -6340f, 12.5f, 1.25f);
        try (Connection c = world.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, map, spawnMask, orientation FROM creature WHERE guid = 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(15271, rs.getInt(1));
            assertEquals(530, rs.getInt(2));
            assertEquals(1, rs.getInt(3));
            assertEquals(1.25f, rs.getFloat(4), 0.01f);
        }
    }

    @Test
    void saveNpcEditsWhenOrphanGuidShouldLeaveMemoryUnchanged() {
        world = memPool("orphan");
        createCreatureTable(false);
        ObjectMgr mgr = new ObjectMgr();
        QuestService service = new QuestService(mgr, Path.of("target", "npc-persist-test"),
                MapSurfaceService.unavailable(), world);
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        EditorException ex = assertThrows(EditorException.class, () -> service.saveNpcEdits(session));
        assertTrue(ex.getMessage().contains("guid 1"));
        assertTrue(session.dirty());
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

    private void createCreatureTable(boolean withExtras) {
        try (Connection c = world.get(); Statement s = c.createStatement()) {
            if (withExtras) {
                s.execute("""
                        CREATE TABLE creature (
                          guid INT PRIMARY KEY,
                          id INT,
                          map INT,
                          spawnMask INT,
                          position_x DECIMAL(40,20),
                          position_y DECIMAL(40,20),
                          position_z DECIMAL(40,20),
                          orientation DECIMAL(40,20),
                          spawntimesecsmin INT,
                          spawntimesecsmax INT,
                          spawndist FLOAT,
                          MovementType INT
                        )
                        """);
            } else {
                s.execute("""
                        CREATE TABLE creature (
                          guid INT PRIMARY KEY,
                          position_x DECIMAL(40,20),
                          position_y DECIMAL(40,20),
                          position_z DECIMAL(40,20)
                        )
                        """);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertPose(int guid, float x, float y, float z, float o) throws Exception {
        try (Connection c = world.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT position_x, position_y, position_z, orientation FROM creature WHERE guid = ?")) {
            ps.setInt(1, guid);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(x, rs.getFloat(1), 0.05f);
                assertEquals(y, rs.getFloat(2), 0.05f);
                assertEquals(z, rs.getFloat(3), 0.05f);
                assertEquals(o, rs.getFloat(4), 0.05f);
            }
        }
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
