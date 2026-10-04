package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.content.dbc.WdbcFile;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.map.WorldMapAreas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEditSessionTest {
    @Test
    void changesWhenMovedAndRenamedShouldNameTheGuidAndTheTemplate() {
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 10f, 20f, 30f);
        session.rename(15271, "Arcane Wyrm");
        session.setFaction(15271, 72);
        List<NpcEditSession.Change> changes = session.changes();
        assertEquals(2, changes.size());
        assertEquals(NpcEditSession.Change.Kind.MOVED, changes.get(0).kind());
        assertEquals(1, changes.get(0).guid());
        assertTrue(changes.get(0).toString().contains("guid 1"));
        assertTrue(changes.get(0).toString().contains("moved"));
        assertEquals(NpcEditSession.Change.Kind.TEMPLATE, changes.get(1).kind());
        assertEquals(15271, changes.get(1).entry());
        assertTrue(changes.get(1).toString().contains("entry 15271"));
        assertTrue(changes.get(1).toString().contains("name"));
        assertTrue(changes.get(1).toString().contains("faction"));
    }

    @Test
    void clearWhenNameAndPositionChangedShouldRestoreTheLoadedPin() {
        MapSpawnLayer.Pin pin = wyrm();
        NpcEditSession session = new NpcEditSession();
        session.remember(pin, 49, 0, 1, 14);
        session.move(1, 10f, 20f, 30f);
        session.rename(15271, "Renamed");
        session.setCreatureType(15271, 7);
        session.setFaction(15271, 72);
        assertTrue(session.dirty());
        session.revert();
        assertFalse(session.dirty());
        assertEquals(1, session.look(15271).creatureType());
        assertEquals(14, session.look(15271).faction());
        MapSpawnLayer.Pin moved = pin.moved(10f, 20f, 30f).named("Renamed");
        MapSpawnLayer.Pin restored = session.overlay(List.of(moved)).get(0);
        assertEquals(pin.x(), restored.x(), 0.01f);
        assertEquals(pin.y(), restored.y(), 0.01f);
        assertEquals(33f, restored.z(), 0.01f);
        assertEquals("Mana Wyrm", restored.name());
    }

    @Test
    void saveWhenGuidMissingShouldRefuseWithoutUpdatingTemplate() {
        NpcEditSession session = movedAndRenamed();
        Capture sql = new Capture();
        SQLException ex = assertThrows(SQLException.class, () -> NpcEditStore.save(sql, session));
        assertTrue(ex.getMessage().contains("guid 1"));
        assertTrue(sql.sql.stream().noneMatch(s -> s.contains("UPDATE")));
    }

    @Test
    void saveWhenReadbackMismatchesShouldRefuse() {
        NpcEditSession session = movedAndRenamed();
        Capture sql = new Capture();
        sql.guids.add(1);
        sql.entries.add(15271);
        sql.readback = new float[] {0f, 0f, 0f};
        SQLException ex = assertThrows(SQLException.class, () -> NpcEditStore.save(sql, session));
        assertTrue(ex.getMessage().contains("did not keep the saved position"));
    }

    @Test
    void saveWhenDisplayColumnMissingShouldUpdateModelIdAndPosition() throws Exception {
        NpcEditSession session = movedAndRenamed();
        Capture sql = new Capture();
        sql.guids.add(1);
        sql.entries.add(15271);
        sql.failDisplayCode = 1054;
        NpcEditStore.Result written = NpcEditStore.save(sql, session);
        assertEquals(1, written.spawns());
        assertEquals(1, written.templates());
        assertTrue(sql.sql.stream().anyMatch(s -> s.contains("UPDATE creature SET position_x")));
        assertTrue(sql.sql.stream().anyMatch(s -> s.contains("ModelId1")));
        Object[] template = sql.args.stream()
                .filter(a -> a.length == 6 && Integer.valueOf(15271).equals(a[5]))
                .findFirst().orElseThrow();
        assertEquals("Arcane Wyrm", template[0]);
        assertEquals(15476, template[1]);
        assertEquals(9, template[2]);
        assertEquals(7, template[3]);
        assertEquals(72, template[4]);
    }

    @Test
    void saveWhenTemplateErrorIsNotUnknownColumnShouldNotRetryModelId() {
        NpcEditSession session = movedAndRenamed();
        Capture sql = new Capture();
        sql.guids.add(1);
        sql.entries.add(15271);
        sql.failDisplayCode = 1064;
        SQLException ex = assertThrows(SQLException.class, () -> NpcEditStore.save(sql, session));
        assertEquals(1064, ex.getErrorCode());
        assertTrue(sql.sql.stream().noneMatch(s -> s.contains("ModelId1")));
    }

    @Test
    void applyWhenSavedShouldReplaceSpawnAndTemplateInMemory() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 49, 14, 40, 1, 0, "", "", 0));
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 0f));
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 0, 14);
        session.move(1, 10380f, -6340f, 12.5f);
        session.rename(15271, "Arcane Wyrm");
        session.setDisplay(15271, 15476);
        session.setEquipment(15271, 9);
        session.setCreatureType(15271, 1);
        session.setFaction(15271, 72);
        QuestService service = new QuestService(mgr, Path.of("target", "npc-edit-test"));
        service.applyNpcEdits(session);
        ObjectMgr.Spawn spawn = mgr.spawns.get(0);
        assertEquals(10380f, spawn.x(), 0.01f);
        assertEquals(-6340f, spawn.y(), 0.01f);
        assertEquals(12.5f, spawn.z(), 0.01f);
        assertEquals("Arcane Wyrm", mgr.creatures.get(15271).name());
        assertEquals(15476, mgr.creatures.get(15271).display());
        assertEquals(1, mgr.creatures.get(15271).type());
        assertEquals(72, mgr.creatures.get(15271).faction());
        assertEquals(9, mgr.equipmentByEntry.get(15271));
        MapSpawnLayer.Pin pin = MapSpawnLayer.inArea(mgr, WorldMapAreas.SUNSTRIDER).stream()
                .filter(p -> p.guid() == 1).findFirst().orElseThrow();
        assertEquals(12.5f, pin.z(), 0.01f);
        assertEquals("Arcane Wyrm", pin.name());
    }

    @Test
    void noteSpawnMoveWhenFloorMissingShouldKeepTheStatusLine() {
        List<String> lines = new ArrayList<>();
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 49, 14, 40, 1, 0, "", "", 0));
        QuestDomain domain = new QuestDomain(new QuestService(mgr, Path.of("target", "npc-edit-test")), lines::add);
        domain.noteSpawnMove(new QuestMapCanvas.SpawnMove(wyrm().moved(1f, 2f, 33f), false));
        assertTrue(lines.stream().anyMatch(s -> s.contains("No terrain height")));
    }

    @Test
    void gearLabelWhenItemsKnownShouldUseTheirNames() {
        ObjectMgr mgr = new ObjectMgr();
        ObjectMgr.ItemTemplate sword = new ObjectMgr.ItemTemplate();
        sword.name = "Gladius";
        mgr.items.put(25, sword);
        mgr.equipmentItems.put(9, new int[] {25, 0, 0});
        assertEquals("9: Gladius", NpcGear.choices(mgr).get(1).label());
        assertEquals("None", NpcGear.choices(mgr).get(0).label());
    }

    @Test
    void racesWhenMaleAndFemaleDisplaysDifferShouldOfferBoth() {
        byte[] strings = "\0Human\0".getBytes(StandardCharsets.UTF_8);
        int[] row = new int[15];
        row[0] = 1;
        row[4] = 49;
        row[5] = 50;
        row[14] = 1;
        List<NpcRaces.Race> races = NpcRaces.from(new WdbcFile(15, 60, List.of(row), strings));
        assertEquals(2, races.size());
        assertEquals(49, races.get(0).displayId());
        assertEquals("Human", races.get(0).name());
        assertEquals(50, races.get(1).displayId());
        assertEquals("Human (female)", races.get(1).name());
    }

    private static NpcEditSession movedAndRenamed() {
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0, 1, 14);
        session.move(1, 11f, 22f, 40f);
        session.rename(15271, "Arcane Wyrm");
        session.setDisplay(15271, 15476);
        session.setEquipment(15271, 9);
        session.setCreatureType(15271, 7);
        session.setFaction(15271, 72);
        return session;
    }

    private static MapSpawnLayer.Pin wyrm() {
        return new MapSpawnLayer.Pin(MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast",
                10349.6f, -6357.29f, 33f);
    }

    private static final class Capture implements NpcEditStore.Sql {
        final List<String> sql = new ArrayList<>();
        final List<Object[]> args = new ArrayList<>();
        final java.util.Set<Integer> guids = new java.util.HashSet<>();
        final java.util.Set<Integer> entries = new java.util.HashSet<>();
        float[] readback;
        int failDisplayCode;

        @Override
        public boolean exists(String statement, int id) {
            sql.add(statement);
            args.add(new Object[] {id});
            if (statement.contains("creature_template")) {
                return entries.contains(id);
            }
            return guids.contains(id);
        }

        @Override
        public void update(String statement, Object... values) throws SQLException {
            if (failDisplayCode != 0 && statement.contains("DisplayId1")) {
                throw new SQLException("Unknown column 'DisplayId1'", "42S22", failDisplayCode);
            }
            sql.add(statement);
            args.add(values);
        }

        @Override
        public float[] floats(String statement, int id) {
            sql.add(statement);
            args.add(new Object[] {id});
            if (readback != null) {
                return readback;
            }
            return new float[] {11f, 22f, 40f};
        }
    }
}
