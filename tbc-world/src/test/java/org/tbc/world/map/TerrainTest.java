package org.tbc.world.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainTest {
    @Test
    void atWhenNoDataDirShouldReturnHint() {
        Terrain t = Terrain.fromDataDir(null);
        assertEquals(12f, t.at(0, 0, 0, 12f), 0.01f);
        assertEquals(0, t.area(0, 0, 0));
    }

    @Test
    void atWhenMapsMissingShouldReturnHint(@TempDir Path dir) {
        Terrain t = new Terrain(dir);
        assertEquals(7f, t.at(0, 0, 0, 7f), 0.01f);
    }

    @Test
    void atWhenFlatHeightTileShouldReturnGridHeight(@TempDir Path dir) throws Exception {
        Path maps = dir.resolve("maps");
        Files.createDirectories(maps);
        ByteBuffer b = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN);
        b.put("MAPS".getBytes());
        b.put("s1.4".getBytes());
        b.putInt(0);
        b.putInt(0);
        b.putInt(40);
        b.putInt(16);
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);
        b.putInt(0);
        b.put("MHGT".getBytes());
        b.putInt(0x0001);
        b.putFloat(42f);
        b.putFloat(42f);
        Files.write(maps.resolve("0003232.map"), b.array());
        Terrain t = new Terrain(dir);
        assertEquals(42f, t.at(0, 0, 0, 1f), 0.01f);
    }
}
