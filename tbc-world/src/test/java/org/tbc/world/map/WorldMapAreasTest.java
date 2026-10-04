package org.tbc.world.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapAreasTest {
    @TempDir
    Path tmp;

    @Test
    void seededWhenListedByNameShouldIncludeElwynnAndEversong() {
        WorldMapAreas areas = WorldMapAreas.seeded();
        List<String> names = areas.list().stream().map(WorldMapArea::displayName).toList();
        assertTrue(names.contains("Elwynn Forest"));
        assertTrue(names.contains("Eversong Woods"));
        WorldMapArea elwynn = areas.byAreaId(AreaTable.ELWYNN_FOREST);
        assertEquals(0, elwynn.mapId());
        assertTrue(new WorldMapAreaMapper(elwynn).contains(-9465f, 62f));
        WorldMapArea eversong = areas.byAreaId(AreaTable.EVERSONG_WOODS);
        assertEquals(530, eversong.mapId());
        assertTrue(new WorldMapAreaMapper(eversong).contains(10349.6f, -6357.29f));
        WorldMapArea isle = areas.byDisplayName("Sunstrider Isle");
        assertEquals(AreaTable.SUNSTRIDER_ISLE, isle.areaId());
        assertEquals(530, isle.mapId());
        assertTrue(new WorldMapAreaMapper(isle).contains(10349.6f, -6357.29f));
        assertTrue(new WorldMapAreaMapper(isle).contains(10458.5f, -6364.61f));
        assertTrue(!new WorldMapAreaMapper(isle).contains(9000f, -8000f));
    }

    @Test
    void listWhenSeededShouldBeSortedByDisplayName() {
        List<WorldMapArea> list = WorldMapAreas.seeded().list();
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1).displayName().compareToIgnoreCase(list.get(i).displayName()) <= 0);
        }
    }

    @Test
    void fromDbcWhenWorldMapAreaPresentShouldReplaceSeedByAreaId() throws Exception {
        Path dbc = tmp.resolve("dbc");
        Files.createDirectories(dbc);
        writeWorldMapArea(dbc.resolve("WorldMapArea.dbc"), 12, 0, 12, "Elwynn",
                100f, 0f, -8000f, -9000f);
        writeAreaTableName(dbc.resolve("AreaTable.dbc"), 12, "Elwynn Forest");
        WorldMapAreas areas = WorldMapAreas.fromDbc(tmp);
        WorldMapArea elwynn = areas.byAreaId(12);
        assertEquals(-8000f, elwynn.locTop(), 1e-3f);
        assertEquals("Elwynn Forest", elwynn.displayName());
        assertTrue(areas.byAreaId(AreaTable.EVERSONG_WOODS) != null);
    }

    @Test
    void fromDbcWhenDegenerateLocShouldSkipRow() throws Exception {
        Path dbc = tmp.resolve("dbc");
        Files.createDirectories(dbc);
        writeWorldMapArea(dbc.resolve("WorldMapArea.dbc"), 1, 0, 99, "Bad",
                1f, 0f, 5f, 5f);
        WorldMapAreas areas = WorldMapAreas.fromDbc(tmp);
        assertNull(areas.byAreaId(99));
    }

    @Test
    void fromDbcWhenNotWdbcShouldKeepSeed() throws Exception {
        Path dbc = tmp.resolve("dbc");
        Files.createDirectories(dbc);
        Files.write(dbc.resolve("WorldMapArea.dbc"), new byte[]{1, 2, 3, 4});
        WorldMapAreas areas = WorldMapAreas.fromDbc(tmp);
        assertEquals(0, areas.byAreaId(12).mapId());
    }

    @Test
    void byDisplayNameWhenInternalOrMissingShouldResolve() {
        WorldMapAreas areas = WorldMapAreas.seeded();
        assertEquals(12, areas.byDisplayName("Elwynn").areaId());
        assertEquals(null, areas.byDisplayName("nope"));
        assertEquals(null, areas.byDisplayName(" "));
        areas.put(null);
        areas.put(new WorldMapArea(1, 0, "x", "x", 1f, 1f, 2f, 2f));
        assertEquals(null, areas.byAreaId(1));
    }

    @Test
    void fromDbcWhenMissingShouldKeepSeed() {
        WorldMapAreas areas = WorldMapAreas.fromDbc(tmp);
        assertEquals(0, areas.byAreaId(12).mapId());
        WorldMapAreas.fromDbc(null);
        assertEquals(0, WorldMapAreas.seeded().byAreaId(12).mapId());
    }

    @Test
    void fromDbcWhenContinentAreaZeroShouldUseMapDbcName() throws Exception {
        Path dbc = tmp.resolve("dbc");
        Files.createDirectories(dbc);
        writeWorldMapArea(dbc.resolve("WorldMapArea.dbc"), 1, 0, 0, "Azeroth",
                4000f, -4000f, 5000f, -5000f);
        writeMapName(dbc.resolve("Map.dbc"), 0, "Eastern Kingdoms");
        WorldMapAreas areas = WorldMapAreas.fromDbc(tmp);
        WorldMapArea continent = areas.list().stream()
                .filter(a -> a.areaId() == 0 && a.mapId() == 0)
                .findFirst()
                .orElseThrow();
        assertEquals("Eastern Kingdoms", continent.displayName());
    }

    private static void writeWorldMapArea(Path file, int id, int mapId, int areaId, String internal,
                                          float locLeft, float locRight, float locTop, float locBottom)
            throws Exception {
        byte[] name = ("\0" + internal + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(20 + 36 + name.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(9);
        b.putInt(36);
        b.putInt(name.length);
        b.putInt(id);
        b.putInt(mapId);
        b.putInt(areaId);
        b.putInt(1);
        b.putInt(Float.floatToIntBits(locLeft));
        b.putInt(Float.floatToIntBits(locRight));
        b.putInt(Float.floatToIntBits(locTop));
        b.putInt(Float.floatToIntBits(locBottom));
        b.putInt(-1);
        b.put(name);
        Files.write(file, b.array());
    }

    private static void writeAreaTableName(Path file, int id, String name) throws Exception {
        byte[] str = ("\0" + name + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int fields = 12;
        int rec = fields * 4;
        ByteBuffer b = ByteBuffer.allocate(20 + rec + str.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(fields);
        b.putInt(rec);
        b.putInt(str.length);
        b.putInt(id);
        for (int i = 1; i < 11; i++) {
            b.putInt(0);
        }
        b.putInt(1);
        b.put(str);
        Files.write(file, b.array());
    }

    private static void writeMapName(Path file, int mapId, String name) throws Exception {
        byte[] str = ("\0" + name + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int fields = 5;
        int rec = fields * 4;
        ByteBuffer b = ByteBuffer.allocate(20 + rec + str.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(fields);
        b.putInt(rec);
        b.putInt(str.length);
        b.putInt(mapId);
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);
        b.putInt(1);
        b.put(str);
        Files.write(file, b.array());
    }
}
