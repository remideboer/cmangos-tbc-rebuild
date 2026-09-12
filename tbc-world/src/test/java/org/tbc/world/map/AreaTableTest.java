package org.tbc.world.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** AreaTable.dbc parent zone — GetZoneIdByAreaFlag. Goldshire 87 → Elwynn 12. */
class AreaTableTest {
    @TempDir
    Path tmp;

    @Test
    void zoneIdWhenGoldshireShouldBeElwynnForest() {
        AreaTable areas = AreaTable.seeded();
        assertEquals(12, areas.zoneId(87));
        assertEquals(87, areas.areaId(87));
        assertEquals(12, areas.zoneId(12));
        assertEquals(0, areas.zoneId(0));
        assertEquals(0, areas.areaId(0));
        assertEquals(0, areas.areaId(99999));
    }

    @Test
    void zoneIdWhenDunMoroghShouldBeSelf() {
        AreaTable areas = AreaTable.seeded();
        assertEquals(1, areas.zoneId(1));
    }

    @Test
    void loadFromDataDirWhenMissingOrNullShouldKeepSeed() {
        AreaTable areas = AreaTable.seeded();
        areas.loadFromDataDir(null);
        areas.loadFromDataDir(tmp);
        assertEquals(12, areas.zoneId(87));
    }

    @Test
    void loadFromDataDirWhenAreaTableDbcShouldIndexIdAndExploreFlag() throws Exception {
        Path dbcDir = tmp.resolve("dbc");
        Files.createDirectories(dbcDir);
        ByteBuffer b = ByteBuffer.allocate(20 + 16).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(4);
        b.putInt(16);
        b.putInt(0);
        b.putInt(99);
        b.putInt(0);
        b.putInt(12);
        b.putInt(7);
        Files.write(dbcDir.resolve("AreaTable.dbc"), b.array());
        AreaTable areas = AreaTable.seeded();
        areas.loadFromDataDir(tmp);
        assertEquals(99, areas.areaId(7));
        assertEquals(12, areas.zoneId(7));
        assertEquals(99, areas.areaId(99));
    }

    @Test
    void loadFromDataDirWhenNotWdbcShouldKeepSeed() throws Exception {
        Path dbcDir = tmp.resolve("dbc");
        Files.createDirectories(dbcDir);
        Files.write(dbcDir.resolve("AreaTable.dbc"), new byte[]{1, 2, 3, 4});
        AreaTable areas = AreaTable.seeded();
        areas.loadFromDataDir(tmp);
        assertEquals(12, areas.zoneId(87));
    }

    @Test
    void loadFromDataDirWhenShortRowShouldSkip() throws Exception {
        Path dbcDir = tmp.resolve("dbc");
        Files.createDirectories(dbcDir);
        ByteBuffer b = ByteBuffer.allocate(20 + 8).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(2);
        b.putInt(8);
        b.putInt(0);
        b.putInt(50);
        b.putInt(1);
        Files.write(dbcDir.resolve("AreaTable.dbc"), b.array());
        AreaTable areas = new AreaTable();
        areas.loadFromDataDir(tmp);
        assertEquals(0, areas.areaId(50));
    }

    @Test
    void loadFromDataDirWhenZeroIdShouldSkip() throws Exception {
        Path dbcDir = tmp.resolve("dbc");
        Files.createDirectories(dbcDir);
        ByteBuffer b = ByteBuffer.allocate(20 + 16).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x43424457);
        b.putInt(1);
        b.putInt(4);
        b.putInt(16);
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);
        b.putInt(12);
        b.putInt(3);
        Files.write(dbcDir.resolve("AreaTable.dbc"), b.array());
        AreaTable areas = new AreaTable();
        areas.loadFromDataDir(tmp);
        assertEquals(0, areas.areaId(3));
    }
}

