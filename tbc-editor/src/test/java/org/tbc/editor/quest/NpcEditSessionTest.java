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
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEditSessionTest {
    @Test
    void clearWhenNameAndPositionChangedShouldRestoreTheLoadedPin() {
        MapSpawnLayer.Pin pin = wyrm();
        NpcEditSession session = new NpcEditSession();
        session.remember(pin, 49, 0);
        session.move(1, 10f, 20f, 30f);
        session.rename(15271, "Renamed");
        assertTrue(session.dirty());
        session.revert();
        assertFalse(session.dirty());
        MapSpawnLayer.Pin moved = pin.moved(10f, 20f, 30f).named("Renamed");
        MapSpawnLayer.Pin restored = session.overlay(List.of(moved)).get(0);
        assertEquals(pin.x(), restored.x(), 0.01f);
        assertEquals(pin.y(), restored.y(), 0.01f);
        assertEquals(33f, restored.z(), 0.01f);
        assertEquals("Mana Wyrm", restored.name());
    }

    @Test
    void saveWhenDisplayColumnMissingShouldUpdateModelIdAndPosition() throws Exception {
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0);
        session.move(1, 11f, 22f, 40f);
        session.rename(15271, "Arcane Wyrm");
        session.setDisplay(15271, 15476);
        session.setEquipment(15271, 9);
        Capture sql = new Capture();
        sql.failDisplay = true;
        NpcEditStore.save(sql, session);
        assertEquals("UPDATE creature SET position_x = ?, position_y = ?, position_z = ? WHERE guid = ?",
                sql.sql.get(0));
        assertEquals(11f, (Float) sql.args.get(0)[0], 0.01f);
        assertEquals(22f, (Float) sql.args.get(0)[1], 0.01f);
        assertEquals(40f, (Float) sql.args.get(0)[2], 0.01f);
        assertEquals(1, sql.args.get(0)[3]);
        assertTrue(sql.sql.get(1).contains("ModelId1"));
        assertEquals("Arcane Wyrm", sql.args.get(1)[0]);
        assertEquals(15476, sql.args.get(1)[1]);
        assertEquals(9, sql.args.get(1)[2]);
        assertEquals(15271, sql.args.get(1)[3]);
    }

    @Test
    void applyWhenSavedShouldReplaceSpawnAndTemplateInMemory() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.creatures.put(15271, new ObjectMgr.CreatureTemplate(
                15271, "Mana Wyrm", 49, 14, 40, 1, 0, "", "", 0));
        mgr.spawns.add(new ObjectMgr.Spawn(1, 15271, 530, 10349.6f, -6357.29f, 33f, 0f));
        NpcEditSession session = new NpcEditSession();
        session.remember(wyrm(), 49, 0);
        session.move(1, 10380f, -6340f, 12.5f);
        session.rename(15271, "Arcane Wyrm");
        session.setDisplay(15271, 15476);
        session.setEquipment(15271, 9);
        QuestService service = new QuestService(mgr, Path.of("target", "npc-edit-test"));
        service.applyNpcEdits(session);
        ObjectMgr.Spawn spawn = mgr.spawns.get(0);
        assertEquals(10380f, spawn.x(), 0.01f);
        assertEquals(-6340f, spawn.y(), 0.01f);
        assertEquals(12.5f, spawn.z(), 0.01f);
        assertEquals("Arcane Wyrm", mgr.creatures.get(15271).name());
        assertEquals(15476, mgr.creatures.get(15271).display());
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

    private static MapSpawnLayer.Pin wyrm() {
        return new MapSpawnLayer.Pin(MapSpawnLayer.Kind.CREATURE, 1, 15271, "Mana Wyrm", "Beast",
                10349.6f, -6357.29f, 33f);
    }

    private static final class Capture implements NpcEditStore.Sql {
        final List<String> sql = new ArrayList<>();
        final List<Object[]> args = new ArrayList<>();
        boolean failDisplay;

        @Override
        public void update(String statement, Object... values) throws SQLException {
            if (failDisplay && statement.contains("DisplayId1")) {
                throw new SQLException("Unknown column 'DisplayId1'");
            }
            sql.add(statement);
            args.add(values);
        }
    }
}
